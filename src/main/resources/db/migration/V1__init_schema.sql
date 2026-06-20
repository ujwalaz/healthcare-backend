-- ============================================================
-- Healthcare OPD Platform - Phase 1 Schema
-- Flyway Migration: V1__init_schema.sql
-- Database: Microsoft SQL Server 2019+
-- ============================================================

-- --------------------------------------------------------
-- 1. hospitals
-- --------------------------------------------------------
CREATE TABLE hospitals (
    id         BIGINT        NOT NULL IDENTITY(1,1),
    name       NVARCHAR(255) NOT NULL,
    address    NVARCHAR(MAX),
    city       NVARCHAR(100),
    phone      NVARCHAR(20)  NOT NULL,
    email      NVARCHAR(150),
    created_at DATETIME2     NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_hospitals PRIMARY KEY (id)
);

-- --------------------------------------------------------
-- 2. doctors
-- --------------------------------------------------------
CREATE TABLE doctors (
    id             BIGINT        NOT NULL IDENTITY(1,1),
    hospital_id    BIGINT        NOT NULL,
    name           NVARCHAR(255) NOT NULL,
    education      NVARCHAR(255) NOT NULL,
    specialization NVARCHAR(255),
    phone          NVARCHAR(20),
    email          NVARCHAR(150) NOT NULL,
    password_hash  NVARCHAR(255) NOT NULL,
    is_active      BIT           NOT NULL DEFAULT 1,
    created_at     DATETIME2     NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_doctors             PRIMARY KEY (id),
    CONSTRAINT uq_doctors_email       UNIQUE (email),
	CONSTRAINT uq_doctors_phone       UNIQUE (phone),
    CONSTRAINT fk_doctors_hospital    FOREIGN KEY (hospital_id)
        REFERENCES hospitals (id) ON DELETE NO ACTION
);

-- --------------------------------------------------------
-- 3. admin_users  (Nursing Desk / Reception)
-- --------------------------------------------------------
CREATE TABLE admin_users (
    id            BIGINT        NOT NULL IDENTITY(1,1),
    hospital_id   BIGINT        NOT NULL,
    name          NVARCHAR(255) NOT NULL,
    mobile_number NVARCHAR(20)  NOT NULL,
    email         NVARCHAR(150),
    password_hash NVARCHAR(255) NOT NULL,
    role          NVARCHAR(20)  NOT NULL DEFAULT 'RECEPTION',
    is_active     BIT           NOT NULL DEFAULT 1,
    created_at    DATETIME2     NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_admin_users         PRIMARY KEY (id),
    CONSTRAINT uq_admin_mobile        UNIQUE (mobile_number),
    CONSTRAINT chk_admin_role         CHECK (role IN ('RECEPTION', 'NURSING')),
    CONSTRAINT fk_admin_hospital      FOREIGN KEY (hospital_id)
        REFERENCES hospitals (id) ON DELETE NO ACTION
);

-- --------------------------------------------------------
-- 4. patients
-- --------------------------------------------------------
CREATE TABLE patients (
    id            BIGINT        NOT NULL IDENTITY(1,1),
    name          NVARCHAR(255) NOT NULL,
    age           INT           NOT NULL,
    gender        NVARCHAR(10)  NOT NULL,
    dob           DATE,
    mobile_number NVARCHAR(20)  NOT NULL,
    email         NVARCHAR(150),
    created_at    DATETIME2     NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_patients            PRIMARY KEY (id),
    CONSTRAINT uq_patients_mobile     UNIQUE (mobile_number),
    CONSTRAINT chk_patients_gender    CHECK (gender IN ('MALE', 'FEMALE', 'OTHER'))
);

-- --------------------------------------------------------
-- 5. patient_hospital_links
-- --------------------------------------------------------
CREATE TABLE patient_hospital_links (
    patient_id  BIGINT    NOT NULL,
    hospital_id BIGINT    NOT NULL,
    linked_at   DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_phl         PRIMARY KEY (patient_id, hospital_id),
    CONSTRAINT fk_phl_patient FOREIGN KEY (patient_id)
        REFERENCES patients (id) ON DELETE CASCADE,
    CONSTRAINT fk_phl_hospital FOREIGN KEY (hospital_id)
        REFERENCES hospitals (id)
        -- SQL Server does not allow CASCADE on both sides of a composite FK;
        -- hospital deletion handled at application layer.
        ON DELETE NO ACTION
);

