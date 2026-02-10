$ErrorActionPreference = 'Stop'

$RootDir = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $RootDir

$DbContainer = 'oraclexe'
$DbImage = 'gvenzl/oracle-xe:21-slim'
$DbPassword = '1234'
$DbConn = "system/$DbPassword@//localhost:1521/XEPDB1"
$WarPath = 'dist/EduFlow.war'
$ForceDbInit = if ($env:FORCE_DB_INIT) { $env:FORCE_DB_INIT } else { '0' }
$UsedElevatedTomcat = $false

function Log($msg) {
  Write-Host "`n[EduFlow] $msg"
}

function Fail($msg) {
  Write-Host "`n[EduFlow][ERROR] $msg" -ForegroundColor Red
  exit 1
}

function Require-Cmd($cmd) {
  if (-not (Get-Command $cmd -ErrorAction SilentlyContinue)) {
    Fail "Missing required command: $cmd"
  }
}

function Invoke-ElevatedPowerShell($command) {
  $proc = Start-Process -FilePath 'powershell' `
    -ArgumentList @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-Command', $command) `
    -Verb RunAs -Wait -PassThru
  return $proc.ExitCode
}

function Get-ComposeCommand {
  try {
    docker compose version | Out-Null
    return @('docker', 'compose')
  } catch {
    if (Get-Command 'docker-compose' -ErrorAction SilentlyContinue) {
      return @('docker-compose')
    }
  }
  return @()
}

function Invoke-ComposeUp($composeCmd) {
  if ($composeCmd.Count -eq 2 -and $composeCmd[0] -eq 'docker' -and $composeCmd[1] -eq 'compose') {
    & docker compose up -d | Out-Null
  } elseif ($composeCmd.Count -eq 1 -and $composeCmd[0] -eq 'docker-compose') {
    & docker-compose up -d | Out-Null
  } else {
    Fail 'Invalid compose command configuration.'
  }
}

function Test-OracleReady {
  try {
    $null = docker exec -i $DbContainer bash -lc "echo 'EXIT;' | sqlplus -s $DbConn" 2>$null
    return $true
  } catch {
    return $false
  }
}

function Get-TableExists($tableName) {
  try {
    $out = docker exec -i $DbContainer bash -lc "sqlplus -s $DbConn <<'SQL'
SET HEADING OFF FEEDBACK OFF PAGESIZE 0 VERIFY OFF ECHO OFF
SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME='${tableName}';
EXIT;
SQL"
    $trimmed = ($out | Out-String).Trim()
    return $trimmed -eq '1'
  } catch {
    return $false
  }
}

Require-Cmd java
Require-Cmd ant
Require-Cmd docker

if (-not $env:CATALINA_HOME) {
  Fail 'CATALINA_HOME is not set. Example: setx CATALINA_HOME C:\\tools\\apache-tomcat-10.1.x'
}
if (-not (Test-Path $env:CATALINA_HOME)) {
  Fail "CATALINA_HOME path not found: $($env:CATALINA_HOME)"
}
if (-not (Test-Path "$($env:CATALINA_HOME)\\bin\\startup.bat")) {
  Fail 'Tomcat startup.bat not found in CATALINA_HOME\\bin'
}

$javaVersionOutput = (& java -version 2>&1 | Out-String)
if ($javaVersionOutput -notmatch 'version\s+"(\d+)') {
  Fail 'Cannot parse java version'
}
$javaMajor = [int]$Matches[1]
if ($javaMajor -lt 17) {
  Fail "Java 17+ required. Found: $javaMajor"
}

try {
  docker info | Out-Null
} catch {
  Fail 'Docker daemon not reachable. Start Docker Desktop first.'
}

$jdbcJar = Join-Path (Get-Location) 'lib/ojdbc.jar'
if (-not (Test-Path $jdbcJar)) {
  Fail 'Oracle JDBC jar missing. Put ojdbc.jar in lib/.'
}

