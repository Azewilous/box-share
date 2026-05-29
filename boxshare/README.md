# BoxShare — Backend

A file storage and sharing API built with Spring Boot and AWS. Users upload files directly to S3 via pre-signed URLs, with upload status tracked asynchronously through SQS. Files can be shared publicly via a token link or privately with specific users by email.

> Learning project to experiment with high data accessibility across systems.

## Tech Stack

- **Java 21 / Spring Boot 4.0.6**
- **PostgreSQL** — user and file metadata
- **AWS S3** — file storage via pre-signed URLs (`software.amazon.awssdk` — S3Client, S3Presigner)
- **AWS SQS** — S3 event notifications for upload status updates
  - `software.amazon.awssdk` SqsClient for raw operations
  - `spring-cloud-aws-starter-sqs` for the `@SqsListener` annotation — Spring manages the polling loop so the consumer is just a method
- **Flyway** — database schema migrations
- **Mailpit** — local email catcher for development (email verification flow)

## Features

- **Auth** — JWT-based authentication with email verification; cookie-based sessions
- **File upload** — pre-signed S3 PUT URLs so the client uploads directly to S3; SQS notifies the backend when the upload completes
- **File visibility** — files are `PRIVATE` by default; owners can make them `PUBLIC`, which generates a share token
- **Public sharing** — `/api/public/file/:token` serves any PUBLIC file without authentication
- **User sharing** — owners can share a private file with a specific BoxShare user by email; shared files appear in that user's file list
- **Role-based access** — `ROLE_USER` and `ROLE_ADMIN`; admins can delete any file, users can only delete their own

## Prerequisites

- Java 21
- Docker Desktop (with WSL2 backend on Windows)

## Local Development (Docker + LocalStack)

Docker Compose starts three services:

| Service | Purpose | Ports |
|---------|---------|-------|
| `postgres` | Database | `5432` |
| `localstack` | Local AWS (S3 + SQS) | `4566` |
| `mailpit` | Email catcher (catches verification emails) | `1025` (SMTP), `8025` (web UI) |

```bash
# 1. Start all services
docker compose up -d

# 2. Run the app with the local profile
gradlew.bat bootRun --args='--spring.profiles.active=local'   # Windows
./gradlew bootRun --args='--spring.profiles.active=local'     # Mac/Linux
```

The `local` profile (`application-local.properties`) points S3/SQS at `localhost:4566` with dummy credentials and mail at `localhost:1025` — no real AWS account or email provider needed.

**LocalStack init scripts** — Docker Compose mounts `./localstack/init/` into LocalStack's init directory (`/etc/localstack/init/ready.d`). Any shell scripts placed there run automatically when LocalStack starts. Use this to pre-create the S3 bucket and SQS queue:

```bash
# localstack/init/setup.sh
awslocal s3 mb s3://box-share-file-store
awslocal sqs create-queue --queue-name box-share-app-queue
# Wire S3 → SQS notifications here if needed
```

**Mailpit** — open `http://localhost:8025` in a browser to see all emails the app sends (registration verification, etc.). No emails leave your machine.

```bash
# Stop
docker compose down

# Stop and wipe the database volume (fresh start)
docker compose down -v
```

## Production / Real AWS

Create `src/main/resources/secrets.properties` (gitignored):

```properties
spring.datasource.username=your_db_user
spring.datasource.password=your_db_password

# AWS SDK — used directly for S3Presigner and SqsClient beans
aws.access.key=YOUR_ACCESS_KEY
aws.secret.key=YOUR_SECRET_KEY
aws.region=us-east-1
aws.s3.bucket=your-s3-bucket-name
aws.queue.name=your-sqs-queue-name

# Spring Cloud AWS — drives the @SqsListener consumer
spring.cloud.aws.credentials.access-key=YOUR_ACCESS_KEY
spring.cloud.aws.credentials.secret-key=YOUR_SECRET_KEY
spring.cloud.aws.region.static=us-east-1

# Mail
spring.mail.host=your-smtp-host
spring.mail.port=587
spring.mail.username=your-smtp-user
spring.mail.password=your-smtp-password
app.mail.from=noreply@yourdomain.com
app.frontend-url=https://yourdomain.com
```

Then run without the `local` profile. Server starts on `http://localhost:8080`.

## API

### Auth

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/register` | Public | Register a new user |
| POST | `/api/auth/login` | Public | Log in, returns JWT cookie |
| POST | `/api/auth/logout` | Public | Clear session |
| GET | `/api/auth/me` | Cookie | Get current user info |

### Users

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/user` | User | Create / look up user by email |
| PUT | `/api/user` | User | Update user profile |
| DELETE | `/api/user/{id}` | User | Delete account |
| POST | `/api/user/verify/:token` | Public | Verify email address |
| POST | `/api/user/verify/resend` | Public | Resend verification email |

### Files

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/file` | User | List files owned by or shared with the current user |
| POST | `/api/file` | User | Register a file and get a pre-signed S3 PUT URL |
| GET | `/api/file/{filename}` | User | Get a pre-signed S3 GET URL |
| DELETE | `/api/file/{id}` | Owner / Admin | Delete a file |
| PATCH | `/api/file/{id}/visibility` | User | Toggle PUBLIC / PRIVATE; generates or clears share token |
| POST | `/api/file/{id}/share?email=` | Owner / Admin | Share a file with a specific user by email |
| GET | `/api/public/file/{token}` | Public | View a PUBLIC file by share token |

### Health

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/system/health` | Health check |

## File Upload Flow

1. `POST /api/file` — creates a file record and returns a pre-signed S3 PUT URL
2. Client uploads the file directly to S3 using that URL
3. S3 notifies SQS — the backend consumes the message and marks the file `COMPLETED`
4. `GET /api/file/{filename}` — returns a pre-signed GET URL to view or download

## File Sharing

**Link share** — `PATCH /api/file/{id}/visibility` with `{ "visibility": "PUBLIC" }` assigns a random UUID share token. Anyone can then access the file at `/api/public/file/{token}` without authentication. Setting visibility back to `PRIVATE` clears the token.

**User share** — `POST /api/file/{id}/share?email=user@example.com` creates a `shared_files` record. The target user sees the file in their `GET /api/file` list with `shared: true`. Duplicate shares are silently ignored.

## Database Migrations (Flyway)

Migrations live in `src/main/resources/db/migration/` and run automatically on startup.

```
V1__init.sql
V2__add_file_table.sql
V3__add_user_roles.sql
V4__add_file_visibility.sql
V5__create_shared_file.sql
```

Rules: version numbers must increase, two underscores between version and description, never edit an applied migration.

## Running Tests

```bash
gradlew.bat test        # Windows
./gradlew test          # Mac/Linux
```

Integration tests use an in-memory H2 database and mock AWS/email dependencies — no Docker needed to run them.
