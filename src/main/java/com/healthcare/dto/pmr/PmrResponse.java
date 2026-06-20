package com.healthcare.dto.pmr;

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
public class PmrResponse {

    private Long id;
    private Long patientId;
    private String patientName;
    private Long hospitalId;
    private String hospitalName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
