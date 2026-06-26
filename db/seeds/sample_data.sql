-- ============================================================
-- Healthcare OPD Platform – Sample Data Seed
-- Database: Microsoft SQL Server 2019+
-- Run manually (NOT a Flyway migration).
-- Covers: hospitals, doctors, admin_users, patients,
--         patient_hospital_links, doctor_schedules,
--         appointments, documents, notifications
-- PMR tables intentionally excluded.
-- ============================================================

SET IDENTITY_INSERT hospitals ON;
SET IDENTITY_INSERT doctors ON;
SET IDENTITY_INSERT admin_users ON;
SET IDENTITY_INSERT patients ON;
SET IDENTITY_INSERT appointments ON;
SET IDENTITY_INSERT documents ON;
SET IDENTITY_INSERT notifications ON;

-- --------------------------------------------------------
-- 1. Hospitals
-- --------------------------------------------------------
INSERT INTO hospitals (id, name, address, city, phone, email) VALUES
  (1, 'City Care Hospital',      '123 MG Road, Shivajinagar',   'Pune',      '020-11112222', 'info@citycare.in'),
  (2, 'Sunrise Medical Centre',  '45 Link Road, Andheri West',  'Mumbai',    '022-33334444', 'contact@sunrisemc.in'),
  (3, 'Green Valley Clinic',     '8 Ring Road, Koramangala',    'Bangalore', '080-55556666', 'hello@greenvalley.in');

-- --------------------------------------------------------
-- 2. Doctors
-- --------------------------------------------------------
INSERT INTO doctors (id, hospital_id, name, education, specialization, phone, email, password_hash, is_active) VALUES
  (1,  1, 'Dr. Anil Mehta',     'MBBS, MD (Cardiology)',      'Cardiology',        '9901001001', 'anil.mehta@citycare.in',      '$2a$10$dummyhashDoctorAnil',   1),
  (2,  1, 'Dr. Sneha Joshi',    'MBBS, MS (Ortho)',           'Orthopaedics',      '9901001002', 'sneha.joshi@citycare.in',     '$2a$10$dummyhashDoctorSneha',  1),
  (3,  1, 'Dr. Ramesh Kulkarni','MBBS, DNB (General)',        'General Medicine',  '9901001003', 'ramesh.k@citycare.in',        '$2a$10$dummyhashDoctorRamesh', 1),
  (4,  2, 'Dr. Priya Nair',     'MBBS, MD (Paediatrics)',     'Paediatrics',       '9902002001', 'priya.nair@sunrisemc.in',     '$2a$10$dummyhashDoctorPriya',  1),
  (5,  2, 'Dr. Kiran Shah',     'MBBS, DGO',                  'Gynaecology',       '9902002002', 'kiran.shah@sunrisemc.in',     '$2a$10$dummyhashDoctorKiran',  1),
  (6,  3, 'Dr. Venkat Rao',     'MBBS, MD (Dermatology)',     'Dermatology',       '9903003001', 'venkat.rao@greenvalley.in',   '$2a$10$dummyhashDoctorVenkat', 1);

-- --------------------------------------------------------
-- 3. Admin Users (Nursing Desk / Reception)
-- --------------------------------------------------------
INSERT INTO admin_users (id, hospital_id, name, mobile_number, email, password_hash, role, is_active) VALUES
  (1, 1, 'Meena Patil',   '8800001001', 'meena@citycare.in',     '$2a$10$dummyhashAdminMeena',  'RECEPTION', 1),
  (2, 1, 'Suresh Rao',    '8800001002', 'suresh@citycare.in',    '$2a$10$dummyhashAdminSuresh', 'NURSING',   1),
  (3, 2, 'Lakshmi Iyer',  '8800002001', 'lakshmi@sunrisemc.in',  '$2a$10$dummyhashAdminLaxmi',  'RECEPTION', 1),
  (4, 3, 'Ravi Shetty',   '8800003001', 'ravi@greenvalley.in',   '$2a$10$dummyhashAdminRavi',   'RECEPTION', 1);

