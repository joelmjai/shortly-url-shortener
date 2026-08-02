# Shortly — URL Shortener API with Analytics (2026)

A REST API like bit.ly, built with **Java + Spring Boot + PostgreSQL**. Register, shorten a long URL into a short code, and every visit to the short link redirects to the original while recording the click. Per-user analytics expose total clicks, clicks per day, and top referrers.

> **Resume line:** Designed and deployed a REST API for URL shortening with click analytics using Java, Spring Boot, and PostgreSQL; implemented indexed schema design, JWT authentication, parameterised queries, and per-day analytics aggregation.

## Features

- **Shorten URLs** — collision-free 8-character codes, with validation (rejects non-`http(s)` input)
- **Redirect + click logging** — `302` redirect to the original URL, recording timestamp and referrer
- **Analytics** — clicks per day, total clicks across a date range, and top referrers
- **JWT authentication** — register / login; each user manages only their own URLs
- **Clean error handling** — a global exception handler returns consistent `400`s for bad input
- **Tested** — integration tests over an in-memory H2 database (no external DB needed to run them)

## Tech stack

| Layer | Choice |
|-------|--------|
| Language / framework | Java 17, Spring Boot 3.5 |
| Persistence | Spring Data JPA (Hibernate) + PostgreSQL |
| Auth | Spring Security + JWT (jjwt) |
| Build | Maven |
| Deploy | Docker (multi-stage), Render, Neon (Postgres) |
| Tests | JUnit 5 + Spring MockMvc + H2 |

## API endpoints

Auth endpoints are public; all `/api/url/**` endpoints require a `Bearer <jwt>` token.

| Method | Endpoint | Description | Success |
|--------|----------|-------------|---------|
| `POST` | `/api/auth/public/register` | Register a user | `200` |
| `POST` | `/api/auth/public/login` | Log in, returns `{ token }` | `200` |
| `POST` | `/api/url/shorten` | `{ "originalUrl": "..." }` → short code | `201` |
| `GET`  | `/{shortCode}` | Redirect to original URL + log click | `302` |
| `GET`  | `/api/url/myurls` | List the current user's URLs | `200` |
| `GET`  | `/api/url/analytics/{shortCode}?startDate=&endDate=` | Clicks per day for one URL | `200` |
| `GET`  | `/api/url/totalClicks?startDate=&endDate=` | Total clicks per day across the user's URLs | `200` |
| `GET`  | `/api/url/referrers/{shortCode}` | Click counts grouped by referrer | `200` |

**Status codes:** invalid URL → `400`, unknown short code → `404`, missing/malformed query params → `400`.

### Example

```bash
# 1. Register + login
curl -X POST localhost:8081/api/auth/public/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo","email":"demo@test.com","password":"Pass1234"}'

TOKEN=$(curl -s -X POST localhost:8081/api/auth/public/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo","password":"Pass1234"}' | jq -r .token)

# 2. Shorten (201)
curl -X POST localhost:8081/api/url/shorten \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"originalUrl":"https://www.wikipedia.org/"}'
# -> {"shortUrl":"qqxoGQZu", "originalUrl":"https://www.wikipedia.org/", ...}

# 3. Visit the short link (302 redirect)
curl -i localhost:8081/qqxoGQZu
```

## Database schema

**`url_mapping`** — one row per shortened URL
- `id` (PK), `orginal_url`, `short_url` (**unique**), `click_count`, `created_date`, `user_id` (FK → `users`)

**`click_event`** — one row per click, for analytics
- `id` (PK), `click_date`, `referrer`, `url_mapping_id` (FK → `url_mapping`)

`short_url` carries a unique constraint so lookups by code are fast and codes can't collide. `click_event` rows link back to their URL via a foreign key, enabling per-day and per-referrer aggregation.

## Running locally

**Prerequisites:** Java 17+ and a PostgreSQL database (e.g. a free [Neon](https://neon.tech) project).

1. Copy the environment template and fill in your values:
   ```bash
   cp .env.example .env
   ```
   ```properties
   DATABASE_URL=jdbc:postgresql://<host>/<db>?sslmode=require
   DATABASE_USERNAME=<username>
   DATABASE_PASSWORD=<password>
   DATABASE_DIALECT=org.hibernate.dialect.PostgreSQLDialect
   JWT_SECRET=<a-long-random-secret>
   ```
   > `.env` is gitignored — never commit real credentials.

2. Export the variables (Spring reads them as `${...}` placeholders) and run:
   ```bash
   ./mvnw spring-boot:run
   ```
   The API starts on `http://localhost:8081`.

## Running tests

```bash
./mvnw test
```

Integration tests run over an in-memory **H2** database, so no external DB or config is required. They exercise the full HTTP stack — security, JWT, validation, and persistence — covering the `201`/`302`/`400`/`404` paths and referrer aggregation.

## Deployment

The included multi-stage `Dockerfile` builds the app and runs the jar. On [Render](https://render.com): create a **Web Service** from this repo (Docker runtime auto-detected), then set the environment variables above in the **Environment** tab. The app binds to `${PORT}`, which Render provides automatically.
