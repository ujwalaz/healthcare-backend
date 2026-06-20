package com.healthcare.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.healthcare.constants.MessageCode;
import com.healthcare.dto.PagedResponse;
import com.healthcare.dto.document.DocumentResponse;
import com.healthcare.dto.document.DocumentSummaryResponse;
import com.healthcare.dto.document.DownloadUrlResponse;
import com.healthcare.entity.Document;
import com.healthcare.exception.AppDeniedException;
import com.healthcare.exception.ResourceNotFoundException;
import com.healthcare.repository.DocumentRepository;
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
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

//@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final BlobServiceClient blobServiceClient;

    @Value("${app.azure.storage.container-name}")
    private String containerName;

    @Value("${app.azure.storage.sas-expiry-minutes}")
    private long sasExpiryMinutes;

    @Transactional
    public DocumentResponse upload(MultipartFile file, Long patientId, Long hospitalId,
                                   Long appointmentId, String documentType, JwtClaims caller) {
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
        return DownloadUrlResponse.builder()
                .downloadUrl(downloadUrl)
                .expiresAt(expiresAt)
                .build();
    }

    @Transactional
    public void deleteDocument(Long documentId, JwtClaims caller) {
        if (!"ADMIN".equals(caller.role())) {
            throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED,
                    "Only ADMIN can delete documents");
        }

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageCode.DOCUMENT_NOT_FOUND));

        if (!document.getHospitalId().equals(caller.hospitalId())) {
            throw new AppDeniedException(MessageCode.DOCUMENT_ACCESS_DENIED);
        }

        document.setIsVisibleToPatient(false);
        documentRepository.save(document);
        log.info("Document id={} soft-deleted (isVisibleToPatient=false)", documentId);
    }

    private DocumentResponse toResponse(Document d) {
        return DocumentResponse.builder()
                .id(d.getId())
                .patientId(d.getPatientId())
                .hospitalId(d.getHospitalId())
                .appointmentId(d.getAppointmentId())
                .documentType(d.getDocumentType())
                .originalFileName(d.getOriginalFileName())
                .uploadedByRole(d.getUploadedByRole())
                .isVisibleToPatient(d.getIsVisibleToPatient())
                .createdAt(d.getCreatedAt())
                .build();
    }

    private DocumentSummaryResponse toSummaryResponse(Document d) {
        return DocumentSummaryResponse.builder()
                .id(d.getId())
                .documentType(d.getDocumentType())
                .originalFileName(d.getOriginalFileName())
                .uploadedByRole(d.getUploadedByRole())
                .appointmentId(d.getAppointmentId())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
