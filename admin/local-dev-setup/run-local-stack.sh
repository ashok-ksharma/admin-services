#!/usr/bin/env bash
# run-local-stack.sh — local stack for admin-service: dependencies in Docker (deps/), the app on the host.
#
# Usage (from anywhere; on Windows use Git Bash or WSL):
#   ./run-local-stack.sh              # same as up
#   ./run-local-stack.sh up           # start the Docker dependencies, wait until healthy, load sample test data on a fresh DB
#   ./run-local-stack.sh down         # stop and remove the containers; the DB and keystore volumes are kept
#   ./run-local-stack.sh wipe         # down + delete the DB and keystore volumes, then up (fresh DB, new keys, sample test data)
#   ./run-local-stack.sh status       # container status
#   ./run-local-stack.sh logs [svc]   # follow logs (default: keymanager)
#   ./run-local-stack.sh tokens       # regenerate the WireMock IAM tokens and reload WireMock
#   ./run-local-stack.sh build        # Maven package admin-service (tests skipped)
#   ./run-local-stack.sh run [port]   # run the built jar against the stack (default port 8086; 8096 next to an IDE run)
#   ./run-local-stack.sh smoke [port] # check dependencies and the app (default port 8086)
#   ./run-local-stack.sh help

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_DIR="$SCRIPT_DIR/deps"
MAVEN_PARENT="$(cd "$SCRIPT_DIR/.." && pwd)"
APP_JAR_GLOB="$MAVEN_PARENT/admin-service/target/admin-service-*.jar"

CONFIG_URI="http://localhost:51200/config"
WIREMOCK_URL="http://localhost:8083"
KEYMANAGER_URL="http://localhost:8089/v1/keymanager"
SMOKE_USER="globaladmin"

info() { printf '[INFO] %s\n' "$*"; }
fail() { printf '[ERROR] %s\n' "$*" >&2; exit 1; }

need_cmd() { command -v "$1" >/dev/null 2>&1 || fail "'$1' not found on PATH"; }

compose() {
  need_cmd docker
  docker info >/dev/null 2>&1 || fail "Docker is not running"
  docker compose -f "$COMPOSE_DIR/docker-compose.yml" "$@"
}

app_jar() {
  local jar
  jar="$(ls $APP_JAR_GLOB 2>/dev/null | grep -v -- '-sources\|-javadoc\|\.original' | head -n 1 || true)"
  [[ -n "$jar" ]] || fail "admin-service jar not found; run: $0 build"
  echo "$jar"
}

cmd_up() {
  info "Starting dependencies (postgres, config-server, wiremock, keymanager)..."
  compose up -d --wait
  seed_sample_data
  compose ps
  info "Ready. Start admin-service from the IDE with:"
  info "  -Dspring.profiles.active=default,local -Dspring.cloud.config.uri=$CONFIG_URI"
}

# Sample test data (deps/mosip-data) is loaded once, on a fresh database; later changes survive "up".
seed_sample_data() {
  local zones
  zones="$(compose exec -T postgres psql -U postgres -d mosip_master -tAc 'select count(*) from master.zone')"
  if [[ "${zones//[[:space:]]/}" == "0" ]]; then
    info "Loading sample test data (deps/mosip-data) into mosip_master..."
    compose --profile seed run --rm masterdata-loader
  fi
}

cmd_down() {
  compose down
}

cmd_wipe() {
  # The keystore holds the keys that mosip_keymgr points to; both volumes go in the same step.
  info "Removing the containers, the DB volume and the keystore volume..."
  compose down -v
  cmd_up
}

cmd_status() {
  compose ps
}

cmd_logs() {
  compose logs -f "${1:-keymanager}"
}

cmd_tokens() {
  need_cmd java
  (cd "$SCRIPT_DIR" && java tools/jwt/GenerateTokens.java)
  if curl -fs "$WIREMOCK_URL/__admin/health" >/dev/null 2>&1; then
    curl -fs -X POST "$WIREMOCK_URL/__admin/mappings/reset" >/dev/null
    info "WireMock reloaded the mappings"
  fi
}

cmd_build() {
  need_cmd mvn
  (cd "$MAVEN_PARENT" && mvn -pl admin-service -am package -DskipTests -Dgpg.skip=true)
}

