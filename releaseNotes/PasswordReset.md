### Password Reset APIs

#### `GET /api/auth/password-reset/questions`

Public endpoint returning only active question IDs and question text. It never
returns answers or account-specific question details.

#### `POST /api/auth/password-reset/verify`

Request:

```json
{
  "role": "PATIENT",
  "identifier": "9876543210",
  "questionId": 1,
  "answer": "normalized answer"
}
```

Identifier mapping:

- `PATIENT`: `patients.mobile_number`
- `DOCTOR`: `doctors.phone`
- `ADMIN`: `admin_users.mobile_number`

The backend must verify that the selected question ID matches the account's
configured question and compare the normalized answer with its BCrypt hash.
Unknown accounts, wrong questions, and wrong answers must use generic errors to
avoid account enumeration. Failed attempts are rate-limited and capped.

Successful response:

```json
{
  "resetToken": "one-time-token",
  "expiresAt": "2026-10-14T12:05:00Z"
}
```

#### `POST /api/auth/password-reset/complete`

Request:

```json
{
  "resetToken": "one-time-token",
  "newPassword": "new-password"
}
```

The token expires after 10 minutes and is single-use. The new password must
meet the existing minimum length of eight characters and must be stored only as
a BCrypt hash.

### Database Changes

Flyway remains disabled. Apply these changes through a manually executed SQL
script; do not modify `V1__init_schema.sql`.

Create a fixed question catalog:

```sql
security_questions (
    id,
    question_text,
    is_active,
    display_order,
    created_at
)
```

Add the following fields to `patients`, `doctors`, and `admin_users`:

```sql
security_question_id BIGINT NULL,
security_answer_hash NVARCHAR(255) NULL,
password_version INT NOT NULL DEFAULT 0
```

Add a `password_reset_tokens` table containing the hashed reset token, role,
account ID, failed-attempt count, expiry time, consumed time, and creation time.
Only token hashes are stored. Existing accounts will be populated manually with
the selected question ID and a BCrypt answer hash.

### Frontend Requirements

- Add `Forgot password?` to all three login tabs.
- Add a public `/forgot-password` route.
- Load and display all security questions in a selector.
- Collect role, mobile/phone identifier, selected question, and answer.
- After successful verification, show new-password and confirmation fields.
- Send only the reset token and new password to the completion endpoint.
- Clear reset state and redirect to login after success.
- Add security-question selection and answer fields to patient registration.
- Doctor and admin account question data will be configured manually.

### Security and Validation

- Normalize answers by trimming whitespace and applying case-insensitive comparison.
- Never store or log plain-text answers, passwords, or reset tokens.
- Limit verification attempts to five failures per account in a 15-minute window,
  then temporarily lock verification for that account; expire reset tokens after
  10 minutes.
- Consume reset tokens atomically to prevent reuse.
- Increment `password_version` after reset so existing JWT sessions are invalidated.
- Validate the selected role before querying the corresponding account table.

### Acceptance Tests

- All three roles can load the question catalog.
- Correct role, identifier, question, and answer produce a reset token.
- A question belonging to another account is rejected.
- Wrong answers are limited and eventually invalidate the reset attempt.
- Expired or reused reset tokens are rejected.
- Passwords are stored as BCrypt hashes.
- Existing JWT sessions are rejected after password reset.
- Frontend handles validation, error states, successful reset, and redirect to login.
