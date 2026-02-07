# EduFlow

EduFlow is a role-based academic routine management system built with JSP, Servlets, Oracle DB, and Apache Ant.

## Quick Start (5 Steps)

Use this if your machine already has Java, Ant, Docker, and Tomcat 10.

1. Clone:
```bash
git clone <YOUR_GITHUB_REPO_URL>
cd EduFlow
```
2. Put Oracle JDBC jar in `lib/` (example: `lib/ojdbc.jar`).
3. Start Oracle XE:
```bash
docker compose up -d
docker logs -f oraclexe
```
4. Initialize DB:
```bash
docker cp database/schema.sql oraclexe:/tmp/schema.sql
docker cp database/seed.sql oraclexe:/tmp/seed.sql
docker exec -it oraclexe bash -lc "sqlplus system/1234@//localhost:1521/XEPDB1 <<'SQL'
@/tmp/schema.sql
@/tmp/seed.sql
COMMIT;
EXIT;
SQL"
```
5. Build + deploy:
```bash
ant clean dist
cp dist/EduFlow.war "$CATALINA_HOME/webapps/"
"$CATALINA_HOME/bin/shutdown.sh" || true
"$CATALINA_HOME/bin/startup.sh"
```
Open: `http://localhost:8080/EduFlow/login.jsp`

## One-Command Setup Scripts

If you prefer automation, run:

### Linux/macOS

```bash
chmod +x scripts/setup.sh
./scripts/setup.sh
```

### Windows (PowerShell)

```powershell
powershell -ExecutionPolicy Bypass -File .\\scripts\\setup.ps1
```

What these scripts do:

- validate Java/Ant/Docker/Tomcat config
- start Oracle XE container
- apply `database/schema.sql` and `database/seed.sql`
- build `dist/EduFlow.war`
- deploy WAR to Tomcat and restart it

Script requirements:

- `CATALINA_HOME` must already be set
- `lib/ojdbc.jar` must exist

## What This Project Aims For

EduFlow helps institutions manage class routines with controlled workflow.

- Students see approved routines in a day-based weekly UI.
- Teachers submit routine change requests.
- Admin approves or rejects requests.
- System blocks schedule conflicts:
- student overlap
- room clash
- teacher double-booking
- Admin can mark weekdays as `WORKING`, `WEEKEND`, or `HOLIDAY`.
- Announcements are visible to students and teachers.

## Tech Stack

- Java 17+
- JSP / Servlets (Jakarta, Tomcat 10)
- Oracle XE (Docker)
- Apache Ant
- Tailwind CSS CDN
- Vanilla JavaScript

## Project Structure

```text
EduFlow/
├── build.xml
├── config.properties
├── database/
│   ├── schema.sql
│   └── seed.sql
├── lib/
│   └── ojdbc jar (not committed)
├── src/com/eduflow/
│   ├── dao/
│   ├── model/
│   ├── servlet/
│   └── util/
└── web/
    ├── *.jsp
    └── WEB-INF/web.xml
```

## 0. Clone Repository

```bash
git clone <YOUR_GITHUB_REPO_URL>
cd EduFlow
```

## 1. Machine Setup From Zero

Choose your OS section and complete it first.

### Windows (PowerShell)

Install tools:

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
winget install Apache.Ant
winget install Docker.DockerDesktop
```

Tomcat 10:

1. Download zip from Apache Tomcat 10 official site.
2. Extract to a path like `C:\tools\apache-tomcat-10.1.x`.

Set `CATALINA_HOME`:

```powershell
setx CATALINA_HOME "C:\tools\apache-tomcat-10.1.x"
```

Restart terminal after `setx`.

Verify:

```powershell
java -version
ant -version
docker --version
$env:CATALINA_HOME
```

### macOS (Terminal)

Install tools with Homebrew:

```bash
brew update
brew install openjdk@21 ant docker
```

Install Docker Desktop from official app if docker daemon is unavailable from CLI.

Tomcat 10:

```bash
brew install tomcat
```

Set `CATALINA_HOME`:

```bash
echo 'export CATALINA_HOME="$(brew --prefix tomcat)/libexec"' >> ~/.zshrc
source ~/.zshrc
```

Verify:

```bash
java -version
ant -version
docker --version
echo $CATALINA_HOME
```

### Linux (Ubuntu/Debian)

Install tools:

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk ant docker.io
```

