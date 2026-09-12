# Healthcare OPD Platform — Backend

Spring Boot REST API for the Healthcare OPD Platform. Supports three roles — **Patient**, **Doctor**, and **Admin** — with JWT authentication, Azure SQL Database, and Azure Blob Storage.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.2.5 (Java 17) |
| Security | Spring Security + JWT (jjwt 0.12.5) |
| Persistence | Spring Data JPA / Hibernate |
| Database | Azure SQL Database (SQL Server) |
| Migrations | Flyway |
| Storage | Azure Blob Storage |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Build | Maven |

---

## Project Structure

The controller layer is now **generated + hand-written in two parts**, driven entirely by `openapi/healthcare-api.json`:

```
src/main/java/com/healthcare/
├── annotation/      # @ApiMessage — declares the MessageCode used to envelope a handler's response
├── api/             # *ApiImpl.java — hand-written, implements the generated *Api interfaces, delegates to service/
├── config/          # Security, CORS, Azure Blob, Swagger, LocalTimeConverter, ApiResponseEnvelopeAdvice
├── dto/             # Generated from openapi/healthcare-api.json at build time
├── entity/          # JPA entities
├── repository/      # Spring Data JPA repositories
├── service/         # Business logic (unchanged — still called by *ApiImpl)
├── security/        # JwtAuthFilter, JwtUtil, JwtClaims, SecurityUtils
└── exception/       # GlobalExceptionHandler, custom exceptions

target/generated-sources/openapi/src/main/java/com/healthcare/
├── api/             # *Api.java — GENERATED interfaces (AuthApi, AppointmentsApi, HospitalsApi, PatientsApi,
│                     # PmrApi, DoctorsApi, DocumentsApi, NotificationsApi). Do not edit — regenerated on every build.
└── dto/              # Generated request/response DTOs (incl. per-type *PagedResponse wrappers)

src/main/resources/
├── application.properties
└── openapi/
    └── healthcare-api.json   # OpenAPI 3.0 spec — single source of truth for both DTOs and API interfaces
```

**Request flow:** `*Api` (generated interface, defines route/params/validation via annotations) → `*ApiImpl` (hand-written `@RestController`, implements the interface) → `*Service` (business logic, unchanged) → `*Repository` (Spring Data JPA).

There is no `controller/` package anymore — the 8 old hand-written controllers were removed in favor of the `*ApiImpl` classes, which own the routes directly.

### Preserving the `ApiResponse<T>` envelope

Because OpenAPI schemas can't express a generic wrapper without an explosion of per-type schemas, the spec's response schemas reference bare DTOs, and the generated interfaces return bare `ResponseEntity<Dto>`. To keep the existing wire contract (`{ success, code, message, data }`) that the frontend depends on:

- Every non-void `*ApiImpl` method is annotated with `@ApiMessage(MessageCode.XXX)`.
- `ApiResponseEnvelopeAdvice` (a `ResponseBodyAdvice`) detects that annotation and wraps the raw body into `ApiResponse.ok(code, body)` at serialization time — transparently, with no spec/schema changes needed.
- **Known limitation:** Spring does not invoke `ResponseBodyAdvice` for `ResponseEntity<Void>` with a `null` body, so the 3 void endpoints (`logout`, `deleteDocument`, `markAllNotificationsRead`) return an empty 200 response instead of an enveloped `{ data: null }` body.

---

## Prerequisites

- Java 17+
- Maven 3.8+
- Access to an Azure SQL Database instance
- (Optional) Azure Blob Storage account for document upload/download

---

## Configuration

All configuration lives in `src/main/resources/application.properties`.

| Property | Description |
|---|---|
| `spring.datasource.url` | JDBC URL for Azure SQL — includes `sendTimeAsDatetime=false` (required for TIME column compatibility) |
| `spring.datasource.username` | DB username |
| `spring.datasource.password` | DB password |
| `app.jwt.secret` | HS256 secret — must be ≥ 32 characters |
| `app.jwt.expiration-ms` | Token TTL in milliseconds (default 24 h) |
| `app.azure.storage.connection-string` | Azure Blob Storage connection string |
| `app.azure.storage.container-name` | Blob container name |
| `app.azure.storage.sas-expiry-minutes` | SAS URL validity window (default 60 min) |

