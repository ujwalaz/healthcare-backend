package com.healthcare.dto.pmr;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class PmrEntryRequest {

    @NotNull(message = "Appointment ID is required")
    private Long appointmentId;

    @NotNull(message = "Entry date is required")
    private LocalDate entryDate;

    private String diagnosis;
    private String symptoms;
    private String treatmentPlan;
    private String doctorNotes;
}
