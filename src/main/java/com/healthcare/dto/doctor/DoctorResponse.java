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
public class DoctorResponse {

    private Long id;
    private String name;
    private String specialization;
    private String education;
    private String phone;
    private String email;
    private Long hospitalId;
    private String hospitalName;
    private Boolean isActive;
}
