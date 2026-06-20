package com.healthcare.dto.pmr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PmrEntryResponse {

    private Long id;
    private Long doctorId;
    private String doctorName;
    private Long appointmentId;
    private LocalDate entryDate;
    private String diagnosis;
    private String symptoms;
    private String treatmentPlan;
    private String doctorNotes;
    private LocalDateTime createdAt;
}
