# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:25-jdk-noble AS build

ARG SERVICE_NAME

WORKDIR /workspace

COPY shared/event-contracts/build.gradle.kts \
     shared/event-contracts/settings.gradle.kts \
     ./shared/event-contracts/
COPY shared/event-contracts/src ./shared/event-contracts/src

COPY services/${SERVICE_NAME}/gradlew \
     services/${SERVICE_NAME}/build.gradle.kts \
     services/${SERVICE_NAME}/settings.gradle.kts \
     ./services/${SERVICE_NAME}/
COPY services/${SERVICE_NAME}/gradle ./services/${SERVICE_NAME}/gradle
COPY services/${SERVICE_NAME}/src ./services/${SERVICE_NAME}/src

WORKDIR /workspace/services/${SERVICE_NAME}

RUN --mount=type=cache,id=gradle-${SERVICE_NAME},target=/root/.gradle \
    chmod +x gradlew \
    && ./gradlew bootJar --no-daemon \
    && jar_path="$(find build/libs \
        -maxdepth 1 \
        -type f \
        -name '*.jar' \
        ! -name '*-plain.jar' \
        -print \
        -quit)" \
    && test -n "$jar_path" \
    && cp "$jar_path" /workspace/application.jar

FROM eclipse-temurin:25-jre-noble AS runtime

RUN apt-get update \
    && apt-get install --yes --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 tritonwatch \
    && useradd \
        --uid 10001 \
        --gid tritonwatch \
        --no-create-home \
        --shell /usr/sbin/nologin \
        tritonwatch

WORKDIR /app

COPY --from=build /workspace/application.jar /app/application.jar

USER 10001:10001

ENTRYPOINT ["java", "-jar", "/app/application.jar"]
