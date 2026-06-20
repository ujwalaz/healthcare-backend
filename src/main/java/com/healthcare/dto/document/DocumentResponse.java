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
public class DocumentResponse {

    private Long id;
    private Long patientId;
    private Long hospitalId;
    private Long appointmentId;
    private String documentType;
    private String originalFileName;
    private String uploadedByRole;
    private Boolean isVisibleToPatient;
    private LocalDateTime createdAt;
}