> **Important:** `sendTimeAsDatetime=false` must remain in the JDBC URL. Without it, the MSSQL JDBC driver sends `java.sql.Time` as `DATETIME`, causing type mismatch errors on SQL Server `TIME` columns.

### Internal-token protected endpoints

`GET /api/appointments/slots/today-summary` is exempt from JWT authentication and instead requires a static shared-secret header:

```
X-Internal-Token: InternalWebApp
```

The expected value is configurable via `app.internal-token` (defaults to `InternalWebApp`). `InternalTokenFilter` intercepts this specific path before Spring Security's normal auth chain and rejects requests with a missing/incorrect header (`401`, `AUTH_INTERNAL_TOKEN_INVALID`). This endpoint is intended for trusted internal callers (e.g. an internal dashboard), not end-user (patient/doctor/admin) JWTs.

---

## Running Locally

```bash
cd healthcare-backend
mvn spring-boot:run
```

Server starts on **http://localhost:8080**.

---

## API Documentation (Swagger UI)

Once running, open:

```
http://localhost:8080/swagger-ui.html
```

Click **Authorize** and paste a JWT token (from any login endpoint) to test secured endpoints.

Raw OpenAPI JSON: `http://localhost:8080/v3/api-docs`

---

## API Endpoints

All endpoints are prefixed with `/api`. Responses are wrapped in `ApiResponse<T>` (`{ code, message, data }`). Paginated list endpoints return `PagedResponse<T>` and accept `page` (default `0`) / `size` (default `20`) query params.

### Auth (`/api/auth`) — Public

| Method | Path | Query Params | Body |
|---|---|---|---|
| POST | `/patient/register` | — | `{ name, age, gender, mobileNumber, ... }` |
| POST | `/login` | `role` (required: `PATIENT`|`DOCTOR`|`ADMIN`) | `{ mobileNumber, password }` |
| POST | `/logout` | — | — (requires `Authorization` header) |

The 3 previous role-specific login endpoints (`/patient/login`, `/doctor/login`, `/admin/login`) have been **merged into a single `POST /api/auth/login?role=...`** endpoint, dispatching internally to the right role logic based on the `role` query param (invalid/missing role -> 400 `AUTH_INVALID_ROLE`).

Login/register responses return `AuthResponse` `{ token, id, name, role, hospitalId }`.

### Hospitals (`/api/hospitals`) — All roles

| Method | Path | Query Params |
|---|---|---|
| GET | `/` | `page`, `size` |
| GET | `/{id}` | — |

### Doctors (`/api/doctors`)

| Method | Path | Role | Notes |
|---|---|---|---|
| GET | `/` | All | `hospitalId` (optional), `page`, `size` |
| GET | `/{id}` | All | — |
| GET | `/{id}/schedule` | All | Weekly recurring schedule |
| POST | `/{id}/schedule` | DOCTOR/ADMIN | Body: `List<ScheduleRequest>` |
| GET | `/{id}/calendar` | All | `from`, `to` (ISO date) |

### Appointments (`/api/appointments`)

| Method | Path | Role | Notes |
|---|---|---|---|
| GET | `/slots` | All | `doctorId`, `date` — available booking slots for one doctor |
| GET | `/slots/today-summary` | Internal (`X-Internal-Token` header, not JWT) | Total available slots **today**, aggregated across all hospitals and doctors — `date`, `totalAvailableSlots`, `totalHospitals`, `totalDoctors` (no per-hospital breakdown) |
| POST | `/` | PATIENT/ADMIN | Body: `AppointmentRequest` |
| GET | `/` | All | `status`, `date` (optional), `page`, `size` — scoped to caller |
| GET | `/{id}` | All (own/hospital-scoped) | — |
| PATCH | `/{id}/status` | ADMIN | Body: `StatusUpdateRequest` |

### Patients (`/api/patients`)

