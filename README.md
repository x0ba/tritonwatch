# Tritonwatch

WIP: App that alerts students via email or text when their UCSD courses open up.

## Architecture

- [tldraw diagram](https://www.tldraw.com/f/uMOnB2d7simw__NtL01GR?d=v-184.603.1179.812.page) (it's so out of date)
- `user-service` — user profiles and notification preferences (port `8081`)
- `watchlist-service` — watch requests and watch-created events (port `8082`)
- `ingestion-service` — tracking, UCSD polling, and weekly course catalog (port `8083`)
- `notification-service` — subscriptions and notifications (port `8084`)
- `shared/event-contracts` — the versioned event payloads and topic constants

The services are independent Gradle builds. Each includes `event-contracts` as a Gradle composite build, so no local
Maven publish step is needed.

## How does it work?

The entire thing is powered by the [UCSD Class Planner's](https://classplanner.apps.ucsd.edu/workspace?term=FA26) public API. 

When a user watches a course, the watchlist microservice saves that watch and fires off an event to the ingestion service. The ingestion service keeps a list of all the courses that everyone using Tritonwatch has watched, and sends that full list on every request to `POST https://classplanner.apps.ucsd.edu/api/v1/catalog/courses/search`. Class Planner paginates search results with a page size of 48, so if more than 48 watched courses match, ingestion walks `offset` until it has collected every page. This runs every 2 minutes, and if a course ever becomes available, an event is sent to the notification microservice which then sends the notification to the user with Postmark and/or Twilio.

The search function for courses just calls the same API as the search functionality in the class planner app.

## Local infrastructure

Start PostgreSQL, Kafka, and Redis:

```bash
docker compose -f infra/docker-compose.yml up -d
```

Kafka runs in KRaft mode at `localhost:9092`; auto topic creation is disabled. The one-shot `kafka-init` container
creates:

- `tritonwatch.user-course-watch-created.v1`
- `tritonwatch.course-tracking-requested.v1`
- `tritonwatch.course-section-became-available.v1`
- `tritonwatch.user-notification-settings-updated.v1` (compacted)

To reset all local database state (the database initialization script only runs on a new volume):

```bash
docker compose -f infra/docker-compose.yml down -v
```

## AWS deployment

The cost-optimized production deployment uses Amazon ECS with one EC2 container instance for the APIs, plus S3 and
CloudFront for same-origin SPA + API at `tritonwatch.app`. See the complete
[Amazon ECS deployment guide](docs/deployment-ecs.md) and the
[GitHub Actions CI/CD guide](docs/ci-cd.md).

## Running a service

Create the ignored backend development environment file once:

```bash
cp .env.example .env
```

Replace `YOUR_INSTANCE` in `.env` with the Clerk Development instance hostname. Each Spring Boot service imports the
root `.env` directly when run from its service directory; no shell export or Mise wrapper is required.

```bash
cd services/watchlist-service
./gradlew bootRun
```

Most runtime settings have local defaults and can be overridden with environment variables, including `DATABASE_URL`,
`DATABASE_USERNAME`, `DATABASE_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`, and `SERVER_PORT`. Ingestion also accepts
`INGESTION_POLL_INTERVAL`, `UCSD_API_BASE_URL`, `CATALOG_SYNC_CRON` (default Monday 05:00 UTC), and
`CATALOG_SYNC_ON_STARTUP` (syncs the full term catalog when empty).

## Course catalog API

`ingestion-service` keeps an internal UCSD course catalog (refreshed weekly from Class Planner) and exposes:

- `GET /api/v1/catalog/terms` — configured terms
- `GET /api/v1/catalog/courses?term=FA26&q=CSE` — search by course code or title
- `POST /api/v1/catalog/sync` — force a catalog refresh (local/ops)

The frontend add-watch picker uses these endpoints via `VITE_CATALOG_API_BASE_URL` (default `http://localhost:8083`).

`watchlist-service` and `user-service` are Clerk-protected resource servers. They require `CLERK_ISSUER` and
`CLERK_AUTHORIZED_PARTIES`; the frontend requires `VITE_CLERK_PUBLISHABLE_KEY`.
See the complete [Clerk setup and integration guide](docs/clerk.md).

## User profile API

`user-service` exposes self-service endpoints whose owner is always taken from the verified Clerk session token's
`sub` claim:

- `GET /api/v1/me` — get the current profile
- `PUT /api/v1/me` — create or replace display name, email, and E.164 phone number
- `PUT /api/v1/me/notification-preferences` — replace email/SMS preferences
- `POST /api/v1/me/email/verification-requests` — send an email verification code via Postmark
- `POST /api/v1/me/email/verifications` — confirm the email verification code
- `POST /api/v1/me/phone/verification-requests` — start Twilio Verify for the profile phone
- `POST /api/v1/me/phone/verifications` — confirm the SMS verification code
- `DELETE /api/v1/me` — soft-delete the profile and disable notifications

Email and phone destinations must be verified before they become effective delivery channels. Email
verification uses a hashed one-time code delivered by Postmark. Phone verification uses Twilio Verify.
SMS opt-in/opt-out changes are audited.

`notification-service` projects `UserNotificationSettingsUpdated` into a local settings table, fans out
`CourseSectionBecameAvailable` into idempotent `delivery_attempts`, and sends via Postmark (email) and
Twilio (SMS). Configure:

- `POSTMARK_SERVER_TOKEN`, `POSTMARK_FROM_EMAIL`
- `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`
- `TWILIO_FROM_NUMBER` and/or `TWILIO_MESSAGING_SERVICE_SID` (alerts)
- `TWILIO_VERIFY_SERVICE_SID` (phone verification in user-service)

Create or replace a profile (omitted nullable fields are cleared):

```json
{
  "displayName": "Daniel",
  "email": "daniel@ucsd.edu",
  "phoneNumber": "+18585550123"
}
```

Phone numbers must already be in E.164 format. A new profile returns `201 Created`; an update returns `200 OK`. `GET`
returns `404 Not Found` until the profile has been created.

Replace notification preferences:

```json
{
  "emailEnabled": true,
  "smsEnabled": true,
  "smsConsentAccepted": true,
  "smsConsentPolicyVersion": "2026-08-30"
}
```

The consent fields are required only when SMS transitions from disabled to enabled. Enabling a channel requires its
contact value to exist. Changing a phone number automatically disables SMS and records an opt-out for the old number.
Profile and preference changes atomically write a versioned `UserNotificationSettingsUpdated` event to the user-service
outbox; the relay publishes it with the Clerk user ID from the verified token subject as the Kafka key.

Request email verification (Postmark):

```http
POST /api/v1/me/email/verification-requests
```

Confirm with:

```json
{ "code": "123456" }
```

Request phone verification (Twilio Verify) and confirm with the same `{ "code": "..." }` shape on
`/api/v1/me/phone/verification-requests` and `/api/v1/me/phone/verifications`.

## Database migrations

Flyway is enabled in every service. Placeholder `V1__baseline.sql` migrations intentionally contain no schema design.
Add service-owned `V2` and later migrations under:

```text
services/<service>/src/main/resources/db/migration
```

Hibernate is configured with `ddl-auto=validate`; application startup never creates or mutates the schema.

## Event conventions

Event payloads are Java records under `shared/event-contracts`. Topic names are centralized in `KafkaTopics`. Producers
use JSON, idempotence, and `acks=all`; consumers use service-specific consumer groups, record acknowledgements, and
container-managed offset commits. Use a stable `courseId:term` Kafka key for course-scoped ordering.