-- --------------------------------------------------------
-- 4. Patients
-- --------------------------------------------------------
INSERT INTO patients (id, name, age, gender, dob, mobile_number, email) VALUES
  (1,  'Riya Sharma',      28, 'FEMALE', '1997-04-15', '9876540001', 'riya.sharma@gmail.com'),
  (2,  'Arjun Desai',      35, 'MALE',   '1990-08-22', '9876540002', 'arjun.desai@gmail.com'),
  (3,  'Kavita Nair',      42, 'FEMALE', '1983-11-05', '9876540003', 'kavita.nair@gmail.com'),
  (4,  'Mohammed Shaikh',  29, 'MALE',   '1996-02-18', '9876540004', 'mshaikh@gmail.com'),
  (5,  'Sunita Yadav',     55, 'FEMALE', '1970-07-30', '9876540005', 'sunita.yadav@gmail.com'),
  (6,  'Deepak Verma',     40, 'MALE',   '1985-03-12', '9876540006', 'deepak.v@gmail.com'),
  (7,  'Pooja Iyer',       24, 'FEMALE', '2001-09-01', '9876540007', 'pooja.iyer@gmail.com'),
  (8,  'Rahul Gupta',      33, 'MALE',   '1992-06-25', '9876540008', 'rahul.g@gmail.com'),
  (9,  'Anita Pillai',     48, 'FEMALE', '1977-12-10', '9876540009', 'anita.p@gmail.com'),
  (10, 'Sanjay Kulkarni',  61, 'MALE',   '1964-05-03', '9876540010', 'sanjay.k@gmail.com');

-- --------------------------------------------------------
-- 5. Patient–Hospital Links
-- --------------------------------------------------------
INSERT INTO patient_hospital_links (patient_id, hospital_id) VALUES
  (1, 1), (1, 2),   -- Riya visits City Care + Sunrise
  (2, 1),
  (3, 1), (3, 3),   -- Kavita visits City Care + Green Valley
  (4, 2),
  (5, 1),
  (6, 1), (6, 2),
  (7, 2),
  (8, 3),
  (9, 1),
  (10, 1), (10, 3);

-- --------------------------------------------------------
-- 6. Doctor Schedules
--    day_of_week: 0=Mon 1=Tue 2=Wed 3=Thu 4=Fri 5=Sat 6=Sun
-- --------------------------------------------------------
INSERT INTO doctor_schedules (doctor_id, day_of_week, start_time, end_time, slot_duration_minutes, is_active) VALUES
  -- Dr. Anil Mehta (Cardiology, City Care) – Mon/Wed/Fri mornings
  (1, 0, '09:00', '12:00', 30, 1),
  (1, 2, '09:00', '12:00', 30, 1),
  (1, 4, '09:00', '12:00', 30, 1),

  -- Dr. Sneha Joshi (Ortho, City Care) – Tue/Thu/Sat
  (2, 1, '10:00', '13:00', 30, 1),
  (2, 3, '10:00', '13:00', 30, 1),
  (2, 5, '09:00', '12:00', 30, 1),

  -- Dr. Ramesh Kulkarni (General, City Care) – Mon–Sat afternoons
  (3, 0, '14:00', '18:00', 20, 1),
  (3, 1, '14:00', '18:00', 20, 1),
  (3, 2, '14:00', '18:00', 20, 1),
  (3, 3, '14:00', '18:00', 20, 1),
  (3, 4, '14:00', '18:00', 20, 1),
  (3, 5, '10:00', '13:00', 20, 1),

  -- Dr. Priya Nair (Paediatrics, Sunrise) – Mon/Wed/Fri
  (4, 0, '09:30', '12:30', 20, 1),
  (4, 2, '09:30', '12:30', 20, 1),
  (4, 4, '09:30', '12:30', 20, 1),

  -- Dr. Kiran Shah (Gynaecology, Sunrise) – Tue/Thu/Sat
  (5, 1, '11:00', '14:00', 30, 1),
  (5, 3, '11:00', '14:00', 30, 1),
  (5, 5, '10:00', '13:00', 30, 1),

  -- Dr. Venkat Rao (Dermatology, Green Valley) – Mon–Fri
  (6, 0, '10:00', '13:00', 15, 1),
  (6, 1, '10:00', '13:00', 15, 1),
  (6, 2, '10:00', '13:00', 15, 1),
  (6, 3, '10:00', '13:00', 15, 1),
  (6, 4, '10:00', '13:00', 15, 1);

