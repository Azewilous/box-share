# BoxShare

A full-stack file storage and sharing application, themed after the Pokémon Colosseum PC Box storage screen. Users can upload, view, and share files through a retro game-inspired grid interface backed by a Spring Boot API and AWS S3.

> Learning project to experiment with high data accessibility across systems.

## Repositories

| Folder | Description |
|--------|-------------|
| [`boxshare/`](./boxshare) | Spring Boot backend — REST API, auth, file management, AWS S3/SQS |
| [`boxshare-app/`](./boxshare-app) | React frontend — the file grid UI |

## Features

- Register, log in, and verify your email
- Upload files directly to S3 via pre-signed URLs
- Browse your files in a Pokémon Colosseum-style slot grid
- Preview images, video, audio, PDFs, and text files inline
- Share files via a public link (`/view/:token`) or directly with another user by email
- Shared files appear with a distinct purple tint in the recipient's grid
- Toggle files between public and private; revoke share links at any time
- Delete files with a confirmation prompt
- Update your profile

## Architecture

```
boxshare-app (React)          boxshare (Spring Boot)
     │                               │
     │  REST + JWT cookie            │  Pre-signed URLs
     │──────────────────────────────>│──────────────────> AWS S3
     │                               │
     │                               │  SQS listener
     │                               │<────────────────── AWS SQS
     │                               │        (upload complete events)
     │                               │
     │                               │  SMTP
     │                               │──────────────────> Mailpit (local)
```

## Local Development

Both services need to be running together.

**1. Start the infrastructure** (from `boxshare/`):

```bash
docker compose up -d
```

This starts PostgreSQL, LocalStack (S3 + SQS), and Mailpit.

| Service | URL |
|---------|-----|
| Backend API | `http://localhost:8080` |
| Frontend | `http://localhost:5173` |
| Mailpit (email UI) | `http://localhost:8025` |
| LocalStack (AWS) | `http://localhost:4566` |

**2. Start the backend** (from `boxshare/`):

```bash
gradlew.bat bootRun --args='--spring.profiles.active=local'   # Windows
./gradlew bootRun --args='--spring.profiles.active=local'     # Mac/Linux
```

**3. Start the frontend** (from `boxshare-app/`):

```bash
npm install
npm run dev
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 19, TypeScript, Vite, CSS Modules |
| Backend | Java 21, Spring Boot 4, Spring Security |
| Database | PostgreSQL + Flyway |
| File storage | AWS S3 (LocalStack locally) |
| Async events | AWS SQS + Spring Cloud AWS `@SqsListener` |
| Email | JavaMailSender + Mailpit locally |
| Auth | JWT (cookie-based) |

## Testing

**Backend** — integration tests with H2 in-memory database, mocked AWS and email:
```bash
# from boxshare/
gradlew.bat test
```

**Frontend** — unit tests (Vitest) and E2E tests (Playwright, no backend required):
```bash
# from boxshare-app/
npm test              # unit tests
npm run test:coverage # unit tests with coverage report
npm run test:e2e      # E2E tests (starts Vite automatically)
npm run test:e2e:ui   # E2E with interactive Playwright UI
```

## Project Structure

```
boxshare/
├── boxshare/               # Spring Boot backend
│   ├── src/main/java/      # Application source
│   ├── src/test/java/      # Integration tests
│   ├── src/main/resources/ # Config, Flyway migrations
│   ├── localstack/init/    # LocalStack startup scripts (S3/SQS setup)
│   └── docker-compose.yaml # PostgreSQL, LocalStack, Mailpit
│
└── boxshare-app/           # React frontend
    ├── src/
    │   ├── api/            # Axios API functions
    │   ├── components/     # React components
    │   ├── context/        # Auth context
    │   └── utils/          # File utilities
    └── e2e/                # Playwright E2E tests
```

See each project's README for full setup and API details:
- [Backend README](./boxshare/README.md)
- [Frontend README](./boxshare-app/README.md)
