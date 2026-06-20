package com.healthcare.dto.hospital;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class HospitalResponse {

    private Long id;
    private String name;
    private String address;
    private String city;
    private String phone;
    private String email;
}
