package com.healthcare.service;

import com.healthcare.constants.MessageCode;
import com.healthcare.dto.PagedResponse;
import com.healthcare.dto.pmr.PmrEntryRequest;
import com.healthcare.dto.pmr.PmrEntryResponse;
import com.healthcare.dto.pmr.PmrResponse;
import com.healthcare.entity.Doctor;
import com.healthcare.entity.Hospital;
import com.healthcare.entity.Patient;
import com.healthcare.entity.PatientMedicalRecord;
import com.healthcare.entity.PmrEntry;
import com.healthcare.exception.AppDeniedException;
import com.healthcare.exception.PmrAccessDeniedException;
import com.healthcare.exception.ResourceNotFoundException;
import com.healthcare.repository.DoctorPmrAccessLogRepository;
import com.healthcare.repository.DoctorRepository;
import com.healthcare.repository.HospitalRepository;
import com.healthcare.repository.PatientMedicalRecordRepository;
import com.healthcare.repository.PatientRepository;
import com.healthcare.repository.PmrEntryRepository;
import com.healthcare.security.JwtClaims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PmrService {

    private final PatientMedicalRecordRepository pmrRepository;
    private final PmrEntryRepository pmrEntryRepository;
    private final DoctorPmrAccessLogRepository accessLogRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final HospitalRepository hospitalRepository;

    @Transactional(readOnly = true)
    public PmrResponse getPmr(Long patientId, Long hospitalId, JwtClaims caller) {
        Long resolvedHospitalId = resolveHospitalId(caller, hospitalId);
        verifyPmrAccess(caller, patientId);

        PatientMedicalRecord pmr = pmrRepository.findByPatientIdAndHospitalId(patientId, resolvedHospitalId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageCode.PMR_NOT_FOUND));

        return buildPmrResponse(pmr);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PmrEntryResponse> getPmrEntries(Long patientId, Long hospitalId, Pageable pageable, JwtClaims caller) {
        Long resolvedHospitalId = resolveHospitalId(caller, hospitalId);
        verifyPmrAccess(caller, patientId);

        PatientMedicalRecord pmr = pmrRepository.findByPatientIdAndHospitalId(patientId, resolvedHospitalId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageCode.PMR_NOT_FOUND));

        Page<PmrEntry> page = pmrEntryRepository.findByPmrId(pmr.getId(), pageable);
        Page<PmrEntryResponse> responsePage = page.map(this::toPmrEntryResponse);
        return PagedResponse.from(responsePage);
    }

    @Transactional
    public PmrEntryResponse addPmrEntry(Long patientId, Long hospitalId, PmrEntryRequest request, JwtClaims caller) {
        if (!"DOCTOR".equals(caller.role())) {
            throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED,
                    "Only doctors can add PMR entries");
        }

        verifyDoctorPmrAccess(caller.userId(), patientId);

        Long resolvedHospitalId = caller.hospitalId();
        PatientMedicalRecord pmr = pmrRepository.findByPatientIdAndHospitalId(patientId, resolvedHospitalId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageCode.PMR_NOT_FOUND));

        PmrEntry entry = PmrEntry.builder()
                .pmrId(pmr.getId())
                .doctorId(caller.userId())
                .appointmentId(request.getAppointmentId())
                .entryDate(request.getEntryDate())
                .diagnosis(request.getDiagnosis())
                .symptoms(request.getSymptoms())
                .treatmentPlan(request.getTreatmentPlan())
                .doctorNotes(request.getDoctorNotes())
                .createdAt(LocalDateTime.now())
                .build();

        entry = pmrEntryRepository.save(entry);

        pmr.setUpdatedAt(LocalDateTime.now());
        pmrRepository.save(pmr);

        log.info("PMR entry created with id={} for patientId={}", entry.getId(), patientId);
        return toPmrEntryResponse(entry);
    }

    private Long resolveHospitalId(JwtClaims caller, Long queryHospitalId) {
        return switch (caller.role()) {
            case "PATIENT" -> {
                if (queryHospitalId == null) {
                    throw new AppDeniedException(MessageCode.VALIDATION_FAILED, "hospitalId query parameter is required");
                }
                yield queryHospitalId;
            }
            case "DOCTOR", "ADMIN" -> caller.hospitalId();
            default -> throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED);
        };
    }

    private void verifyPmrAccess(JwtClaims caller, Long patientId) {
        switch (caller.role()) {
            case "PATIENT" -> {
                if (!caller.userId().equals(patientId)) {
                    throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED,
                            "Patients can only access their own medical records");
                }
            }
            case "DOCTOR" -> verifyDoctorPmrAccess(caller.userId(), patientId);
            case "ADMIN" -> throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED,
                    "Admins do not have access to patient medical records");
            default -> throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED);
        }
    }

    private void verifyDoctorPmrAccess(Long doctorId, Long patientId) {
        boolean hasAccess = accessLogRepository
                .findFirstByDoctorIdAndPatientIdAndAccessExpiresAtAfter(doctorId, patientId, LocalDateTime.now())
                .isPresent();
        if (!hasAccess) {
            throw new PmrAccessDeniedException();
        }
    }

    private PmrResponse buildPmrResponse(PatientMedicalRecord pmr) {
        Patient patient = patientRepository.findById(pmr.getPatientId()).orElse(null);
        Hospital hospital = hospitalRepository.findById(pmr.getHospitalId()).orElse(null);

        return PmrResponse.builder()
                .id(pmr.getId())
                .patientId(pmr.getPatientId())
                .patientName(patient != null ? patient.getName() : "Unknown")
                .hospitalId(pmr.getHospitalId())
                .hospitalName(hospital != null ? hospital.getName() : "Unknown")
                .createdAt(pmr.getCreatedAt())
                .updatedAt(pmr.getUpdatedAt())
                .build();
    }

    private PmrEntryResponse toPmrEntryResponse(PmrEntry entry) {
        Doctor doctor = doctorRepository.findById(entry.getDoctorId()).orElse(null);
        return PmrEntryResponse.builder()
                .id(entry.getId())
                .doctorId(entry.getDoctorId())
                .doctorName(doctor != null ? doctor.getName() : "Unknown")
                .appointmentId(entry.getAppointmentId())
                .entryDate(entry.getEntryDate())
                .diagnosis(entry.getDiagnosis())
                .symptoms(entry.getSymptoms())
                .treatmentPlan(entry.getTreatmentPlan())
                .doctorNotes(entry.getDoctorNotes())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
