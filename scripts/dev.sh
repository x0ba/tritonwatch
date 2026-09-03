#!/usr/bin/env bash
set -euo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
compose_file="$repository_root/infra/docker-compose.yml"
frontend_directory="$repository_root/frontend"
log_directory="$repository_root/.dev-logs"
child_pids=()
follower_pids=()

color_reset=""
color_user=""
color_watchlist=""
color_ingestion=""
color_notification=""
color_frontend=""
color_dim=""

if [[ -t 1 ]]; then
  color_reset=$'\033[0m'
  color_user=$'\033[34m'
  color_watchlist=$'\033[32m'
  color_ingestion=$'\033[33m'
  color_notification=$'\033[35m'
  color_frontend=$'\033[36m'
  color_dim=$'\033[2m'
fi

compose() {
  docker compose -f "$compose_file" "$@"
}

usage() {
  cat <<'EOF'
Start the Tritonwatch local development environment.

Usage:
  ./scripts/dev.sh          Start Docker infra, backend services, and the frontend
  ./scripts/dev.sh up       Same as the default
  ./scripts/dev.sh down     Stop leftover app processes and Docker containers
  ./scripts/dev.sh help     Show this help

Docker stays up after Ctrl+C so Postgres, Kafka, and Redis restart quickly.
Use ./scripts/dev.sh down to stop those containers. Database volumes are kept.

Required once:
  cp .env.example .env
  cp frontend/.env.example frontend/.env
EOF
}

require_command() {
  local command_name="$1"
  local hint="${2:-}"
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "Required command is not installed: $command_name" >&2
    if [[ -n "$hint" ]]; then
      echo "$hint" >&2
    fi
    exit 1
  fi
}

load_sdkman() {
  local sdkman_init="${SDKMAN_DIR:-$HOME/.sdkman}/bin/sdkman-init.sh"
  if [[ ! -s "$sdkman_init" ]]; then
    return 0
  fi

  set +u
  # shellcheck disable=SC1090
  source "$sdkman_init"
  set -u
  pushd "$repository_root" >/dev/null
  set +u
  sdk env >/dev/null
  set -u
  popd >/dev/null
}

require_env_file() {
  local file_path="$1"
  local example_path="$2"
  if [[ -f "$file_path" ]]; then
    return 0
  fi
  echo "Missing $file_path" >&2
  echo "Copy $example_path and fill in the values." >&2
  exit 1
}

env_file_has_placeholder() {
  local file_path="$1"
  local key="$2"
  local placeholder="$3"
  local value
  value="$(awk -F= -v key="$key" '$1 == key { print substr($0, index($0, "=") + 1); exit }' "$file_path")"
  [[ -z "$value" || "$value" == *"$placeholder"* ]]
}

wait_for_tcp() {
  local name="$1"
  local host="$2"
  local port="$3"
  local timeout_seconds="${4:-90}"
  local elapsed=0

  echo "Waiting for $name on ${host}:${port}..."
  while ((elapsed < timeout_seconds)); do
    if bash -c "echo >/dev/tcp/${host}/${port}" >/dev/null 2>&1; then
      echo "$name is ready."
      return 0
    fi
    sleep 1
    elapsed=$((elapsed + 1))
  done

  echo "Timed out waiting for $name on ${host}:${port}" >&2
  compose ps
  return 1
}

wait_for_kafka_init() {
  local timeout_seconds="${1:-90}"
  local elapsed=0
  local container_id=""
  local status=""
  local exit_code=""

  echo "Waiting for Kafka topics..."
  while ((elapsed < timeout_seconds)); do
    container_id="$(compose ps -a -q kafka-init || true)"
    if [[ -n "$container_id" ]]; then
      status="$(docker inspect -f '{{.State.Status}}' "$container_id")"
      exit_code="$(docker inspect -f '{{.State.ExitCode}}' "$container_id")"
      if [[ "$status" == "exited" ]]; then
        if [[ "$exit_code" == "0" ]]; then
          echo "Kafka topics are ready."
          return 0
        fi
        echo "kafka-init exited with status $exit_code" >&2
        compose logs kafka-init
        return 1
      fi
    fi
    sleep 1
    elapsed=$((elapsed + 1))
  done

  echo "Timed out waiting for kafka-init to create topics." >&2
  compose logs kafka-init
  return 1
}

listening_pids_on_port() {
  local port="$1"
  lsof -nP -iTCP:"$port" -sTCP:LISTEN -t 2>/dev/null || true
}

stop_port() {
  local port="$1"
  local pids
  pids="$(listening_pids_on_port "$port")"
  if [[ -z "$pids" ]]; then
    return 0
  fi
  echo "Stopping process on port $port"
  # shellcheck disable=SC2086
  kill $pids 2>/dev/null || true
}

stop_app_ports() {
  local port
  for port in 8081 8082 8083 8084 5173; do
    stop_port "$port"
  done
}

