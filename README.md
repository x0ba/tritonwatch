# Tritonwatch

WIP: App that alerts students via email or text when their UCSD courses open up.

## Architecture

Tritonwatch makes use of an event-driven microservice architecture, using Kafka for communication between microservices.

- [tldraw diagram](https://www.tldraw.com/f/uMOnB2d7simw__NtL01GR?d=v-184.603.1179.812.page) (wildly out of date I fear)
- `services/user-service` - handles user profiles and notification prefs (port `8081`)
- `services/watchlist-service` - hosts API endpoints for watchlist operations (port `8082`)
- `ingestion-service` - polls UCSD class planner API (`https://classplanner.apps.ucsd.edu/api/v1/catalog/courses/search`) for course changes, also handles course search (port `8083`)
- `notification-service` - WIP, sends notifications to users with Twilio and/or Postmark (port `8084`)
- `shared/event-contracts` - versioned event payloads & topic names

## How does it work?

The entire thing is powered by the [UCSD Class Planner's](https://classplanner.apps.ucsd.edu/workspace?term=FA26) public API. 

When a user watches a course, the watchlist microservice saves that watch and fires off an event to the ingestion service. The ingestion service keeps a list of all the courses that everyone using Tritonwatch has watched, and sends that full list on every request to `POST https://classplanner.apps.ucsd.edu/api/v1/catalog/courses/search`. Class Planner paginates search results with a page size of 48, so if more than 48 watched courses match, ingestion walks `offset` until it has collected every page. This runs every 2 minutes, and if a course ever becomes available, an event is sent to the notification microservice which then sends the notification to the user with Postmark and/or Twilio.

The search function for courses just calls the same API as the search functionality in the class planner app.

## Getting started

Install [sdkman](https://sdkman.io/) to manage Java versions, [vite-plus](https://viteplus.dev/) the React frontend, and [mise-en-place](https://mise.jdx.dev/) for further dev tooling (AWS, Terraform). I would use mise to manage Java versions but it doesn't integrate very well with Gradle atm (see [gradle/issues/29508](https://github.com/gradle/gradle/issues/29508) and [gradle/issues/29355](https://github.com/gradle/gradle/issues/29355)).

Run these commands to install and use the required tooling:

```bash

mise activate
sdk env
```

Then start the local PostgreSQL, Kafka, and Redis containers. Postgres databases and Kafka topics will be created *once* on Docker compose start; if you add a database or a topic you'll have to take the volume down and recreate it.

```bash
docker compose -f infra/docker-compose.yml up -d
```

Copy the example environment file to `.env` and fill in everything:

```bash
cp .env.example .env
```

Start each service as follows:

```bash
cd services/watchlist-service
./gradlew bootRun
```

And start the frontend:

```bash
cd frontend
vp i
vp dev
```

## Deployment

The [production deployment](https://tritonwatch.app) is deployed on a single `t3a.medium` in ECS (`us-west-2`). I didn't wanna pay **$500+ dollars per month** (wtf), so Postgres and Kafka are containers on that instance too. Their data is on the instance disk at `/opt/tritonwatch`. AWS Backup snapshots the whole machine daily and keeps seven days.

The frontend is served on CloudFront. The Vite build is stored in a private S3 bucket. `/api/*` and `/health/*` go to Caddy on the instance, which proxies to the Spring services. The frontend calls `https://tritonwatch.app` for APIs as well, same origin. The security group only lets CloudFront onto port 80. Redis from the local compose file is not in production.

The ECS task is `infra/production/compose.yml` turned into a task definition: Postgres, Kafka, the four services, Caddy. One task. A deploy stops the old one before starting the new one, so the APIs go down for a brief moment on every deploy (I apologize).

Push to `main` deploys via GitHub Actions. The job assumes an IAM role with OIDC, so there are no AWS keys in GitHub. Frontend-only changes build and sync to S3. Service or infra changes rebuild images, or retag them if that source hash is already in ECR, then apply Terraform in `infra/aws-ecs`. Images get the commit SHA as a tag. The job finishes by curling `/health/user`, `/health/watchlist`, and `/health/ingestion`.

Terraform state lives in `s3://x0ba-tritonwatch-tfstate`. The real `terraform.tfvars` is a GitHub environment secret. `infra/aws-ecs/terraform.tfvars.example` is the example file without the secrets. 

Clerk, Postmark, and Twilio are the external services used outside of AWS.