-- --------------------------------------------------------
-- 7. Appointments
--    Mix of SCHEDULED / COMPLETED / CANCELLED across roles
--    Dates span past (completed), today, and upcoming
-- --------------------------------------------------------
INSERT INTO appointments
  (id, patient_id, doctor_id, hospital_id, appointment_date, start_time, end_time,
   booked_by_role, booked_by_id, is_offline, status, notes)
VALUES
  -- ---- City Care – Dr. Anil Mehta (Cardiology) ----
  (1,  1, 1, 1, '2025-06-02', '09:00', '09:30', 'PATIENT', 1,  0, 'COMPLETED', 'Routine cardiac check'),
  (2,  2, 1, 1, '2025-06-02', '09:30', '10:00', 'ADMIN',   1,  0, 'COMPLETED', 'BP follow-up'),
  (3,  5, 1, 1, '2025-06-04', '09:00', '09:30', 'PATIENT', 5,  0, 'COMPLETED', 'Chest pain consultation'),
  (4,  9, 1, 1, '2025-06-06', '09:30', '10:00', 'ADMIN',   1,  1, 'COMPLETED', 'Walk-in – palpitations'),
  (5,  1, 1, 1, '2025-06-16', '09:00', '09:30', 'PATIENT', 1,  0, 'COMPLETED', 'Post-medication review'),
  (6,  2, 1, 1, '2025-06-20', '09:00', '09:30', 'PATIENT', 2,  0, 'SCHEDULED', 'Monthly BP monitoring'),
  (7,  5, 1, 1, '2025-06-20', '09:30', '10:00', 'ADMIN',   1,  0, 'SCHEDULED', 'ECG review'),
  (8,  9, 1, 1, '2025-06-23', '09:00', '09:30', 'PATIENT', 9,  0, 'SCHEDULED', 'Stress test follow-up'),
  (9,  10,1, 1, '2025-06-25', '09:30', '10:00', 'ADMIN',   1,  0, 'SCHEDULED', 'Angioplasty pre-op consult'),
  (10, 1, 1, 1, '2025-06-10', '09:00', '09:30', 'PATIENT', 1,  0, 'CANCELLED', 'Patient rescheduled'),

  -- ---- City Care – Dr. Sneha Joshi (Orthopaedics) ----
  (11, 3, 2, 1, '2025-06-03', '10:00', '10:30', 'PATIENT', 3,  0, 'COMPLETED', 'Knee pain evaluation'),
  (12, 6, 2, 1, '2025-06-05', '10:00', '10:30', 'ADMIN',   1,  0, 'COMPLETED', 'Post-fracture X-ray review'),
  (13, 3, 2, 1, '2025-06-19', '10:00', '10:30', 'PATIENT', 3,  0, 'SCHEDULED', 'Physio progress check'),
  (14, 6, 2, 1, '2025-06-21', '10:30', '11:00', 'PATIENT', 6,  0, 'SCHEDULED', 'Shoulder pain'),
  (15, 10,2, 1, '2025-06-07', '10:00', '10:30', 'ADMIN',   1,  1, 'CANCELLED', 'Doctor unavailable'),

  -- ---- City Care – Dr. Ramesh Kulkarni (General Medicine) ----
  (16, 4, 3, 1, '2025-06-02', '14:00', '14:20', 'PATIENT', 4,  0, 'COMPLETED', 'Fever and cold'),
  (17, 8, 3, 1, '2025-06-03', '14:00', '14:20', 'ADMIN',   2,  0, 'COMPLETED', 'Diabetes management'),
  (18, 5, 3, 1, '2025-06-04', '14:20', '14:40', 'PATIENT', 5,  0, 'COMPLETED', 'Thyroid prescription renewal'),
  (19, 2, 3, 1, '2025-06-19', '14:00', '14:20', 'PATIENT', 2,  0, 'SCHEDULED', 'Vitamin D deficiency'),
  (20, 4, 3, 1, '2025-06-20', '14:00', '14:20', 'PATIENT', 4,  0, 'SCHEDULED', 'Follow-up after antibiotics'),
  (21, 8, 3, 1, '2025-06-21', '14:20', '14:40', 'ADMIN',   1,  0, 'SCHEDULED', 'HbA1c review'),

  -- ---- Sunrise – Dr. Priya Nair (Paediatrics) ----
  (22, 7, 4, 2, '2025-06-04', '09:30', '09:50', 'PATIENT', 7,  0, 'COMPLETED', 'Child vaccination – 6-month'),
  (23, 1, 4, 2, '2025-06-06', '09:30', '09:50', 'ADMIN',   3,  0, 'COMPLETED', 'Infant growth check'),
  (24, 7, 4, 2, '2025-06-20', '09:30', '09:50', 'PATIENT', 7,  0, 'SCHEDULED', 'Fever in toddler'),
  (25, 4, 4, 2, '2025-06-23', '09:50', '10:10', 'ADMIN',   3,  0, 'SCHEDULED', 'Annual health checkup'),

  -- ---- Sunrise – Dr. Kiran Shah (Gynaecology) ----
  (26, 1, 5, 2, '2025-06-05', '11:00', '11:30', 'PATIENT', 1,  0, 'COMPLETED', 'Routine gynaecology checkup'),
  (27, 3, 5, 2, '2025-06-10', '11:00', '11:30', 'ADMIN',   3,  0, 'CANCELLED', 'Patient cancelled'),
  (28, 1, 5, 2, '2025-06-21', '11:00', '11:30', 'PATIENT', 1,  0, 'SCHEDULED', 'Sonography follow-up'),

  -- ---- Green Valley – Dr. Venkat Rao (Dermatology) ----
  (29, 3, 6, 3, '2025-06-03', '10:00', '10:15', 'PATIENT', 3,  0, 'COMPLETED', 'Acne treatment review'),
  (30, 8, 6, 3, '2025-06-04', '10:00', '10:15', 'PATIENT', 8,  0, 'COMPLETED', 'Eczema flare-up'),
  (31, 10,6, 3, '2025-06-05', '10:15', '10:30', 'ADMIN',   4,  0, 'COMPLETED', 'Psoriasis check'),
  (32, 3, 6, 3, '2025-06-20', '10:00', '10:15', 'PATIENT', 3,  0, 'SCHEDULED', 'Medication side-effect review'),
  (33, 8, 6, 3, '2025-06-23', '10:00', '10:15', 'PATIENT', 8,  0, 'SCHEDULED', 'Follow-up eczema'),
  (34, 10,6, 3, '2025-06-25', '10:15', '10:30', 'ADMIN',   4,  0, 'SCHEDULED', 'Patch test results');

