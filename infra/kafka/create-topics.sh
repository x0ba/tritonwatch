#!/bin/sh
set -eu

BOOTSTRAP_SERVER="${KAFKA_BOOTSTRAP_SERVER:-kafka:29092}"

create_topic() {
  /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server "$BOOTSTRAP_SERVER" \
    --create \
    --if-not-exists \
    --topic "$1" \
    --partitions "${KAFKA_TOPIC_PARTITIONS:-3}" \
    --replication-factor 1 \
    --config cleanup.policy=delete \
    --config retention.ms="${KAFKA_TOPIC_RETENTION_MS:-604800000}"
}

create_topic tritonwatch.user-course-watch-created.v1
create_topic tritonwatch.course-tracking-requested.v1
create_topic tritonwatch.course-section-became-available.v1
