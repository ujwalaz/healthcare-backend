package com.healthcare.dto.patient;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class PatientUpdateRequest {

    private String name;
    private Integer age;
    private LocalDate dob;
    private String email;
}