Enable docker:

```bash
sudo systemctl enable --now docker
sudo usermod -aG docker $USER
```

Log out and log in again.

Tomcat 10:

1. Download Tomcat 10 tar.gz from Apache.
2. Extract to `~/tomcat` (or any path you prefer).

Set `CATALINA_HOME`:

```bash
echo 'export CATALINA_HOME="$HOME/tomcat"' >> ~/.bashrc
source ~/.bashrc
```

Verify:

```bash
java -version
ant -version
docker --version
echo $CATALINA_HOME
```

## 2. Add Oracle JDBC Driver

1. Download Oracle JDBC driver jar from Oracle (for Java 17+, usually `ojdbc17.jar`).
2. Rename/copy it as `ojdbc.jar` and put it in project `lib/`.

Example:

```text
EduFlow/lib/ojdbc.jar
```

The Ant build includes all jars from `lib/`.

## 3. Start Oracle XE with Docker

Run:

```bash
docker run -d --name oraclexe \
  -p 1521:1521 -p 5500:5500 \
  -e ORACLE_PASSWORD=1234 \
  gvenzl/oracle-xe:21-slim
```

Or use the included compose file:

```bash
docker compose up -d
```

Watch logs until ready:

```bash
docker logs -f oraclexe
```

Wait for `DATABASE IS READY TO USE!`.

## 4. Configure App DB Settings

File: `config.properties`

```properties
db.url=jdbc:oracle:thin:@localhost:1521/XEPDB1
db.user=system
db.password=1234
```

## 5. Initialize Database

Copy SQL files into container:

```bash
docker cp database/schema.sql oraclexe:/tmp/schema.sql
docker cp database/seed.sql oraclexe:/tmp/seed.sql
```

Run SQL:

```bash
docker exec -it oraclexe bash -lc "sqlplus system/1234@//localhost:1521/XEPDB1 <<'SQL'
@/tmp/schema.sql
@/tmp/seed.sql
COMMIT;
EXIT;
SQL"
```

## 6. Build WAR

```bash
ant clean dist
```

Expected output file:

```text
dist/EduFlow.war
```

## 7. Deploy to Tomcat 10

### Windows (PowerShell)

```powershell
Copy-Item dist\EduFlow.war "$env:CATALINA_HOME\webapps\EduFlow.war" -Force
& "$env:CATALINA_HOME\bin\shutdown.bat"
& "$env:CATALINA_HOME\bin\startup.bat"
```

### macOS/Linux

```bash
cp dist/EduFlow.war "$CATALINA_HOME/webapps/"
"$CATALINA_HOME/bin/shutdown.sh" || true
"$CATALINA_HOME/bin/startup.sh"
```

## 8. Open Application

```text
http://localhost:8080/EduFlow/login.jsp
```

## Demo Credentials

- Admin: `admin@demo.com` / `admin123`
- Teacher: `teacher@demo.com` / `teacher123`
- Student: `student@demo.com` / `student123`

## Clean Re-Initialization (Optional)

If DB gets inconsistent, recreate container and rerun setup.

```bash
docker rm -f oraclexe
docker run -d --name oraclexe -p 1521:1521 -p 5500:5500 -e ORACLE_PASSWORD=1234 gvenzl/oracle-xe:21-slim
```

Then repeat Section 5 onward.

## Troubleshooting

- `ORA-12541`: Oracle container not running.
- `ORA-00942`: schema/seed not loaded.
- Servlet compile errors for `javax.*`: use Tomcat 10 compatible code and correct servlet API.
- Docker permission denied on Linux: add user to docker group and log in again.

## Conflict Rule Used

```text
new_start < existing_end AND new_end > existing_start
```

## License

Add your preferred license before publishing publicly.