-- --------------------------------------------------------
-- 6. doctor_schedules
--    Recurring weekly availability per doctor.
--    day_of_week: 0 = Monday ... 6 = Sunday
-- --------------------------------------------------------
CREATE TABLE doctor_schedules (
    id                    BIGINT   NOT NULL IDENTITY(1,1),
    doctor_id             BIGINT   NOT NULL,
    day_of_week           TINYINT  NOT NULL, -- 0=Mon, 1=Tue, 2=Wed, 3=Thu, 4=Fri, 5=Sat, 6=Sun
    start_time            TIME     NOT NULL,
    end_time              TIME     NOT NULL,
    slot_duration_minutes INT      NOT NULL DEFAULT 30,
    is_active             BIT      NOT NULL DEFAULT 1,
    CONSTRAINT pk_doctor_schedules    PRIMARY KEY (id),
    CONSTRAINT uq_doctor_day          UNIQUE (doctor_id, day_of_week),
    CONSTRAINT chk_day_of_week        CHECK (day_of_week BETWEEN 0 AND 6),
    CONSTRAINT fk_schedule_doctor     FOREIGN KEY (doctor_id)
        REFERENCES doctors (id) ON DELETE CASCADE
);

-- --------------------------------------------------------
-- 7. appointments
--    start_time/end_time derived from doctor_schedules.
--    UNIQUE constraint prevents double-booking.
-- --------------------------------------------------------
CREATE TABLE appointments (
    id               BIGINT        NOT NULL IDENTITY(1,1),
    patient_id       BIGINT        NOT NULL,
    doctor_id        BIGINT        NOT NULL,
    hospital_id      BIGINT        NOT NULL,
    appointment_date DATE          NOT NULL,
    start_time       TIME          NOT NULL,
    end_time         TIME          NOT NULL,
    booked_by_role   NVARCHAR(10)  NOT NULL,
    booked_by_id     BIGINT        NOT NULL,
    is_offline       BIT           NOT NULL DEFAULT 0,
    status           NVARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
    notes            NVARCHAR(MAX),
    created_at       DATETIME2     NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_appointments        PRIMARY KEY (id),
    CONSTRAINT uq_appt_slot           UNIQUE (doctor_id, appointment_date, start_time),
    CONSTRAINT chk_appt_booked_role   CHECK (booked_by_role IN ('PATIENT', 'ADMIN')),
    CONSTRAINT chk_appt_status        CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT fk_appt_patient        FOREIGN KEY (patient_id)
        REFERENCES patients (id) ON DELETE NO ACTION,
    CONSTRAINT fk_appt_doctor         FOREIGN KEY (doctor_id)
        REFERENCES doctors (id) ON DELETE NO ACTION,
    CONSTRAINT fk_appt_hospital       FOREIGN KEY (hospital_id)
        REFERENCES hospitals (id) ON DELETE NO ACTION
);

-- --------------------------------------------------------
-- 8. patient_medical_records  (PMR)
--    One record per patient per hospital.
-- --------------------------------------------------------
CREATE TABLE patient_medical_records (
    id          BIGINT    NOT NULL IDENTITY(1,1),
    patient_id  BIGINT    NOT NULL,
    hospital_id BIGINT    NOT NULL,
    created_at  DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at  DATETIME2,
    CONSTRAINT pk_pmr             PRIMARY KEY (id),
    CONSTRAINT uq_pmr             UNIQUE (patient_id, hospital_id),
    CONSTRAINT fk_pmr_patient     FOREIGN KEY (patient_id)
        REFERENCES patients (id) ON DELETE NO ACTION,
    CONSTRAINT fk_pmr_hospital    FOREIGN KEY (hospital_id)
        REFERENCES hospitals (id) ON DELETE NO ACTION
);

-- --------------------------------------------------------
-- 9. pmr_entries
--    Individual clinical notes written by doctors.
-- --------------------------------------------------------
CREATE TABLE pmr_entries (
    id             BIGINT        NOT NULL IDENTITY(1,1),
    pmr_id         BIGINT        NOT NULL,
    doctor_id      BIGINT        NOT NULL,
    appointment_id BIGINT        NOT NULL,
    entry_date     DATE          NOT NULL,
    diagnosis      NVARCHAR(MAX),
    symptoms       NVARCHAR(MAX),
    treatment_plan NVARCHAR(MAX),
    doctor_notes   NVARCHAR(MAX),
    created_at     DATETIME2     NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_pmr_entries           PRIMARY KEY (id),
    CONSTRAINT fk_entry_pmr             FOREIGN KEY (pmr_id)
        REFERENCES patient_medical_records (id) ON DELETE CASCADE,
    CONSTRAINT fk_entry_doctor          FOREIGN KEY (doctor_id)
        REFERENCES doctors (id) ON DELETE NO ACTION,
    CONSTRAINT fk_entry_appointment     FOREIGN KEY (appointment_id)
        REFERENCES appointments (id) ON DELETE NO ACTION
);

