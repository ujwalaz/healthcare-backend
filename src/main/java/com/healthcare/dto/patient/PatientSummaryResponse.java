package com.healthcare.dto.patient;

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
public class PatientSummaryResponse {

    private Long id;
    private String name;
    private String mobileNumber;
    private Integer age;
    private String gender;
}
