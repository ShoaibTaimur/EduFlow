#!/usr/bin/env bash
set -euo pipefail

# One-command launcher for EduFlow on Linux.
# It prepares prerequisites runtime state and delegates full setup to scripts/setup.sh.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

log() { printf "\n[EduFlow Launcher] %s\n" "$1"; }
fail() { printf "\n[EduFlow Launcher][ERROR] %s\n" "$1"; exit 1; }

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || fail "Missing required command: $1"
}

detect_tomcat_home() {
  if [[ -n "${CATALINA_HOME:-}" && -x "${CATALINA_HOME}/bin/startup.sh" ]]; then
    return 0
  fi

  local candidates=(
    "$HOME/tomcat"
    "$HOME/apache-tomcat-10.1.0"
    "/opt/tomcat"
    "/usr/local/tomcat"
  )

  for c in "${candidates[@]}"; do
    if [[ -x "$c/bin/startup.sh" ]]; then
      export CATALINA_HOME="$c"
      return 0
    fi
  done

  return 1
}

require_cmd bash
require_cmd java
require_cmd ant
require_cmd docker

if [[ ! -f "scripts/setup.sh" ]]; then
  fail "scripts/setup.sh not found."
fi

if [[ ! -f "lib/ojdbc.jar" ]]; then
  fail "Missing lib/ojdbc.jar. Add Oracle JDBC jar first."
fi

if ! detect_tomcat_home; then
  fail "CATALINA_HOME is not set and Tomcat could not be auto-detected. Export CATALINA_HOME and retry."
fi

log "Using CATALINA_HOME=$CATALINA_HOME"

if ! docker info >/dev/null 2>&1; then
  log "Docker may require sudo or daemon start. Trying to start docker service with sudo..."
  sudo systemctl start docker || true
fi

log "Ensuring setup script is executable"
chmod +x scripts/setup.sh

log "Running full setup/deploy pipeline"
./scripts/setup.sh

log "Done. Open: http://localhost:8080/EduFlow/login.jsp"
