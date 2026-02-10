$ErrorActionPreference = 'Stop'

$RootDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $RootDir

function Log($msg) {
  Write-Host "`n[EduFlow Launcher] $msg"
}

function Fail($msg) {
  Write-Host "`n[EduFlow Launcher][ERROR] $msg" -ForegroundColor Red
  exit 1
}

function Require-Cmd($cmd) {
  if (-not (Get-Command $cmd -ErrorAction SilentlyContinue)) {
    Fail "Missing required command: $cmd"
  }
}

function Detect-TomcatHome {
  if ($env:CATALINA_HOME -and (Test-Path "$($env:CATALINA_HOME)\\bin\\startup.bat")) {
    return $true
  }

  $candidates = @(
    "$env:USERPROFILE\\tomcat",
    "$env:USERPROFILE\\apache-tomcat-10.1.0",
    'C:\\tomcat',
    'C:\\Program Files\\Apache Software Foundation\\Tomcat 10.1'
  )

  foreach ($c in $candidates) {
    if (Test-Path "$c\\bin\\startup.bat") {
      $env:CATALINA_HOME = $c
      return $true
    }
  }

  return $false
}

Require-Cmd powershell
Require-Cmd java
Require-Cmd ant
Require-Cmd docker

if (-not (Test-Path 'scripts/setup.ps1')) {
  Fail 'scripts/setup.ps1 not found.'
}

if (-not (Test-Path 'lib/ojdbc.jar')) {
  Fail 'Missing lib/ojdbc.jar. Add Oracle JDBC jar first.'
}

if (-not (Detect-TomcatHome)) {
  Fail 'CATALINA_HOME is not set and Tomcat could not be auto-detected. Set CATALINA_HOME and retry.'
}

Log "Using CATALINA_HOME=$($env:CATALINA_HOME)"

try {
  docker info | Out-Null
} catch {
  Log 'Docker daemon may be stopped. Trying to start com.docker.service...'
  try {
    Start-Service com.docker.service -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 3
  } catch {
    # ignore and let setup.ps1 fail with a clear error if still unavailable
  }
}

Log 'Running full setup/deploy pipeline'
& powershell -NoProfile -ExecutionPolicy Bypass -File '.\\scripts\\setup.ps1'
if ($LASTEXITCODE -ne 0) {
  Fail 'Setup pipeline failed.'
}

Log 'Done. Open: http://localhost:8080/EduFlow/login.jsp'
