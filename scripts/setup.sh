#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

DB_CONTAINER="oraclexe"
DB_IMAGE="gvenzl/oracle-xe:21-slim"
DB_PASSWORD="1234"
DB_CONN="system/${DB_PASSWORD}@//localhost:1521/XEPDB1"
WAR_PATH="dist/EduFlow.war"

log() { printf "\n[EduFlow] %s\n" "$1"; }
fail() { printf "\n[EduFlow][ERROR] %s\n" "$1"; exit 1; }

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || fail "Missing required command: $1"
}

require_cmd java
require_cmd ant
require_cmd docker

if [[ -z "${CATALINA_HOME:-}" ]]; then
  fail "CATALINA_HOME is not set. Example: export CATALINA_HOME=\"$HOME/tomcat\""
fi

[[ -d "$CATALINA_HOME" ]] || fail "CATALINA_HOME directory not found: $CATALINA_HOME"
[[ -x "$CATALINA_HOME/bin/startup.sh" ]] || fail "Tomcat startup script not found in $CATALINA_HOME/bin"

JAVA_MAJOR="$(java -version 2>&1 | awk -F '[\".]' '/version/ {print $2; exit}')"
if [[ -n "$JAVA_MAJOR" && "$JAVA_MAJOR" -lt 17 ]]; then
  fail "Java 17+ is required. Found: $JAVA_MAJOR"
fi

if ! docker info >/dev/null 2>&1; then
  fail "Docker daemon is not reachable. Start Docker first."
fi

shopt -s nullglob
OJDBC=(lib/ojdbc.jar)
shopt -u nullglob
if [[ ${#OJDBC[@]} -eq 0 ]]; then
  fail "Oracle JDBC jar missing. Put ojdbc jar in lib/ (e.g., lib/ojdbc.jar)."
fi

if [[ -f docker-compose.yml ]]; then
  log "Starting Oracle XE via docker compose"
  docker compose up -d
else
  if ! docker ps -a --format '{{.Names}}' | grep -qx "$DB_CONTAINER"; then
    log "Creating Oracle XE container"
    docker run -d --name "$DB_CONTAINER" -p 1521:1521 -p 5500:5500 -e ORACLE_PASSWORD="$DB_PASSWORD" "$DB_IMAGE" >/dev/null
  else
    log "Starting existing Oracle XE container"
    docker start "$DB_CONTAINER" >/dev/null || true
  fi
fi

log "Waiting for Oracle XE readiness"
READY=0
for _ in {1..120}; do
  if docker logs "$DB_CONTAINER" 2>&1 | grep -q "DATABASE IS READY TO USE"; then
    READY=1
    break
  fi
  sleep 2
done
[[ "$READY" -eq 1 ]] || fail "Oracle XE did not become ready in time."

log "Applying schema and seed"
docker cp database/schema.sql "$DB_CONTAINER":/tmp/schema.sql
docker cp database/seed.sql "$DB_CONTAINER":/tmp/seed.sql
docker exec -i "$DB_CONTAINER" bash -lc "sqlplus -s $DB_CONN <<'SQL'
@/tmp/schema.sql
@/tmp/seed.sql
COMMIT;
EXIT;
SQL"

log "Building WAR"
ant clean dist
[[ -f "$WAR_PATH" ]] || fail "WAR not generated at $WAR_PATH"

log "Deploying WAR to Tomcat"
cp "$WAR_PATH" "$CATALINA_HOME/webapps/EduFlow.war"
"$CATALINA_HOME/bin/shutdown.sh" >/dev/null 2>&1 || true
"$CATALINA_HOME/bin/startup.sh" >/dev/null

log "Setup complete"
printf "Open: http://localhost:8080/EduFlow/login.jsp\n"
printf "Admin: admin@demo.com / admin123\n"
printf "Teacher: teacher@demo.com / teacher123\n"
printf "Student: student@demo.com / student123\n"
