package com.healthcare.repository;

import com.healthcare.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    java.util.Optional<Document> findFirstByDoctorIdAndDocumentTypeAndIsVisibleToPatientTrueAndIsDeletedFalseOrderByCreatedAtDesc(
            Long doctorId, String documentType);

    @Query("SELECT d FROM Document d WHERE d.patientId = :patientId AND d.isDeleted = false AND d.isVisibleToPatient = true " +
           "AND (:documentType IS NULL OR d.documentType = :documentType) " +
           "AND (:appointmentId IS NULL OR d.appointmentId = :appointmentId)")
    Page<Document> findVisibleByPatientWithFilters(
            @Param("patientId") Long patientId,
            @Param("documentType") String documentType,
            @Param("appointmentId") Long appointmentId,
            Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.hospitalId = :hospitalId AND d.isDeleted = false " +
           "AND (:documentType IS NULL OR d.documentType = :documentType) " +
           "AND (:patientId IS NULL OR d.patientId = :patientId) " +
           "AND (:appointmentId IS NULL OR d.appointmentId = :appointmentId)")
    Page<Document> findByHospitalWithFilters(
            @Param("hospitalId") Long hospitalId,
            @Param("patientId") Long patientId,
            @Param("documentType") String documentType,
            @Param("appointmentId") Long appointmentId,
            Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.patientId = :patientId AND d.isDeleted = false AND d.uploadedByRole = 'PATIENT' " +
           "AND (:documentType IS NULL OR d.documentType = :documentType)")
    Page<Document> findPatientUploadedByPatient(
            @Param("patientId") Long patientId,
            @Param("documentType") String documentType,
            Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.patientId = :patientId AND d.isDeleted = false " +
           "AND (:documentType IS NULL OR d.documentType = :documentType) " +
           "AND (:appointmentId IS NULL OR d.appointmentId = :appointmentId)")
    Page<Document> findByPatientWithFilters(
            @Param("patientId") Long patientId,
            @Param("documentType") String documentType,
            @Param("appointmentId") Long appointmentId,
            Pageable pageable);
}