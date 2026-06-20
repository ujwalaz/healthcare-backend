package com.healthcare.dto.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentSummaryResponse {

    private Long id;
    private String documentType;
    private String originalFileName;
    private String uploadedByRole;
    private Long appointmentId;
    private LocalDateTime createdAt;
}
