package com.healthcare.dto.auth;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class PatientRegisterRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Age is required")
    @Min(value = 0, message = "Age must be 0 or greater")
    private Integer age;

    @NotNull(message = "Gender is required")
    private String gender;

    private LocalDate dob;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "[0-9]{10}", message = "Mobile number must be 10 digits")
    private String mobileNumber;

    private String email;
}
