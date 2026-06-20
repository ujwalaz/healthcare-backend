package com.healthcare.dto.appointment;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StatusUpdateRequest {

    @NotBlank(message = "Status is required")
    private String status;

    private String notes;
}
