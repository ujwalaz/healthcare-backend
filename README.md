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

```
src/main/java/com/healthcare/
├── config/         # Security, CORS, Azure Blob, Swagger, LocalTimeConverter
├── controller/     # REST controllers (Auth, Appointment, Doctor, Hospital, Patient, PMR, Document, Notification)
├── dto/            # Generated from openapi/healthcare-api.json at build time
├── entity/         # JPA entities
├── repository/     # Spring Data JPA repositories
├── service/        # Business logic
├── security/       # JwtAuthFilter, JwtUtil, JwtClaims, SecurityUtils
└── exception/      # GlobalExceptionHandler, custom exceptions

src/main/resources/
├── application.properties
└── openapi/
    └── healthcare-api.json   # OpenAPI 3.0 spec — source of truth for all DTOs
```

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

| Method | Path | Body |
|---|---|---|
| POST | `/patient/register` | `{ name, age, gender, mobileNumber, ... }` |
| POST | `/patient/login` | `{ mobileNumber }` |
| POST | `/doctor/login` | `{ email, password }` |
| POST | `/admin/login` | `{ mobileNumber, password }` |
| POST | `/logout` | — (requires `Authorization: Bearer <token>` header) |

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
| GET | `/slots` | All | `doctorId`, `date` — available booking slots |
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

## DTO Generation

DTOs are **not** hand-written. They are generated at build time from the OpenAPI spec:

```
src/main/resources/openapi/healthcare-api.json
```

Run generation manually:

```bash
mvn generate-sources
```

Generated classes land in `target/generated-sources/openapi/src/main/java/com/healthcare/dto/`.  
**Do not edit generated files** — edit the JSON spec instead and regenerate.

---

## Database Migrations

Flyway is configured but currently **disabled** (`spring.flyway.enabled=false`).  
The schema is initialised by `db/migration/V1__init_schema.sql`.

To add schema changes, create `V2__description.sql`, `V3__description.sql`, etc.  
**Never modify `V1__init_schema.sql`.**
