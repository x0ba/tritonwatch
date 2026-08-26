# Tritonwatch

WIP: App that alerts students via email or text when their UCSD courses open up.

## Architecture

- [tldraw diagram](https://www.tldraw.com/f/uMOnB2d7simw__NtL01GR?d=v-184.603.1179.812.page)
- `course-service` — course-facing API (port `8081`)
- `watchlist-service` — watch requests and watch-created events (port `8082`)
- `ingestion-service` — tracking and UCSD polling (port `8083`)
- `notification-service` — subscriptions and notifications (port `8084`)
- `shared/event-contracts` — the versioned event payloads and topic constants

The services are independent Gradle builds. Each includes `event-contracts` as a Gradle composite build, so no local Maven publish step is needed.

## Local infrastructure

Start PostgreSQL, Kafka, and Redis:

```bash
docker compose -f infra/docker-compose.yml up -d
```

Kafka runs in KRaft mode at `localhost:9092`; auto topic creation is disabled. The one-shot `kafka-init` container creates:

- `tritonwatch.user-course-watch-created.v1`
- `tritonwatch.course-tracking-requested.v1`
- `tritonwatch.course-section-changed.v1`

To reset all local database state (the database initialization script only runs on a new volume):

```bash
docker compose -f infra/docker-compose.yml down -v
```

## Running a service

```bash
cd services/watchlist-service
./gradlew bootRun
```

All runtime settings have local defaults and can be overridden with environment variables, including `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`, and `SERVER_PORT`. Ingestion also accepts `INGESTION_POLL_INTERVAL` and `UCSD_API_BASE_URL`.

## Database migrations

Flyway is enabled in every service. Placeholder `V1__baseline.sql` migrations intentionally contain no schema design. Add service-owned `V2` and later migrations under:

```text
services/<service>/src/main/resources/db/migration
```

Hibernate is configured with `ddl-auto=validate`; application startup never creates or mutates the schema.

## Event conventions

Event payloads are Java records under `shared/event-contracts`. Topic names are centralized in `KafkaTopics`. Producers use JSON, idempotence, and `acks=all`; consumers use service-specific consumer groups, record acknowledgements, and container-managed offset commits. Use a stable `courseId:term` Kafka key for course-scoped ordering.
