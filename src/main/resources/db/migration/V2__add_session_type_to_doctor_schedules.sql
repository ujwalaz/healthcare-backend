-- ============================================================
-- V2 Migration: Add session_type to doctor_schedules
-- Supports Morning + Evening sessions per doctor per day
-- ============================================================

-- 1. Add session_type column (default existing rows to MORNING)
ALTER TABLE doctor_schedules
    ADD session_type NVARCHAR(10) NOT NULL DEFAULT 'MORNING';

-- 2. Check constraint
ALTER TABLE doctor_schedules
    ADD CONSTRAINT chk_session_type CHECK (session_type IN ('MORNING', 'EVENING'));

-- 3. Drop old unique key (one schedule per doctor per day)
ALTER TABLE doctor_schedules DROP CONSTRAINT uq_doctor_day;

-- 4. New unique key: one session type per doctor per day
ALTER TABLE doctor_schedules
    ADD CONSTRAINT uq_doctor_day_session UNIQUE (doctor_id, day_of_week, session_type);
