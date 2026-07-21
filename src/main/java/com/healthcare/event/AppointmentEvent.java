package com.healthcare.event;

public record AppointmentEvent(
        Long appointmentId,
        String eventType,        // "BOOKED" | "CANCELLED" | "COMPLETED"
        Long patientId,
        String patientName,
        Long doctorId,
        String doctorName,
        Long hospitalId,
        String appointmentDate,  // formatted "dd MMM yyyy"
        String startTime,        // formatted "HH:mm"
        String actorRole         // role of who triggered the event (PATIENT or ADMIN)
) {}