-- --------------------------------------------------------
-- 8. Documents (bills, prescriptions, reports)
-- --------------------------------------------------------
INSERT INTO documents
  (id, patient_id, hospital_id, appointment_id, document_type, storage_key, original_file_name,
   uploaded_by_role, uploaded_by_id, is_visible_to_patient)
VALUES
  (1,  1, 1, 1,    'PRESCRIPTION',   'city-care/patient-1/appt-1/prescription.pdf',    'prescription_anil_02jun.pdf',  'ADMIN',   1, 1),
  (2,  1, 1, 1,    'BILL',           'city-care/patient-1/appt-1/bill.pdf',             'bill_02jun.pdf',               'ADMIN',   1, 1),
  (3,  2, 1, 2,    'PRESCRIPTION',   'city-care/patient-2/appt-2/prescription.pdf',    'prescription_arjun_02jun.pdf', 'ADMIN',   1, 1),
  (4,  5, 1, 3,    'REPORT',         'city-care/patient-5/appt-3/ecg_report.pdf',       'ecg_sunita_04jun.pdf',         'ADMIN',   2, 1),
  (5,  5, 1, 3,    'BILL',           'city-care/patient-5/appt-3/bill.pdf',             'bill_04jun.pdf',               'ADMIN',   1, 1),
  (6,  3, 1, 11,   'PRESCRIPTION',   'city-care/patient-3/appt-11/prescription.pdf',   'prescription_kavita_03jun.pdf','ADMIN',   1, 1),
  (7,  6, 1, 12,   'REPORT',         'city-care/patient-6/appt-12/xray.pdf',            'xray_deepak_05jun.pdf',        'ADMIN',   2, 1),
  (8,  6, 1, 12,   'BILL',           'city-care/patient-6/appt-12/bill.pdf',            'bill_05jun.pdf',               'ADMIN',   1, 1),
  (9,  4, 1, 16,   'PRESCRIPTION',   'city-care/patient-4/appt-16/prescription.pdf',   'prescription_mohammed_02jun.pdf','ADMIN', 2, 1),
  (10, 8, 1, 17,   'PRESCRIPTION',   'city-care/patient-8/appt-17/prescription.pdf',   'prescription_rahul_03jun.pdf', 'ADMIN',   2, 1),
  (11, 8, 3, 30,   'PRESCRIPTION',   'green-valley/patient-8/appt-30/prescription.pdf','prescription_eczema_04jun.pdf','ADMIN',   4, 1),
  (12, 10,3, 31,   'REPORT',         'green-valley/patient-10/appt-31/biopsy.pdf',      'biopsy_sanjay_05jun.pdf',      'ADMIN',   4, 1),
  -- Patient-uploaded previous records
  (13, 1, 1, NULL, 'PREVIOUS_RECORD','city-care/patient-1/prev/old_ecg_2023.pdf',       'old_ecg_2023.pdf',             'PATIENT', 1, 1),
  (14, 3, 3, NULL, 'PREVIOUS_RECORD','green-valley/patient-3/prev/allergy_test_2024.pdf','allergy_test_2024.pdf',       'PATIENT', 3, 1),
  (15, 10,1, NULL, 'PREVIOUS_RECORD','city-care/patient-10/prev/angio_report_2022.pdf', 'angio_report_2022.pdf',        'PATIENT', 10,1);

