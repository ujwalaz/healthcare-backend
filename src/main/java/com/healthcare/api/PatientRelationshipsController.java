package com.healthcare.api;

import com.healthcare.security.SecurityUtils;
import com.healthcare.service.PatientRelationshipService;
import com.healthcare.service.PatientRelationshipService.MemberRequest;
import com.healthcare.service.PatientRelationshipService.MemberResponse;
import com.healthcare.service.PatientRelationshipService.MemberUpdate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/patients/family-members")
public class PatientRelationshipsController {
    private final PatientRelationshipService relationships;

    @GetMapping
    public List<MemberResponse> list() {
        return relationships.list(ownerId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse create(@Valid @RequestBody MemberRequest request) {
        return relationships.create(ownerId(), request);
    }

    @PatchMapping("/{memberId}")
    public MemberResponse update(@PathVariable("memberId") Long memberId, @Valid @RequestBody MemberUpdate request) {
        return relationships.update(ownerId(), memberId, request);
    }

    @DeleteMapping("/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable("memberId") Long memberId) {
        relationships.remove(ownerId(), memberId);
    }

    private Long ownerId() {
        SecurityUtils.requireRole("PATIENT");
        return SecurityUtils.getCurrentClaims().userId();
    }
}