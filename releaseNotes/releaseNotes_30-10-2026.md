# Healthcare Platform Release Notes

## Release window

- Yesterday: 30 October 2026
- Today: 31 October 2026

## 30 October 2026

### Guest patient appointment booking

- Admins and doctors can book appointments for patients who are not yet registered.
- Booking supports either an existing `patientId` or guest details:
  - `patientName`
  - `patientMobileNumber`
  - Optional gender and date of birth
- Guest patients are stored as unregistered patient records.
- A later patient registration using the same mobile number claims the guest record and preserves its appointment and medical history.
- OTP verification is intentionally not included in this flow.
- Doctors can book only with themselves as the treating doctor and only at their own hospital.
- Doctor patient search is scoped to the doctor’s hospital.

### Database and API updates

- Added support for unregistered patients without passwords.
- Added `is_registered` to patients.
- Updated the appointment booking contract and Postman collection.
- Allowed `DOCTOR` as an appointment booking role.
- Added manual database scripts:
  - `healthcare-backend/src/main/resources/db/manual/guest_patient_registration.sql`
  - `healthcare-backend/src/main/resources/db/manual/doctor_booking.sql`

## 31 October 2026

### Patient account soft deletion

- Added `DELETE /api/patients/profile` for patient self-service account deletion.
- Account deletion is soft delete only:
  - Sets `is_deleted` and `deleted_at`.
  - Invalidates existing JWTs by incrementing `password_version`.
  - Blocks login, password reset, profile access, and patient search for deleted accounts.
  - Preserves appointments, PMR records, documents, and audit history.
- Added `patient_account_soft_delete.sql`.

### Document deletion

- Patients can delete their own documents.
- Admins can delete documents belonging to their hospital.
- Azure Blob files are permanently deleted.
- Document metadata is soft-deleted in the database using `is_deleted` and `deleted_at`.
- Deleted documents are excluded from listing and download APIs.
- Added `document_soft_delete.sql`.
- Updated OpenAPI and Postman documentation.

## Deployment notes

Because Flyway is disabled, run these manual SQL scripts before deployment:

1. `guest_patient_registration.sql`
2. `doctor_booking.sql`
3. `patient_account_soft_delete.sql`
4. `document_soft_delete.sql`

Frontend lint passed. OpenAPI and Postman collection JSON validation passed. Backend Maven compilation still needs to be run in an environment with Maven available so the OpenAPI-generated DTOs and interfaces are regenerated.