-- --------------------------------------------------------
-- 9. Notifications
-- --------------------------------------------------------
INSERT INTO notifications (id, recipient_user_type, recipient_id, title, message, is_read) VALUES
  -- Appointment reminders to patients
  (1,  'PATIENT', 1,  'Appointment Reminder',          'You have an appointment with Dr. Anil Mehta tomorrow (20 Jun) at 09:00.', 0),
  (2,  'PATIENT', 2,  'Appointment Reminder',          'Your appointment with Dr. Anil Mehta is on 20 Jun at 09:00.', 0),
  (3,  'PATIENT', 5,  'Appointment Reminder',          'Reminder: ECG review with Dr. Anil Mehta on 20 Jun at 09:30.', 1),
  (4,  'PATIENT', 3,  'Appointment Scheduled',         'Your appointment with Dr. Sneha Joshi on 19 Jun at 10:00 is confirmed.', 1),
  (5,  'PATIENT', 7,  'Appointment Reminder',          'Your child''s appointment with Dr. Priya Nair is on 20 Jun at 09:30.', 0),
  (6,  'PATIENT', 1,  'Appointment Reminder',          'Gynaecology appointment with Dr. Kiran Shah on 21 Jun at 11:00.', 0),
  -- Lab / document ready
  (7,  'PATIENT', 5,  'Report Available',              'Your ECG report from 04 Jun is now available in Documents.', 1),
  (8,  'PATIENT', 6,  'Report Available',              'Your X-ray report from 05 Jun is ready. Please check Documents.', 1),
  (9,  'PATIENT', 10, 'Report Available',              'Biopsy report from Green Valley is now available for download.', 0),
  -- Appointment cancellations
  (10, 'PATIENT', 1,  'Appointment Cancelled',         'Your appointment with Dr. Anil Mehta on 10 Jun has been cancelled as requested.', 1),
  (11, 'PATIENT', 3,  'Appointment Cancelled',         'Your appointment with Dr. Kiran Shah on 10 Jun was cancelled. Please rebook.', 0),
  -- Doctor notifications
  (12, 'DOCTOR',  1,  'Schedule Update',               'Your schedule for tomorrow (20 Jun) has 2 confirmed appointments.', 0),
  (13, 'DOCTOR',  3,  'New Appointment Booked',        'New appointment booked: Mohammed Shaikh on 20 Jun at 14:00.', 1),
  -- Admin notifications
  (14, 'ADMIN',   1,  'Walk-in Appointment Logged',    'Walk-in appointment for Anita Pillai logged on 06 Jun.', 1),
  (15, 'ADMIN',   3,  'New Patient Registration',      'New patient Pooja Iyer registered and booked with Dr. Priya Nair.', 1);

-- --------------------------------------------------------
-- Reset IDENTITY_INSERT
-- --------------------------------------------------------
SET IDENTITY_INSERT notifications OFF;
SET IDENTITY_INSERT documents OFF;
SET IDENTITY_INSERT appointments OFF;
SET IDENTITY_INSERT patients OFF;
SET IDENTITY_INSERT admin_users OFF;
SET IDENTITY_INSERT doctors OFF;
SET IDENTITY_INSERT hospitals OFF;
