package com.healthcare.service;

import com.healthcare.constants.MessageCode;
import com.healthcare.entity.Patient;
import com.healthcare.entity.PatientRelationship;
import com.healthcare.exception.AppDeniedException;
import com.healthcare.exception.BadRequestException;
import com.healthcare.exception.ResourceNotFoundException;
import com.healthcare.repository.PatientRelationshipRepository;
import com.healthcare.repository.PatientRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientRelationshipService {
    private final PatientRepository patients;
    private final PatientRelationshipRepository relationships;

    public enum Gender { MALE, FEMALE, OTHER }
    public enum RelationshipType { CHILD, PARENT, SPOUSE, SIBLING, OTHER }

    public record MemberRequest(@NotBlank @Size(max = 255) String name,
                                @NotNull @PastOrPresent LocalDate dob,
                                @NotNull Gender gender, @NotNull RelationshipType relationshipType,
                                boolean authorizedToManage) {}

    public record MemberUpdate(@Size(min = 1, max = 255) String name,
                               @PastOrPresent LocalDate dob, Gender gender,
                               RelationshipType relationshipType) {}

    public record MemberResponse(Long memberId, String name, LocalDate dob,
                                 Gender gender, RelationshipType relationshipType) {}

    @Transactional(readOnly = true)
    public List<MemberResponse> list(Long ownerId) {
        requireActiveOwner(ownerId);
        return relationships.findActiveMembers(ownerId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public MemberResponse create(Long ownerId, MemberRequest request) {
        lockOwner(ownerId);
        if (!request.authorizedToManage()) {
            throw new BadRequestException(MessageCode.VALIDATION_FAILED,
                    "Confirm that you have permission to manage appointments for this family member");
        }
        Patient member = patients.save(Patient.builder()
                .name(request.name().strip()).dob(Date.valueOf(request.dob()))
                .gender(request.gender().name()).isRegistered(false).passwordVersion(0).build());
        PatientRelationship link = new PatientRelationship();
        link.setOwnerPatientId(ownerId);
        link.setMember(member);
        link.setRelationshipType(request.relationshipType().name());
        link.setCreatedAt(LocalDateTime.now());
        return toResponse(relationships.save(link));
    }

    @Transactional
    public MemberResponse update(Long ownerId, Long memberId, MemberUpdate request) {
        lockOwner(ownerId);
        PatientRelationship link = findMember(ownerId, memberId);
        Patient member = link.getMember();
        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw new BadRequestException(MessageCode.VALIDATION_FAILED, "Name cannot be blank");
            }
            member.setName(request.name().strip());
        }
        if (request.dob() != null) member.setDob(Date.valueOf(request.dob()));
        if (request.gender() != null) member.setGender(request.gender().name());
        if (request.relationshipType() != null) link.setRelationshipType(request.relationshipType().name());
        link.setUpdatedAt(LocalDateTime.now());
        return toResponse(link);
    }

    @Transactional
    public void remove(Long ownerId, Long memberId) {
        lockOwner(ownerId);
        findMember(ownerId, memberId).setRevokedAt(LocalDateTime.now());
    }

    public void lockOwner(Long ownerId) {
        patients.lockActiveAccount(ownerId)
                .orElseThrow(() -> new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED));
    }

    public void requireActiveOwner(Long ownerId) {
        if (!patients.existsByIdAndIsRegisteredTrueAndIsDeletedFalse(ownerId)) {
            throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED);
        }
    }

    public void requireMember(Long ownerId, Long memberId) {
        if (relationships.findActiveMember(ownerId, memberId).isEmpty()) {
            throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED);
        }
    }

    private PatientRelationship findMember(Long ownerId, Long memberId) {
        return relationships.findActiveMember(ownerId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageCode.PATIENT_NOT_FOUND));
    }

    private MemberResponse toResponse(PatientRelationship link) {
        Patient member = link.getMember();
        return new MemberResponse(member.getId(), member.getName(), member.getDob().toLocalDate(),
                Gender.valueOf(member.getGender()), RelationshipType.valueOf(link.getRelationshipType()));
    }
}