package com.healthcare.dto.patient;

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
public class PatientResponse {

    private Long id;
    private String name;
    private Integer age;
    private String gender;
    private LocalDate dob;
    private String mobileNumber;
    private String email;
    private LocalDateTime createdAt;
}
