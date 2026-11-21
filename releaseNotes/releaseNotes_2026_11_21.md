
## 21 November 2026

### Backend family member management

- Registered patients can add and manage family members after profile creation. Each family member has a separate patient ID and uses the account owner's login for appointment management.
- Added `GET` and `POST /api/patients/family-members` for listing and adding members, and `PATCH` and `DELETE /api/patients/family-members/{memberId}` for updating and removing them.
- Patient bookings accept an active family member's `patientId`. Omitting it books for the signed-in patient. The appointment records the family member as the patient and the account owner as the person who booked.
- Appointment lists include active family members' appointments. The backend verifies the relationship for appointment details and cancellation. Removing a member revokes the owner's access and preserves the member's appointment history.
- The scope is family profiles and appointments. Linking an existing patient account and sharing family documents or PMR require separate permissions.

### Backend deployment and verification

- Run `healthcare-backend/src/main/resources/db/manual/patient_relationship.sql` before deploying. Apply it after the guest patient registration, audit timestamp, and patient account soft-delete scripts. It creates `patient_relationship` and allows family profiles to omit a mobile number while keeping supplied numbers unique.
- The Maven build passed with JDK 21. The migration and end-to-end booking flow still need verification against SQL Server.