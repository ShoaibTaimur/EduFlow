# EduFlow

EduFlow is a role-based academic routine management system built with JSP, Servlets, Oracle DB, Apache Ant, and Tomcat.

## Project Goal

EduFlow manages academic class routines with controlled workflow:

- Students can view approved schedules.
- Teachers can create new class requests and submit edits for existing classes.
- Admin reviews requests, approves/rejects them, and can also edit schedules directly.
- Conflict rules prevent overlapping classes, room clashes, and teacher double-booking.

## Feature Highlights

- Role-based authentication (`admin`, `teacher`, `student`).
- Teacher request workflow: create new schedule requests and update requests for existing assigned classes.
- Admin approval workflow: approve/reject teacher requests, including update requests (`requestType=UPDATE` + `scheduleId`).
- Admin direct editing for existing approved schedules.
- Conflict checks on create/update: student conflict, room clash, teacher availability.
- Day policy management: mark `WORKING`, `WEEKEND`, `HOLIDAY`.
- Auto cleanup rule: setting a day to `WEEKEND`/`HOLIDAY` deletes approved classes on that day.
- Announcements: admin and teacher can publish; teacher/student can view live announcements.
- Admin Data Browser page to inspect table data in UI.

## Tech Stack

- Java 17+ (project compiles with source/target 17)
- JSP + Servlets (Jakarta namespace, Tomcat 10)
- Oracle XE (Docker)
- Apache Ant (`build.xml`)
- Tailwind CSS CDN + Vanilla JavaScript

## Project Structure

```text
EduFlow/
├── build.xml
├── config.properties
├── database/
│   ├── schema.sql
│   └── seed.sql
├── lib/
│   └── ojdbc.jar   (you add this manually)
├── scripts/
│   ├── setup.sh
│   └── setup.ps1
├── src/com/eduflow/
│   ├── dao/
│   ├── model/
│   ├── servlet/
│   └── util/
└── web/
    ├── *.jsp
    └── WEB-INF/web.xml
```

## Prerequisites (All OS)

Install these first:

- Java 17+ (JDK)
- Apache Ant
- Docker (daemon running)
- Apache Tomcat 10
- Oracle JDBC jar named exactly: `lib/ojdbc.jar`

Important:

- This project expects `ojdbc.jar` (not `ojdbc17.jar`) in `lib/`.
- Set `CATALINA_HOME` to your Tomcat 10 directory.

## Start From Zero (Manual)

### 1. Clone

```bash
git clone <YOUR_GITHUB_REPO_URL>
cd EduFlow
```

### 2. Add JDBC Driver

Place Oracle JDBC jar here:

```text
EduFlow/lib/ojdbc.jar
```

### 3. Start Oracle XE (Docker)

If `docker-compose.yml` exists:

```bash
docker compose up -d
```

Or direct run:

```bash
docker run -d --name oraclexe \
  -p 1521:1521 -p 5500:5500 \
  -e ORACLE_PASSWORD=1234 \
  gvenzl/oracle-xe:21-slim
```

Check readiness:

```bash
docker logs -f oraclexe
```

Wait for: `DATABASE IS READY TO USE!`

### 4. Configure DB Connection

`config.properties`:

```properties
db.url=jdbc:oracle:thin:@localhost:1521/XEPDB1
db.user=system
db.password=1234
```

### 5. Load Schema + Seed Data

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

### 6. Build + Deploy

```bash
ant clean dist
cp dist/EduFlow.war "$CATALINA_HOME/webapps/"
"$CATALINA_HOME/bin/shutdown.sh" || true
"$CATALINA_HOME/bin/startup.sh"
```

Open:

```text
http://localhost:8080/EduFlow/login.jsp
```

## One-Command Setup (Recommended)

If prerequisites are already installed and `CATALINA_HOME` is set, use:

### Linux/macOS

```bash
chmod +x scripts/setup.sh
./scripts/setup.sh
```

### Windows (PowerShell)

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup.ps1
```

These scripts will:

- start Oracle XE container
- wait for DB readiness
- load schema and seed SQL
- build WAR
- deploy WAR to Tomcat
- restart Tomcat

## Demo Accounts

- Admin: `admin@demo.com` / `admin123`
- Teacher: `teacher@demo.com` / `teacher123`
- Student: `student@demo.com` / `student123`

## Daily Re-Run After Restart

When you reopen your laptop:

1. Start Docker daemon/Desktop.
2. Start Oracle container:
   - `docker start oraclexe` (or `docker compose up -d`)
3. Start Tomcat:
   - `"$CATALINA_HOME/bin/startup.sh"` (Windows: `startup.bat`)
4. Open `http://localhost:8080/EduFlow/login.jsp`

If you changed code, redeploy first:

```bash
ant clean dist
cp dist/EduFlow.war "$CATALINA_HOME/webapps/"
"$CATALINA_HOME/bin/shutdown.sh" || true
"$CATALINA_HOME/bin/startup.sh"
```

## Conflict Rule

```text
new_start < existing_end AND new_end > existing_start
```

## Troubleshooting

- `ORA-12541`: Oracle is not running or port `1521` is not available.
- `ORA-00942`: run schema/seed loading again.
- Docker permission denied on Linux: run `sudo usermod -aG docker $USER`, then logout/login.
- Tomcat deployment permission errors: use `sudo` for deploy commands if Tomcat directory is root-owned.

## Can This Be Fully One-Click Across Windows/macOS/Linux?

Short answer: partially yes, fully automatic install of all missing tools is not reliable/safe in one universal click.

What is possible now:

- one command setup from this repo using `scripts/setup.sh` or `scripts/setup.ps1`
- automatic DB start, data load, build, deploy

What is not guaranteed in a single cross-platform click:

- automatic installation of Java/Ant/Docker/Tomcat/Oracle JDBC without admin prompts
- handling all OS security policies and package manager differences transparently

If you want, a next step can be adding OS-specific bootstrap installers:

- Windows bootstrap `.ps1`
- macOS bootstrap `.sh` with Homebrew
- Linux bootstrap `.sh` for apt-based systems

## License

This project is licensed under the license defined in this GitHub repository.
