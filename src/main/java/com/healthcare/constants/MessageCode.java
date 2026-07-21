package com.healthcare.constants;

public enum MessageCode {

    // Generic
    SUCCESS("SUCCESS", "Operation completed successfully"),
    CREATED("CREATED", "Resource created successfully"),
    UPDATED("UPDATED", "Resource updated successfully"),
    DELETED("DELETED", "Resource deleted successfully"),
    FETCHED("FETCHED", "Resource fetched successfully"),
    VALIDATION_FAILED("VALIDATION_FAILED", "Validation failed"),
    INTERNAL_ERROR("INTERNAL_ERROR", "An internal error occurred"),

    // Auth
    AUTH_LOGIN_SUCCESS("AUTH_LOGIN_SUCCESS", "Login successful"),
    AUTH_REGISTER_SUCCESS("AUTH_REGISTER_SUCCESS", "Registration successful"),
    AUTH_INVALID_CREDENTIALS("AUTH_INVALID_CREDENTIALS", "Invalid credentials"),
    AUTH_ACCOUNT_INACTIVE("AUTH_ACCOUNT_INACTIVE", "Account is inactive"),
    AUTH_TOKEN_INVALID("AUTH_TOKEN_INVALID", "Authentication token is invalid or expired"),
    AUTH_UNAUTHORIZED("AUTH_UNAUTHORIZED", "You are not authorized to perform this action"),

    // Patient
    PATIENT_FETCHED("PATIENT_FETCHED", "Patient fetched successfully"),
    PATIENT_PROFILE_UPDATED("PATIENT_PROFILE_UPDATED", "Patient profile updated successfully"),
    PATIENT_NOT_FOUND("PATIENT_NOT_FOUND", "Patient not found"),
    PATIENT_MOBILE_EXISTS("PATIENT_MOBILE_EXISTS", "A patient with this mobile number already exists"),

    // Hospital
    HOSPITAL_FETCHED("HOSPITAL_FETCHED", "Hospital fetched successfully"),
    HOSPITAL_NOT_FOUND("HOSPITAL_NOT_FOUND", "Hospital not found"),

    // Doctor
    DOCTOR_FETCHED("DOCTOR_FETCHED", "Doctor fetched successfully"),
    DOCTOR_NOT_FOUND("DOCTOR_NOT_FOUND", "Doctor not found"),
    DOCTOR_SCHEDULE_FETCHED("DOCTOR_SCHEDULE_FETCHED", "Doctor schedule fetched successfully"),
    DOCTOR_SCHEDULE_SAVED("DOCTOR_SCHEDULE_SAVED", "Doctor schedule saved successfully"),
    DOCTOR_CALENDAR_FETCHED("DOCTOR_CALENDAR_FETCHED", "Doctor calendar fetched successfully"),

    // Appointment
    APPOINTMENT_BOOKED("APPOINTMENT_BOOKED", "Appointment booked successfully"),
    APPOINTMENT_FETCHED("APPOINTMENT_FETCHED", "Appointment fetched successfully"),
    APPOINTMENT_STATUS_UPDATED("APPOINTMENT_STATUS_UPDATED", "Appointment status updated successfully"),
    APPOINTMENT_SLOTS_FETCHED("APPOINTMENT_SLOTS_FETCHED", "Available slots fetched successfully"),
    APPOINTMENT_NOT_FOUND("APPOINTMENT_NOT_FOUND", "Appointment not found"),
    APPOINTMENT_SLOT_UNAVAILABLE("APPOINTMENT_SLOT_UNAVAILABLE", "The requested appointment slot is already booked"),
    APPOINTMENT_SLOT_INVALID("APPOINTMENT_SLOT_INVALID", "The requested slot does not match any valid schedule slot"),
    APPOINTMENT_TRANSITION_INVALID("APPOINTMENT_TRANSITION_INVALID", "Invalid appointment status transition"),

    // PMR
    PMR_FETCHED("PMR_FETCHED", "Medical record fetched successfully"),
    PMR_ENTRIES_FETCHED("PMR_ENTRIES_FETCHED", "Medical record entries fetched successfully"),
    PMR_ENTRY_CREATED("PMR_ENTRY_CREATED", "Medical record entry created successfully"),
    PMR_NOT_FOUND("PMR_NOT_FOUND", "Medical record not found"),
    PMR_ACCESS_DENIED("PMR_ACCESS_DENIED", "You do not have active access to this patient's medical record"),

    // Document
    DOCUMENT_UPLOADED("DOCUMENT_UPLOADED", "Document uploaded successfully"),
    DOCUMENT_FETCHED("DOCUMENT_FETCHED", "Documents fetched successfully"),
    DOCUMENT_DOWNLOAD_URL_READY("DOCUMENT_DOWNLOAD_URL_READY", "Download URL generated successfully"),
    DOCUMENT_REMOVED("DOCUMENT_REMOVED", "Document removed successfully"),
    DOCUMENT_NOT_FOUND("DOCUMENT_NOT_FOUND", "Document not found"),
    DOCUMENT_ACCESS_DENIED("DOCUMENT_ACCESS_DENIED", "You do not have access to this document"),

    // Notification
    NOTIFICATION_SENT("NOTIFICATION_SENT", "Notification sent successfully"),
    NOTIFICATION_FETCHED("NOTIFICATION_FETCHED", "Notifications fetched successfully"),
    NOTIFICATION_MARKED_READ("NOTIFICATION_MARKED_READ", "Notification marked as read"),
    NOTIFICATION_NOT_FOUND("NOTIFICATION_NOT_FOUND", "Notification not found"),
    NOTIFICATION_COUNT_FETCHED("NOTIFICATION_COUNT_FETCHED", "Unread notification count fetched"),
    NOTIFICATION_ALL_MARKED_READ("NOTIFICATION_ALL_MARKED_READ", "All notifications marked as read");

    private final String code;
    private final String defaultMessage;

    MessageCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}