#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

DB_CONTAINER="oraclexe"
DB_IMAGE="gvenzl/oracle-xe:21-slim"
DB_PASSWORD="1234"
DB_CONN="system/${DB_PASSWORD}@//localhost:1521/XEPDB1"
WAR_PATH="dist/EduFlow.war"
FORCE_DB_INIT="${FORCE_DB_INIT:-0}"

log() { printf "\n[EduFlow] %s\n" "$1"; }
fail() { printf "\n[EduFlow][ERROR] %s\n" "$1"; exit 1; }

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || fail "Missing required command: $1"
}

require_cmd java
require_cmd ant
require_cmd docker

docker_cmd=(docker)
if ! docker info >/dev/null 2>&1; then
  if command -v sudo >/dev/null 2>&1 && sudo -n docker info >/dev/null 2>&1; then
    docker_cmd=(sudo docker)
  else
    fail "Docker daemon is not reachable for current user. Start Docker or allow docker group/sudo access."
  fi
fi

compose_cmd=()
if "${docker_cmd[@]}" compose version >/dev/null 2>&1; then
  compose_cmd=("${docker_cmd[@]}" compose)
elif command -v docker-compose >/dev/null 2>&1; then
  compose_cmd=(docker-compose)
fi

tomcat_cmd=()
if [[ -d "${CATALINA_HOME:-}" && ! -w "${CATALINA_HOME}/webapps" ]]; then
  if command -v sudo >/dev/null 2>&1; then
    tomcat_cmd=(sudo)
  fi
fi

if [[ -z "${CATALINA_HOME:-}" ]]; then
  fail "CATALINA_HOME is not set. Example: export CATALINA_HOME=\"$HOME/tomcat\""
fi

[[ -d "$CATALINA_HOME" ]] || fail "CATALINA_HOME directory not found: $CATALINA_HOME"
[[ -x "$CATALINA_HOME/bin/startup.sh" ]] || fail "Tomcat startup script not found in $CATALINA_HOME/bin"

JAVA_MAJOR="$(java -version 2>&1 | awk -F '[\".]' '/version/ {print $2; exit}')"
if [[ -n "$JAVA_MAJOR" && "$JAVA_MAJOR" -lt 17 ]]; then
  fail "Java 17+ is required. Found: $JAVA_MAJOR"
fi

shopt -s nullglob
OJDBC=(lib/ojdbc.jar)
shopt -u nullglob
if [[ ${#OJDBC[@]} -eq 0 ]]; then
  fail "Oracle JDBC jar missing. Put ojdbc jar in lib/ (e.g., lib/ojdbc.jar)."
fi

if [[ -f docker-compose.yml && ${#compose_cmd[@]} -gt 0 ]]; then
  log "Starting Oracle XE via docker compose"
  "${compose_cmd[@]}" up -d
else
  if ! "${docker_cmd[@]}" ps -a --format '{{.Names}}' | grep -qx "$DB_CONTAINER"; then
    log "Creating Oracle XE container"
    "${docker_cmd[@]}" run -d --name "$DB_CONTAINER" -p 1521:1521 -p 5500:5500 -e ORACLE_PASSWORD="$DB_PASSWORD" "$DB_IMAGE" >/dev/null
  else
    log "Starting existing Oracle XE container"
    "${docker_cmd[@]}" start "$DB_CONTAINER" >/dev/null || true
  fi
fi

log "Waiting for Oracle XE readiness"
READY=0
for _ in {1..180}; do
  # Container must be running first.
  if ! "${docker_cmd[@]}" ps --format '{{.Names}}' | grep -qx "$DB_CONTAINER"; then
    sleep 2
    continue
  fi

  # Real readiness check: can we open an Oracle session?
  if "${docker_cmd[@]}" exec -i "$DB_CONTAINER" bash -lc "echo 'EXIT;' | sqlplus -s $DB_CONN" >/dev/null 2>&1; then
    READY=1
    break
  fi
  sleep 2
done
[[ "$READY" -eq 1 ]] || fail "Oracle XE did not become ready in time. Check: ${docker_cmd[*]} logs $DB_CONTAINER"

db_has_schema=0
if "${docker_cmd[@]}" exec -i "$DB_CONTAINER" bash -lc "sqlplus -s $DB_CONN <<'SQL'
SET HEADING OFF FEEDBACK OFF PAGESIZE 0 VERIFY OFF ECHO OFF
SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME='USERS';
EXIT;
SQL" | tr -d '[:space:]' | grep -q '^1$'; then
  db_has_schema=1
fi

if [[ "$FORCE_DB_INIT" == "1" || "$db_has_schema" -eq 0 ]]; then
  log "Applying schema and seed"
  "${docker_cmd[@]}" cp database/schema.sql "$DB_CONTAINER":/tmp/schema.sql
  "${docker_cmd[@]}" cp database/seed.sql "$DB_CONTAINER":/tmp/seed.sql
  "${docker_cmd[@]}" exec -i "$DB_CONTAINER" bash -lc "sqlplus -s $DB_CONN <<'SQL'
@/tmp/schema.sql
@/tmp/seed.sql
COMMIT;
EXIT;
SQL"
else
  log "Schema already exists; skipping schema/seed apply. Set FORCE_DB_INIT=1 to re-apply."
fi

log "Building WAR"
ant clean dist
[[ -f "$WAR_PATH" ]] || fail "WAR not generated at $WAR_PATH"

log "Deploying WAR to Tomcat"
if ! "${tomcat_cmd[@]}" cp "$WAR_PATH" "$CATALINA_HOME/webapps/EduFlow.war" 2>/dev/null; then
  if command -v sudo >/dev/null 2>&1; then
    log "Normal deploy failed; retrying WAR copy with sudo"
    sudo cp "$WAR_PATH" "$CATALINA_HOME/webapps/EduFlow.war"
    tomcat_cmd=(sudo)
  else
    fail "Tomcat deploy failed due to permissions and sudo is unavailable."
  fi
fi
"${tomcat_cmd[@]}" "$CATALINA_HOME/bin/shutdown.sh" >/dev/null 2>&1 || true
"${tomcat_cmd[@]}" "$CATALINA_HOME/bin/startup.sh" >/dev/null

log "Setup complete"
printf "Open: http://localhost:8080/EduFlow/login.jsp\n"
printf "Admin: admin@demo.com / admin123\n"
printf "Teacher: teacher@demo.com / teacher123\n"
printf "Student: student@demo.com / student123\n"
