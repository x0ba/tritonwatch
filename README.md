# Tritonwatch

WIP: App that alerts students via email or text when their UCSD courses open up.

## Architecture

- [tldraw diagram](https://www.tldraw.com/f/uMOnB2d7simw__NtL01GR?d=v-184.603.1179.812.page)
- `user-service` — user profiles and notification preferences (port `8081`)
- `watchlist-service` — watch requests and watch-created events (port `8082`)
- `ingestion-service` — tracking and UCSD polling (port `8083`)
- `notification-service` — subscriptions and notifications (port `8084`)
- `shared/event-contracts` — the versioned event payloads and topic constants

The services are independent Gradle builds. Each includes `event-contracts` as a Gradle composite build, so no local
Maven publish step is needed.

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

The cost-optimized production deployment uses Amazon ECS with one EC2 container instance. See the complete [Amazon ECS deployment guide](docs/deployment-ecs.md).

## Running a service

```bash
cd services/watchlist-service
./gradlew bootRun
```

Most runtime settings have local defaults and can be overridden with environment variables, including `DATABASE_URL`,
`DATABASE_USERNAME`, `DATABASE_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`, and `SERVER_PORT`. Ingestion also accepts
`INGESTION_POLL_INTERVAL` and `UCSD_API_BASE_URL`.

`watchlist-service` and `user-service` are Auth0-protected resource servers and also require `AUTH0_ISSUER` and
`AUTH0_AUDIENCE`.
See the complete [Auth0 setup and integration guide](docs/auth0.md).

## User profile API

`user-service` exposes self-service endpoints whose owner is always taken from the verified access token's `sub` claim:

- `GET /api/v1/me` — get the current profile (`read:user-profile`)
- `PUT /api/v1/me` — create or replace display name, email, and E.164 phone number (`update:user-profile`)
- `PUT /api/v1/me/notification-preferences` — replace email/SMS preferences (`update:user-profile`)
- `DELETE /api/v1/me` — soft-delete the profile and disable notifications (`update:user-profile`)

Email and phone destinations are intentionally unverified until provider-backed verification is implemented. The API
reports configured preferences separately from effective delivery channels, and SMS opt-in/opt-out changes are audited.

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
outbox; the relay publishes it with the Auth0 subject as the Kafka key.

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