-- --------------------------------------------------------
-- 10. documents
--     Metadata for files stored in Azure Blob Storage.
-- --------------------------------------------------------
CREATE TABLE documents (
    id                    BIGINT        NOT NULL IDENTITY(1,1),
    patient_id            BIGINT        NOT NULL,
    hospital_id           BIGINT        NOT NULL,
    appointment_id        BIGINT,
    document_type         NVARCHAR(20)  NOT NULL,
    storage_key           NVARCHAR(500) NOT NULL, -- Azure Blob Storage object path
    original_file_name    NVARCHAR(255),
    uploaded_by_role      NVARCHAR(10)  NOT NULL,
    uploaded_by_id        BIGINT        NOT NULL,
    is_visible_to_patient BIT           NOT NULL DEFAULT 1,
    created_at            DATETIME2     NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_documents           PRIMARY KEY (id),
    CONSTRAINT chk_doc_type           CHECK (document_type IN ('BILL', 'PRESCRIPTION', 'REPORT', 'DOCTOR_NOTE', 'PREVIOUS_RECORD')),
    CONSTRAINT chk_doc_uploaded_role  CHECK (uploaded_by_role IN ('PATIENT', 'ADMIN', 'DOCTOR')),
    CONSTRAINT fk_doc_patient         FOREIGN KEY (patient_id)
        REFERENCES patients (id) ON DELETE NO ACTION,
    CONSTRAINT fk_doc_hospital        FOREIGN KEY (hospital_id)
        REFERENCES hospitals (id) ON DELETE NO ACTION,
    CONSTRAINT fk_doc_appointment     FOREIGN KEY (appointment_id)
        REFERENCES appointments (id) ON DELETE SET NULL
);

-- --------------------------------------------------------
-- 11. doctor_pmr_access_log
--     Grants doctors time-limited access to a patient PMR
--     scoped to a specific appointment window.
-- --------------------------------------------------------
CREATE TABLE doctor_pmr_access_log (
    id                BIGINT    NOT NULL IDENTITY(1,1),
    doctor_id         BIGINT    NOT NULL,
    patient_id        BIGINT    NOT NULL,
    appointment_id    BIGINT    NOT NULL,
    access_granted_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    access_expires_at DATETIME2 NOT NULL,
    CONSTRAINT pk_pmr_access_log          PRIMARY KEY (id),
    CONSTRAINT uq_access_appt             UNIQUE (doctor_id, appointment_id),
    CONSTRAINT fk_access_doctor           FOREIGN KEY (doctor_id)
        REFERENCES doctors (id) ON DELETE CASCADE,
    CONSTRAINT fk_access_patient          FOREIGN KEY (patient_id)
        REFERENCES patients (id) ON DELETE NO ACTION,
    CONSTRAINT fk_access_appointment      FOREIGN KEY (appointment_id)
        REFERENCES appointments (id) ON DELETE NO ACTION
);

-- --------------------------------------------------------
-- 12. notifications
--     In-app notifications for all user types.
-- --------------------------------------------------------
CREATE TABLE notifications (
    id                  BIGINT        NOT NULL IDENTITY(1,1),
    recipient_user_type NVARCHAR(10)  NOT NULL,
    recipient_id        BIGINT        NOT NULL,
    title               NVARCHAR(255) NOT NULL,
    message             NVARCHAR(MAX) NOT NULL,
    is_read             BIT           NOT NULL DEFAULT 0,
    created_at          DATETIME2     NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_notifications           PRIMARY KEY (id),
    CONSTRAINT chk_notif_recipient_type   CHECK (recipient_user_type IN ('PATIENT', 'DOCTOR', 'ADMIN'))
);

-- --------------------------------------------------------
-- Indexes for common query patterns
-- --------------------------------------------------------
CREATE INDEX idx_doctors_hospital      ON doctors (hospital_id);
CREATE INDEX idx_appointments_patient  ON appointments (patient_id, appointment_date);
CREATE INDEX idx_appointments_doctor   ON appointments (doctor_id, appointment_date);
CREATE INDEX idx_appointments_hospital ON appointments (hospital_id, appointment_date);
CREATE INDEX idx_documents_patient     ON documents (patient_id, document_type);
CREATE INDEX idx_pmr_entries_pmr       ON pmr_entries (pmr_id, entry_date);
CREATE INDEX idx_access_log_doctor     ON doctor_pmr_access_log (doctor_id, access_expires_at);
CREATE INDEX idx_notif_recipient       ON notifications (recipient_user_type, recipient_id, is_read);
