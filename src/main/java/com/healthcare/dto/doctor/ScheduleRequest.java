package com.healthcare.dto.doctor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
public class ScheduleRequest {

    @NotNull(message = "Day of week is required")
    @Min(value = 0, message = "Day of week must be between 0 (Monday) and 6 (Sunday)")
    @Max(value = 6, message = "Day of week must be between 0 (Monday) and 6 (Sunday)")
    private Integer dayOfWeek;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @NotNull(message = "Slot duration is required")
    @Min(value = 5, message = "Slot duration must be at least 5 minutes")
    private Integer slotDurationMinutes;
}