cmd_run() {
  local port="${1:-8086}" jar
  need_cmd java
  jar="$(app_jar)"
  curl -fs "$CONFIG_URI/admin/default,local" >/dev/null 2>&1 || fail "config server not reachable; run: $0 up"
  info "Running $(basename "$jar") on port $port (Ctrl+C to stop)"
  java --add-modules=ALL-SYSTEM --add-opens=java.base/java.lang=ALL-UNNAMED \
    -Dspring.profiles.active=default,local \
    -Dspring.cloud.config.uri="$CONFIG_URI" \
    -Dserver.port="$port" \
    -jar "$jar"
}

check() {
  local name="$1" expected="$2" code
  shift 2
  code="$(curl -s -o /dev/null -w '%{http_code}' "$@" || true)"
  if [[ "$code" == "$expected" ]]; then
    printf '  [ OK ] %-45s %s\n' "$name" "$code"
  else
    printf '  [FAIL] %-45s %s (expected %s)\n' "$name" "$code" "$expected"
    SMOKE_FAILED=1
  fi
}

check_contains() {
  local name="$1" expected="$2" body
  shift 2
  body="$(curl -s "$@" || true)"
  if [[ "$body" == *"$expected"* ]]; then
    printf '  [ OK ] %-45s %s\n' "$name" "$expected"
  else
    printf '  [FAIL] %-45s missing %s\n' "$name" "$expected"
    SMOKE_FAILED=1
  fi
}

cmd_smoke() {
  local port="${1:-8086}" app="http://localhost:${1:-8086}" token
  SMOKE_FAILED=0
  info "Dependencies"
  check "config-server admin/default,local" 200 "$CONFIG_URI/admin/default,local"
  check "wiremock health" 200 "$WIREMOCK_URL/__admin/health"
  check "keymanager health" 200 "$KEYMANAGER_URL/actuator/health"
  check "keymanager KERNEL:SIGN certificate" 200 "$KEYMANAGER_URL/getCertificate?applicationId=KERNEL&referenceId=SIGN"

  token="$(curl -s -D - -o /dev/null -X POST "$WIREMOCK_URL/v1/authmanager/authenticate/internal/useridPwd" \
    -H 'Content-Type: application/json' -d "{\"request\":{\"userName\":\"$SMOKE_USER\"}}" \
    | awk 'tolower($1)=="authorization:" {print $2}' | tr -d '\r')"
  [[ -n "$token" ]] || { printf '  [FAIL] login %s on WireMock\n' "$SMOKE_USER"; SMOKE_FAILED=1; }

  info "admin-service on port $port"
  check "health" 200 "$app/v1/admin/actuator/health"
  check "masterdata languages (token)" 200 -H "Cookie: Authorization=$token" "$app/v1/masterdata/languages"
  check "masterdata languages (no token)" 401 "$app/v1/masterdata/languages"
  check "admin roles (token)" 200 -H "Cookie: Authorization=$token" "$app/v1/admin/roles"
  check_contains "sample test data: zone hierarchy (token)" '"code":"MOR"' -H "Cookie: Authorization=$token" \
    "$app/v1/masterdata/zones/hierarchy/eng"
  check "admin keymanager proxy KERNEL:SIGN (token)" 200 -H "Cookie: Authorization=$token" \
    "$app/v1/admin/keymanager/getCertificate?applicationId=KERNEL&referenceId=SIGN"

  [[ "$SMOKE_FAILED" == 0 ]] || fail "smoke checks failed"
  info "All smoke checks passed"
}

cmd_help() {
  sed -n '2,15p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
}

main() {
  local cmd="${1:-up}"
  [[ $# -gt 0 ]] && shift
  case "$cmd" in
    up)     cmd_up ;;
    down)   cmd_down ;;
    wipe)   cmd_wipe ;;
    status) cmd_status ;;
    logs)   cmd_logs "$@" ;;
    tokens) cmd_tokens ;;
    build)  cmd_build ;;
    run)    cmd_run "$@" ;;
    smoke)  cmd_smoke "$@" ;;
    help|-h|--help) cmd_help ;;
    *) cmd_help; fail "unknown command: $cmd" ;;
  esac
}

main "$@"
