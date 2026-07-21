package com.healthcare.controller;

import com.healthcare.dto.*;
import com.healthcare.constants.MessageCode;
import com.healthcare.security.JwtClaims;
import com.healthcare.security.SecurityUtils;
import com.healthcare.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<DocumentResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam Long patientId,
            @RequestParam Long hospitalId,
            @RequestParam(required = false) Long appointmentId,
            @RequestParam String documentType) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        DocumentResponse response = documentService.upload(file, patientId, hospitalId, appointmentId, documentType, caller);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(MessageCode.DOCUMENT_UPLOADED, response));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<PagedResponse<DocumentSummaryResponse>>> getDocuments(
            @PathVariable Long patientId,
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) Long appointmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<DocumentSummaryResponse> response =
                documentService.getDocuments(patientId, documentType, appointmentId, pageable, caller);
        return ResponseEntity.ok(ApiResponse.ok(MessageCode.DOCUMENT_FETCHED, response));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<ApiResponse<DownloadUrlResponse>> getDownloadUrl(@PathVariable Long id) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        DownloadUrlResponse response = documentService.getDownloadUrl(id, caller);
        return ResponseEntity.ok(ApiResponse.ok(MessageCode.DOCUMENT_DOWNLOAD_URL_READY, response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable Long id) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        documentService.deleteDocument(id, caller);
        return ResponseEntity.ok(ApiResponse.ok(MessageCode.DOCUMENT_REMOVED, null));
    }
}