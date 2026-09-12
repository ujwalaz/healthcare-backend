package com.healthcare.api;

import com.healthcare.annotation.ApiMessage;
import com.healthcare.constants.MessageCode;
import com.healthcare.dto.DocumentResponse;
import com.healthcare.dto.DocumentSummaryPagedResponse;
import com.healthcare.dto.DocumentSummaryResponse;
import com.healthcare.dto.DownloadUrlResponse;
import com.healthcare.dto.PagedResponse;
import com.healthcare.security.JwtClaims;
import com.healthcare.security.SecurityUtils;
import com.healthcare.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class DocumentsApiImpl implements DocumentsApi {

    private final DocumentService documentService;

    @Override
    @ApiMessage(MessageCode.DOCUMENT_UPLOADED)
    public ResponseEntity<DocumentResponse> uploadDocument(MultipartFile file, Long patientId, Long hospitalId,
                                                            String documentType, Long appointmentId) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        DocumentResponse response = documentService.upload(file, patientId, hospitalId, appointmentId, documentType, caller);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @ApiMessage(MessageCode.DOCUMENT_FETCHED)
    public ResponseEntity<DocumentSummaryPagedResponse> getPatientDocuments(Long patientId, String documentType,
                                                                             Long appointmentId, Integer page, Integer size) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<DocumentSummaryResponse> paged =
                documentService.getDocuments(patientId, documentType, appointmentId, pageable, caller);
        DocumentSummaryPagedResponse response = new DocumentSummaryPagedResponse()
                .content(paged.getContent())
                .page(paged.getPage())
                .size(paged.getSize())
                .totalElements(paged.getTotalElements())
                .totalPages(paged.getTotalPages());
        return ResponseEntity.ok(response);
    }

    @Override
    @ApiMessage(MessageCode.DOCUMENT_DOWNLOAD_URL_READY)
    public ResponseEntity<DownloadUrlResponse> getDocumentDownloadUrl(Long id) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        DownloadUrlResponse response = documentService.getDownloadUrl(id, caller);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> deleteDocument(Long id) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        documentService.deleteDocument(id, caller);
        return ResponseEntity.ok().build();
    }
}
