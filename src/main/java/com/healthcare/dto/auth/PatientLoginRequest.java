package com.healthcare.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PatientLoginRequest {

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "[0-9]{10}", message = "Mobile number must be 10 digits")
    private String mobileNumber;
}
