package com.healthcare.repository;

import com.healthcare.entity.PatientRelationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PatientRelationshipRepository extends JpaRepository<PatientRelationship, Long> {
    @Query("select r from PatientRelationship r join fetch r.member m " +
           "where r.ownerPatientId = :ownerId and r.revokedAt is null and m.isDeleted = false order by r.id")
    List<PatientRelationship> findActiveMembers(@Param("ownerId") Long ownerId);

    @Query("select r from PatientRelationship r join fetch r.member m where r.ownerPatientId = :ownerId " +
           "and m.id = :memberId and r.revokedAt is null and m.isDeleted = false")
    Optional<PatientRelationship> findActiveMember(@Param("ownerId") Long ownerId, @Param("memberId") Long memberId);
}