| Method | Path | Role | Notes |
|---|---|---|---|
| GET | `/profile` | PATIENT | Own profile |
| PUT | `/profile` | PATIENT | Body: `PatientUpdateRequest` |
| GET | `/{id}` | ADMIN/DOCTOR | — |
| GET | `/search` | ADMIN | `query`, `page`, `size` |

### PMR (`/api/pmr`) — access-window enforced for doctors

| Method | Path | Role | Notes |
|---|---|---|---|
| GET | `/patient/{patientId}` | PATIENT (own)/DOCTOR (appointment window) | `hospitalId` optional |
| GET | `/patient/{patientId}/entries` | PATIENT (own)/DOCTOR (appointment window) | `hospitalId`, `page`, `size` |
| POST | `/patient/{patientId}/entry` | DOCTOR (appointment window) | Body: `PmrEntryRequest` |

### Documents (`/api/documents`)

| Method | Path | Role | Notes |
|---|---|---|---|
| POST | `/upload` | PATIENT/ADMIN | Multipart: `file`, `patientId`, `hospitalId`, `appointmentId` (optional), `documentType` |
| GET | `/patient/{patientId}` | PATIENT (own)/ADMIN | `documentType`, `appointmentId`, `page`, `size` |
| GET | `/{id}/download` | PATIENT (own, visible)/ADMIN | Returns a time-limited Azure Blob SAS URL |
| DELETE | `/{id}` | PATIENT (own)/ADMIN | — |

### Notifications (`/api/notifications`)

| Method | Path | Role | Notes |
|---|---|---|---|
| GET | `/my` | All | `isRead` (optional), `page`, `size` |
| PATCH | `/{id}/read` | All | Mark single notification read |
| POST | `/` | ADMIN | Body: `NotificationRequest` |
| GET | `/unread-count` | All | — |
| PATCH | `/read-all` | All | — |

---

## Role Access Summary

| Action | PATIENT | DOCTOR | ADMIN |
|---|---|---|---|
| View hospitals / doctors | ✅ | ✅ | ✅ |
| Book appointments | ✅ | ❌ | ✅ |
| Cancel appointments | ✅ (own) | ❌ | ✅ |
| PMR read | ✅ (own) | ✅ (appointment window only) | ❌ |
| PMR write | ❌ | ✅ (appointment window only) | ❌ |
| Upload documents | ✅ (PREVIOUS_RECORD type only) | ❌ | ✅ |
| Download documents | ✅ (own, visible) | ❌ | ✅ |

---

## DTO & API Generation

DTOs **and** the `*Api` controller interfaces are **not** hand-written. Both are generated at build time from the OpenAPI spec via `openapi-generator-maven-plugin`:

```
src/main/resources/openapi/healthcare-api.json
```

Run generation manually:

```bash
mvn generate-sources
```

Generated classes land in `target/generated-sources/openapi/src/main/java/com/healthcare/{dto,api}/`.
**Do not edit generated files** — edit the JSON spec instead and regenerate. Every operation in the spec has an explicit `operationId`, which becomes the generated Java method name; interfaces are grouped by the OpenAPI `tags` field (`useTags=true`) into one `*Api` per domain (`AuthApi`, `AppointmentsApi`, `HospitalsApi`, `PatientsApi`, `PmrApi`, `DoctorsApi`, `DocumentsApi`, `NotificationsApi`). `interfaceOnly=true` + `skipDefaultInterface=true` mean generated interfaces have no default (501) method bodies, so every operation **must** be implemented by a matching `*ApiImpl` class in `com.healthcare.api`.

> **Note:** don't re-declare `@Valid`/`@NotNull`/etc. on `*ApiImpl` `@Override` method parameters — the generated interface already carries those constraint annotations, and redeclaring them on the overriding method throws `jakarta.validation.ConstraintDeclarationException` (HV000151) at runtime.

---

## Database Migrations

Flyway is configured but currently **disabled** (`spring.flyway.enabled=false`).  
The schema is initialised by `db/migration/V1__init_schema.sql`.

To add schema changes, create `V2__description.sql`, `V3__description.sql`, etc.  
**Never modify `V1__init_schema.sql`.**