$composeCmd = @()
if (Test-Path 'docker-compose.yml') {
  $composeCmd = Get-ComposeCommand
}

if ((Test-Path 'docker-compose.yml') -and $composeCmd.Count -gt 0) {
  Log 'Starting Oracle XE via docker compose'
  Invoke-ComposeUp $composeCmd
} else {
  $existing = docker ps -a --format '{{.Names}}' | Select-String -Pattern "^$DbContainer$"
  if (-not $existing) {
    Log 'Creating Oracle XE container'
    docker run -d --name $DbContainer -p 1521:1521 -p 5500:5500 -e ORACLE_PASSWORD=$DbPassword $DbImage | Out-Null
  } else {
    Log 'Starting existing Oracle XE container'
    docker start $DbContainer | Out-Null
  }
}

Log 'Waiting for Oracle XE readiness'
$ready = $false
for ($i = 0; $i -lt 180; $i++) {
  $running = docker ps --format '{{.Names}}' | Select-String -Pattern "^$DbContainer$"
  if ($running -and (Test-OracleReady)) {
    $ready = $true
    break
  }
  Start-Sleep -Seconds 2
}
if (-not $ready) {
  Fail "Oracle XE did not become ready in time. Check: docker logs $DbContainer"
}

$dbHasSchema = Get-TableExists 'USERS'
if ($ForceDbInit -eq '1' -or -not $dbHasSchema) {
  Log 'Applying schema and seed'
  docker cp 'database/schema.sql' "$DbContainer`:/tmp/schema.sql"
  docker cp 'database/seed.sql' "$DbContainer`:/tmp/seed.sql"
  docker exec -i $DbContainer bash -lc "sqlplus -s $DbConn <<'SQL'
@/tmp/schema.sql
@/tmp/seed.sql
COMMIT;
EXIT;
SQL" | Out-Null
} else {
  Log 'Schema already exists; skipping schema/seed apply. Set FORCE_DB_INIT=1 to re-apply.'
}

Log 'Building WAR'
ant clean dist | Out-Null
if (-not (Test-Path $WarPath)) {
  Fail "WAR not generated at $WarPath"
}

Log 'Deploying WAR to Tomcat'
$targetWar = "$($env:CATALINA_HOME)\\webapps\\EduFlow.war"
try {
  Copy-Item $WarPath $targetWar -Force
} catch {
  Log 'Normal deploy failed; retrying WAR copy with elevated permission'
  $copyCmd = "Copy-Item -Path '$((Resolve-Path $WarPath).Path)' -Destination '$targetWar' -Force"
  $code = Invoke-ElevatedPowerShell $copyCmd
  if ($code -ne 0) {
    Fail 'Tomcat deploy failed due to permissions.'
  }
  $UsedElevatedTomcat = $true
}

if ($UsedElevatedTomcat) {
  $shutdownCmd = "& '$($env:CATALINA_HOME)\\bin\\shutdown.bat' | Out-Null"
  $startupCmd = "& '$($env:CATALINA_HOME)\\bin\\startup.bat' | Out-Null"
  [void](Invoke-ElevatedPowerShell $shutdownCmd)
  $startCode = Invoke-ElevatedPowerShell $startupCmd
  if ($startCode -ne 0) {
    Fail 'Tomcat startup failed after elevated deploy.'
  }
} else {
  try {
    & "$($env:CATALINA_HOME)\\bin\\shutdown.bat" | Out-Null
  } catch {
    # ignore shutdown failures if Tomcat is not running
  }
  & "$($env:CATALINA_HOME)\\bin\\startup.bat" | Out-Null
}

Log 'Setup complete'
Write-Host 'Open: http://localhost:8080/EduFlow/login.jsp'
Write-Host 'Admin: admin@demo.com / admin123'
Write-Host 'Teacher: teacher@demo.com / teacher123'
Write-Host 'Student: student@demo.com / student123'
