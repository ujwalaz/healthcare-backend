package com.healthcare.service;

import com.healthcare.dto.*;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.healthcare.constants.MessageCode;
import com.healthcare.entity.Appointment;
import com.healthcare.entity.Document;
import com.healthcare.exception.AppDeniedException;
import com.healthcare.exception.ResourceNotFoundException;
import com.healthcare.repository.AppointmentRepository;
import com.healthcare.repository.DocumentRepository;
import com.healthcare.repository.HospitalRepository;
import com.healthcare.repository.PatientRepository;
import com.healthcare.security.JwtClaims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
            "BILL", "PRESCRIPTION", "REPORT", "DOCTOR_NOTE", "PREVIOUS_RECORD");

    private final DocumentRepository documentRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final HospitalRepository hospitalRepository;
    private final BlobServiceClient blobServiceClient;

    @Value("${app.azure.storage.container-name}")
    private String containerName;

    @Value("${app.azure.storage.sas-expiry-minutes}")
    private long sasExpiryMinutes;

    @Value("${app.document.doctor-access.prior-minutes:30}")
    private long priorMinutes;

    @Value("${app.document.doctor-access.post-minutes:60}")
    private long postMinutes;

    @Transactional
    public DocumentResponse upload(MultipartFile file, Long patientId, Long hospitalId,
                                   Long appointmentId, String documentType, JwtClaims caller) {
        if (file == null || file.isEmpty() || patientId == null || hospitalId == null ||
                !ALLOWED_DOCUMENT_TYPES.contains(documentType)) {
            throw new AppDeniedException(MessageCode.VALIDATION_FAILED,
                    "File, patientId, hospitalId, and a valid documentType are required");
        }
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException(MessageCode.PATIENT_NOT_FOUND);
        }
        if (!hospitalRepository.existsById(hospitalId)) {
            throw new ResourceNotFoundException(MessageCode.HOSPITAL_NOT_FOUND);
        }
        if (appointmentId != null) {
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new ResourceNotFoundException(MessageCode.APPOINTMENT_NOT_FOUND));
            if (!patientId.equals(appointment.getPatientId()) || !hospitalId.equals(appointment.getHospitalId())) {
                throw new AppDeniedException(MessageCode.VALIDATION_FAILED,
                        "Appointment does not belong to the supplied patient and hospital");
            }
        }
        if ("PATIENT".equals(caller.role())) {
            if (!caller.userId().equals(patientId)) {
                throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED,
                        "Patients can only upload documents for themselves");
            }
            if (!"PREVIOUS_RECORD".equals(documentType)) {
                throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED,
                        "Patients can only upload documents of type PREVIOUS_RECORD");
            }
        } else if ("ADMIN".equals(caller.role())) {
            if (!caller.hospitalId().equals(hospitalId)) {
                throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED,
                        "Admin can only upload documents for their hospital");
            }
        } else {
            throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED,
                    "Only PATIENT and ADMIN roles can upload documents");
        }

        String originalFileName = file.getOriginalFilename();
        String storageKey = "patients/" + patientId + "/" + UUID.randomUUID() + "/" + originalFileName;

        try {
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            BlobClient blobClient = containerClient.getBlobClient(storageKey);
            blobClient.upload(file.getInputStream(), file.getSize(), true);
        } catch (IOException e) {
            log.error("Failed to upload file to Azure Blob Storage: {}", e.getMessage());
            throw new RuntimeException("File upload failed: " + e.getMessage(), e);
        }

        Document document = Document.builder()
                .patientId(patientId)
                .hospitalId(hospitalId)
                .appointmentId(appointmentId)
                .documentType(documentType)
                .storageKey(storageKey)
                .originalFileName(originalFileName)
                .uploadedByRole(caller.role())
                .uploadedById(caller.userId())
                .isVisibleToPatient(true)
                .createdAt(LocalDateTime.now())
                .build();

        document = documentRepository.save(document);
        log.info("Document uploaded with id={} for patientId={}", document.getId(), patientId);
        return toResponse(document);
    }

    @Transactional(readOnly = true)
    public PagedResponse<DocumentSummaryResponse> getDocuments(Long patientId, String documentType,
                                                                Long appointmentId, Pageable pageable,
                                                                JwtClaims caller) {
        Page<Document> page;

        if ("PATIENT".equals(caller.role())) {
            if (!caller.userId().equals(patientId)) {
                throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED);
            }
            page = documentRepository.findVisibleByPatientWithFilters(
                    patientId, documentType, appointmentId, pageable);
        } else if ("ADMIN".equals(caller.role())) {
            page = documentRepository.findByHospitalWithFilters(
                    caller.hospitalId(), patientId, documentType, appointmentId, pageable);
        } else if ("DOCTOR".equals(caller.role())) {
            verifyDoctorDocumentAccess(caller.userId(), patientId);
            page = documentRepository.findPatientUploadedByPatient(patientId, documentType, pageable);
        } else {
            throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED);
        }

        return PagedResponse.from(page.map(this::toSummaryResponse));
    }

    @Transactional(readOnly = true)
    public DownloadUrlResponse getDownloadUrl(Long documentId, JwtClaims caller) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageCode.DOCUMENT_NOT_FOUND));

        if ("PATIENT".equals(caller.role())) {
            if (!document.getPatientId().equals(caller.userId()) ||
                !Boolean.TRUE.equals(document.getIsVisibleToPatient())) {
                throw new AppDeniedException(MessageCode.DOCUMENT_ACCESS_DENIED);
            }
        } else if ("ADMIN".equals(caller.role())) {
            if (!document.getHospitalId().equals(caller.hospitalId())) {
                throw new AppDeniedException(MessageCode.DOCUMENT_ACCESS_DENIED);
            }
        } else if ("DOCTOR".equals(caller.role())) {
            if (!"PATIENT".equals(document.getUploadedByRole())) {
                throw new AppDeniedException(MessageCode.DOCUMENT_ACCESS_DENIED,
                        "Doctors can only access patient-uploaded documents");
            }
            verifyDoctorDocumentAccess(caller.userId(), document.getPatientId());
        } else {
            throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED);
        }

        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(document.getStorageKey());

        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(sasExpiryMinutes);
        BlobSasPermission permission = new BlobSasPermission().setReadPermission(true);
        BlobServiceSasSignatureValues values = new BlobServiceSasSignatureValues(expiresAt, permission);
        String sasToken = blobClient.generateSas(values);
        String downloadUrl = blobClient.getBlobUrl() + "?" + sasToken;

        log.info("SAS URL generated for documentId={}", documentId);
        return new DownloadUrlResponse()
                .downloadUrl(URI.create(downloadUrl))
                .expiresAt(expiresAt);
    }

    @Transactional
    public void deleteDocument(Long documentId, JwtClaims caller) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageCode.DOCUMENT_NOT_FOUND));

        if ("PATIENT".equals(caller.role())) {
            if (!document.getPatientId().equals(caller.userId())) {
                throw new AppDeniedException(MessageCode.DOCUMENT_ACCESS_DENIED,
                        "Patients can only delete their own documents");
            }
        } else if ("ADMIN".equals(caller.role())) {
            if (!document.getHospitalId().equals(caller.hospitalId())) {
                throw new AppDeniedException(MessageCode.DOCUMENT_ACCESS_DENIED);
            }
        } else {
            throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED,
                    "Only PATIENT and ADMIN can delete documents");
        }

        document.setIsVisibleToPatient(false);
        documentRepository.save(document);
        log.info("Document id={} soft-deleted (isVisibleToPatient=false)", documentId);
    }

    private DocumentResponse toResponse(Document d) {
        return new DocumentResponse()
                .id(d.getId())
                .patientId(d.getPatientId())
                .hospitalId(d.getHospitalId())
                .appointmentId(d.getAppointmentId())
                .documentType(d.getDocumentType())
                .originalFileName(d.getOriginalFileName())
                .uploadedByRole(d.getUploadedByRole())
                .isVisibleToPatient(d.getIsVisibleToPatient())
                .createdAt(toOffsetDateTime(d.getCreatedAt()));
    }

    private DocumentSummaryResponse toSummaryResponse(Document d) {
        return new DocumentSummaryResponse()
                .id(d.getId())
                .documentType(d.getDocumentType())
                .originalFileName(d.getOriginalFileName())
                .uploadedByRole(d.getUploadedByRole())
                .appointmentId(d.getAppointmentId())
                .createdAt(toOffsetDateTime(d.getCreatedAt()));
    }


    private OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        return value != null ? value.atZone(ZoneId.systemDefault()).toOffsetDateTime() : null;
    }

    private void verifyDoctorDocumentAccess(Long doctorId, Long patientId) {
        LocalDateTime now = LocalDateTime.now();
        List<Appointment> appointments = appointmentRepository.findActiveByDoctorIdAndPatientId(doctorId, patientId);
        boolean hasAccess = appointments.stream().anyMatch(a -> {
            LocalDateTime windowStart = LocalDateTime.of(
                    a.getAppointmentDate().toLocalDate(), a.getStartTime().toLocalTime())
                    .minusMinutes(priorMinutes);
            LocalDateTime windowEnd = LocalDateTime.of(
                    a.getAppointmentDate().toLocalDate(), a.getEndTime().toLocalTime())
                    .plusMinutes(postMinutes);
            return !now.isBefore(windowStart) && !now.isAfter(windowEnd);
        });
        if (!hasAccess) {
            throw new AppDeniedException(MessageCode.DOCUMENT_ACCESS_DENIED,
                    "Document access is only permitted within the appointment time window");
        }
    }
}
