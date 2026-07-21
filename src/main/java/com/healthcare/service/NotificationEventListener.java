package com.healthcare.service;

import com.healthcare.entity.AdminUser;
import com.healthcare.entity.Notification;
import com.healthcare.event.AppointmentEvent;
import com.healthcare.repository.AdminUserRepository;
import com.healthcare.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationRepository notificationRepository;
    private final AdminUserRepository adminUserRepository;

    @EventListener
    public void onAppointmentEvent(AppointmentEvent event) {
        log.info("Processing AppointmentEvent type={} appointmentId={}", event.eventType(), event.appointmentId());
        switch (event.eventType()) {
            case "BOOKED"    -> handleBooked(event);
            case "CANCELLED" -> handleCancelled(event);
            case "COMPLETED" -> handleCompleted(event);
            default -> log.warn("Unknown AppointmentEvent type: {}", event.eventType());
        }
    }

    private void handleBooked(AppointmentEvent e) {
        // Notify doctor
        saveNotification("DOCTOR", e.doctorId(),
                "New Appointment Scheduled",
                e.patientName() + " has booked an appointment on " + e.appointmentDate() + " at " + e.startTime());

        // Notify all admins of the hospital
        fanOutToAdmins(e.hospitalId(),
                "New Appointment",
                e.patientName() + " booked with Dr. " + e.doctorName() + " on " + e.appointmentDate() + " at " + e.startTime());

        // Notify patient only when they self-booked
        if ("PATIENT".equals(e.actorRole())) {
            saveNotification("PATIENT", e.patientId(),
                    "Appointment Confirmed",
                    "Your appointment with Dr. " + e.doctorName() + " on " + e.appointmentDate() + " at " + e.startTime() + " is confirmed.");
        }
    }

    private void handleCancelled(AppointmentEvent e) {
        if ("PATIENT".equals(e.actorRole())) {
            // Patient cancelled → notify doctor + admins
            saveNotification("DOCTOR", e.doctorId(),
                    "Appointment Cancelled",
                    e.patientName() + " cancelled the appointment on " + e.appointmentDate() + " at " + e.startTime());

            fanOutToAdmins(e.hospitalId(),
                    "Appointment Cancelled",
                    e.patientName() + " cancelled appointment with Dr. " + e.doctorName() + " on " + e.appointmentDate());

        } else if ("ADMIN".equals(e.actorRole())) {
            // Admin cancelled → notify patient + doctor
            saveNotification("PATIENT", e.patientId(),
                    "Appointment Cancelled",
                    "Your appointment with Dr. " + e.doctorName() + " on " + e.appointmentDate() + " at " + e.startTime() + " has been cancelled.");

            saveNotification("DOCTOR", e.doctorId(),
                    "Appointment Cancelled by Admin",
                    "Admin cancelled " + e.patientName() + "'s appointment on " + e.appointmentDate() + " at " + e.startTime());
        }
    }

    private void handleCompleted(AppointmentEvent e) {
        saveNotification("PATIENT", e.patientId(),
                "Appointment Completed",
                "Your appointment with Dr. " + e.doctorName() + " on " + e.appointmentDate() + " has been marked complete.");
    }

    private void fanOutToAdmins(Long hospitalId, String title, String message) {
        List<AdminUser> admins = adminUserRepository.findByHospitalId(hospitalId);
        for (AdminUser admin : admins) {
            saveNotification("ADMIN", admin.getId(), title, message);
        }
    }

    private void saveNotification(String recipientUserType, Long recipientId, String title, String message) {
        Notification notification = Notification.builder()
                .recipientUserType(recipientUserType)
                .recipientId(recipientId)
                .title(title)
                .message(message)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
        log.info("Notification saved: type={} recipientId={} title={}", recipientUserType, recipientId, title);
    }
}
