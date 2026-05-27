# BoxShare

A file-sharing backend built with Spring Boot and AWS. Users can upload files directly to S3 via pre-signed URLs, with upload status tracked asynchronously through SQS.

> This is a learning project to experiment with high data accessibility across systems.

## Tech Stack

- **Java 21 / Spring Boot 4.0.6**
- **PostgreSQL** — user and file metadata
- **AWS S3** — file storage via pre-signed URLs
- **AWS SQS** — S3 event notifications for upload status updates
- **Flyway** — database schema migrations

## Prerequisites

- Java 21
- Docker Desktop (with WSL2 backend on Windows)

## Local Development Setup (Docker + LocalStack)

This is the recommended way to run the app locally. PostgreSQL and AWS services (S3, SQS) are provided by Docker — no real AWS account needed.

**1. Start the infrastructure:**

```bash
docker compose up -d
```

This starts PostgreSQL and LocalStack. The LocalStack init script automatically creates the S3 bucket (`box-share-file-store`), SQS queue (`box-share-app-queue`), and wires up S3 → SQS event notifications.

**2. Run the app with the `local` profile:**

```bash
# Windows
gradlew.bat bootRun --args='--spring.profiles.active=local'

# Mac/Linux
./gradlew bootRun --args='--spring.profiles.active=local'
```

The `local` profile (`application-local.properties`) points S3/SQS at `localhost:4566` with dummy credentials — no `secrets.properties` needed for local dev.

**3. Stop the infrastructure:**

```bash
docker compose down

# To also wipe the database volume (fresh start):
docker compose down -v
```

## Production / Real AWS Setup

Create `src/main/resources/secrets.properties` (gitignored) with your real credentials:

```properties
# Database
spring.datasource.username=your_db_user
spring.datasource.password=your_db_password

# AWS
aws.access.key=YOUR_AWS_ACCESS_KEY
aws.secret.key=YOUR_AWS_SECRET_KEY
aws.region=us-east-1
aws.s3.bucket=your-s3-bucket-name
aws.queue.name=your-sqs-queue-name

# Spring Cloud AWS
spring.cloud.aws.credentials.access-key=YOUR_AWS_ACCESS_KEY
spring.cloud.aws.credentials.secret-key=YOUR_AWS_SECRET_KEY
spring.cloud.aws.region.static=us-east-1
```

Then run without the `local` profile:

```bash
gradlew.bat bootRun
```

The server starts on `http://localhost:8080`.


## API

### Users

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/user` | Create a user |
| GET | `/api/user/{id}` | Get a user |
| PUT | `/api/user` | Update a user |
| DELETE | `/api/user/{id}` | Delete a user |

### Files

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/file` | Get a pre-signed S3 PUT URL to upload a file |
| GET | `/api/file/{filename}` | Get a pre-signed S3 GET URL to download a file |

### Health

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/system/health` | Health check |

## File Upload Flow

1. `POST /api/file` — creates a file record (status: `PENDING`) and returns a pre-signed S3 PUT URL
2. Client uploads the file directly to S3 using that URL
3. S3 notifies SQS — the app consumes the message and marks the file as `COMPLETED`
4. `GET /api/file/{filename}` — returns a pre-signed GET URL for downloading

## Database Migrations (Flyway)

Flyway runs automatically on startup and applies any pending migrations in order.

Migration files live in `src/main/resources/db/migration/` and must follow the naming convention:

```
V{version}__{description}.sql
```

Examples:
```
V1__init.sql
V2__add_file_table.sql
V3__add_user_roles.sql
```

Rules:
- Version numbers must be unique and increase — Flyway runs them in order
- Two underscores between the version and description
- Never edit a migration that has already been applied — add a new one instead
- Flyway tracks applied migrations in the `flyway_schema_history` table

**Generating the initial schema from your JPA entities:**

Add these to `application-local.properties` temporarily, run the app once to generate the file, then remove them:

```properties
spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create
spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=src/main/resources/db/migration/V1__init.sql
spring.jpa.properties.jakarta.persistence.schema-generation.database.action=none
spring.jpa.hibernate.ddl-auto=none
```

## Running Tests

```bash
gradlew.bat test
```
