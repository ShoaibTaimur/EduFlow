$ErrorActionPreference = 'Stop'

$RootDir = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $RootDir

$DbContainer = "oraclexe"
$DbImage = "gvenzl/oracle-xe:21-slim"
$DbPassword = "1234"
$DbConn = "system/$DbPassword@//localhost:1521/XEPDB1"
$WarPath = "dist/EduFlow.war"

function Fail($msg) {
  Write-Host "[EduFlow][ERROR] $msg" -ForegroundColor Red
  exit 1
}

function Require-Cmd($cmd) {
  if (-not (Get-Command $cmd -ErrorAction SilentlyContinue)) {
    Fail "Missing required command: $cmd"
  }
}

Require-Cmd java
Require-Cmd ant
Require-Cmd docker

if (-not $env:CATALINA_HOME) {
  Fail "CATALINA_HOME is not set. Example: setx CATALINA_HOME C:\\tools\\apache-tomcat-10.1.x"
}
if (-not (Test-Path $env:CATALINA_HOME)) {
  Fail "CATALINA_HOME path not found: $($env:CATALINA_HOME)"
}
if (-not (Test-Path "$($env:CATALINA_HOME)\\bin\\startup.bat")) {
  Fail "Tomcat startup.bat not found in CATALINA_HOME\\bin"
}

$javaVersionOutput = & java -version 2>&1
if ($javaVersionOutput -notmatch 'version\s+"(\d+)') {
  Fail "Cannot parse java version"
}
$javaMajor = [int]$Matches[1]
if ($javaMajor -lt 17) {
  Fail "Java 17+ required. Found: $javaMajor"
}

try {
  docker info | Out-Null
} catch {
  Fail "Docker daemon not reachable. Start Docker Desktop first."
}

$jdbcJars = Get-ChildItem -Path "lib" -Filter "ojdbc.jar" -ErrorAction SilentlyContinue
if (-not $jdbcJars) {
  Fail "Oracle JDBC jar missing. Put ojdbc jar in lib/ (e.g., lib/ojdbc.jar)."
}

if (Test-Path "docker-compose.yml") {
  Write-Host "[EduFlow] Starting Oracle XE via docker compose"
  docker compose up -d | Out-Null
} else {
  $existing = docker ps -a --format "{{.Names}}" | Select-String -Pattern "^$DbContainer$"
  if (-not $existing) {
    Write-Host "[EduFlow] Creating Oracle XE container"
    docker run -d --name $DbContainer -p 1521:1521 -p 5500:5500 -e ORACLE_PASSWORD=$DbPassword $DbImage | Out-Null
  } else {
    Write-Host "[EduFlow] Starting existing Oracle XE container"
    docker start $DbContainer | Out-Null
  }
}

Write-Host "[EduFlow] Waiting for Oracle XE readiness"
$ready = $false
for ($i = 0; $i -lt 120; $i++) {
  $logs = docker logs $DbContainer 2>&1
  if ($logs -match "DATABASE IS READY TO USE") {
    $ready = $true
    break
  }
  Start-Sleep -Seconds 2
}
if (-not $ready) {
  Fail "Oracle XE did not become ready in time."
}

Write-Host "[EduFlow] Applying schema and seed"
docker cp "database/schema.sql" "$DbContainer`:/tmp/schema.sql"
docker cp "database/seed.sql" "$DbContainer`:/tmp/seed.sql"
docker exec -i $DbContainer bash -lc "sqlplus -s $DbConn <<'SQL'
@/tmp/schema.sql
@/tmp/seed.sql
COMMIT;
EXIT;
SQL" | Out-Null

Write-Host "[EduFlow] Building WAR"
ant clean dist | Out-Null
if (-not (Test-Path $WarPath)) {
  Fail "WAR not generated at $WarPath"
}

Write-Host "[EduFlow] Deploying WAR to Tomcat"
Copy-Item $WarPath "$($env:CATALINA_HOME)\\webapps\\EduFlow.war" -Force
& "$($env:CATALINA_HOME)\\bin\\shutdown.bat" | Out-Null
& "$($env:CATALINA_HOME)\\bin\\startup.bat" | Out-Null

Write-Host "[EduFlow] Setup complete" -ForegroundColor Green
Write-Host "Open: http://localhost:8080/EduFlow/login.jsp"
Write-Host "Admin: admin@demo.com / admin123"
Write-Host "Teacher: teacher@demo.com / teacher123"
Write-Host "Student: student@demo.com / student123"