kill_pid_tree() {
  local pid="$1"
  local child
  if ! kill -0 "$pid" 2>/dev/null; then
    return 0
  fi
  while read -r child; do
    [[ -n "$child" ]] && kill_pid_tree "$child"
  done < <(pgrep -P "$pid" || true)
  kill "$pid" 2>/dev/null || true
}

cleanup() {
  trap - EXIT INT TERM
  echo
  echo "Stopping backend services and frontend..."
  local pid
  for pid in "${follower_pids[@]+"${follower_pids[@]}"}"; do
    kill_pid_tree "$pid"
  done
  for pid in "${child_pids[@]+"${child_pids[@]}"}"; do
    kill_pid_tree "$pid"
  done
  stop_app_ports
  wait || true
  echo "Apps stopped. Docker is still running."
  echo "Stop infra with: $0 down"
}

follow_log() {
  local name="$1"
  local color="$2"
  local log_file="$3"

  : >"$log_file"
  (
    tail -n +1 -F "$log_file" 2>/dev/null | while IFS= read -r line; do
      printf '%s%-16s%s %s\n' "$color" "[$name]" "$color_reset" "$line"
    done
  ) &
  follower_pids+=("$!")
}

start_logged() {
  local name="$1"
  local color="$2"
  local directory="$3"
  shift 3
  local log_file="$log_directory/${name}.log"

  follow_log "$name" "$color" "$log_file"
  (
    cd "$directory"
    exec "$@"
  ) >>"$log_file" 2>&1 &
  local pid
  pid=$!
  child_pids+=("$pid")
  echo "  ${name}  pid ${pid}  ${color_dim}${log_file}${color_reset}"
}

start_infra() {
  require_command docker "Install Docker Desktop or Colima, then retry."
  if ! docker info >/dev/null 2>&1; then
    echo "Docker is not running. Start Docker Desktop or Colima first." >&2
    exit 1
  fi
  if ! docker compose version >/dev/null 2>&1; then
    echo "Docker Compose v2 is required (docker compose)." >&2
    exit 1
  fi

  echo "Starting Postgres, Kafka, and Redis..."
  compose up -d
  wait_for_tcp Postgres localhost 5432 90
  wait_for_tcp Kafka localhost 9092 120
  wait_for_tcp Redis localhost 6379 60
  wait_for_kafka_init 90
}

start_apps() {
  load_sdkman
  require_command java "Install sdkman, then run: sdk env"
  require_command vp "Install Vite+: https://viteplus.dev/"

  require_env_file "$repository_root/.env" "$repository_root/.env.example"
  require_env_file "$frontend_directory/.env" "$frontend_directory/.env.example"

  if env_file_has_placeholder "$repository_root/.env" "CLERK_ISSUER" "YOUR_INSTANCE"; then
    echo "Set CLERK_ISSUER in .env before starting the backend." >&2
    exit 1
  fi
  if env_file_has_placeholder "$frontend_directory/.env" "VITE_CLERK_PUBLISHABLE_KEY" "REPLACE_ME"; then
    echo "Set VITE_CLERK_PUBLISHABLE_KEY in frontend/.env before starting the frontend." >&2
    exit 1
  fi

  mkdir -p "$log_directory"
  stop_app_ports

  if [[ ! -d "$frontend_directory/node_modules" ]]; then
    echo "Installing frontend dependencies..."
    (cd "$frontend_directory" && vp install)
  fi

  echo
  echo "Starting services..."
  start_logged user "$color_user" "$repository_root/services/user-service" \
    ./gradlew --console=plain bootRun
  start_logged watchlist "$color_watchlist" "$repository_root/services/watchlist-service" \
    ./gradlew --console=plain bootRun
  start_logged ingestion "$color_ingestion" "$repository_root/services/ingestion-service" \
    ./gradlew --console=plain bootRun
  start_logged notification "$color_notification" "$repository_root/services/notification-service" \
    ./gradlew --console=plain bootRun
  start_logged frontend "$color_frontend" "$frontend_directory" \
    vp dev

  cat <<EOF

${color_dim}Local endpoints${color_reset}
  Frontend       http://localhost:5173
  User API       http://localhost:8081
  Watchlist API  http://localhost:8082
  Ingestion API  http://localhost:8083
  Notification   http://localhost:8084
  Postgres       localhost:5432
  Kafka          localhost:9092
  Redis          localhost:6379

Logs are in .dev-logs/. Ctrl+C stops the apps.
EOF

  wait
}

down() {
  echo "Stopping leftover app processes..."
  stop_app_ports
  echo "Stopping Docker containers..."
  compose down
  echo "Infra stopped. Postgres data is still in the compose volume."
}

command_name="${1:-up}"
case "$command_name" in
  up | start)
    trap cleanup EXIT INT TERM
    start_infra
    start_apps
    ;;
  down | stop)
    down
    ;;
  help | -h | --help)
    usage
    ;;
  *)
    echo "Unknown command: $command_name" >&2
    usage >&2
    exit 1
    ;;
esac
