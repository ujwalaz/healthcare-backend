package com.healthcare.dto.doctor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorSummaryResponse {

    private Long id;
    private String name;
    private String specialization;
    private String education;
    private Boolean isActive;
}
