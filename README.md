# Career Arc

Career Arc is a local-first career guidance and roadmap portal for students, mentors, and administrators. It combines a student profile, a short interest and aptitude reflection, current skills, and career preferences into explainable Java-based role matches. Selected roles drive skill-gap comparisons and an individualized learning roadmap.

The assessment and recommendation scores are project-level guidance aids, not scientifically validated psychological evaluations or employment predictions. Learning catalog entries included with the demo are illustrative project data.

## Technology

- Java 21
- Spring Boot 3.5, Spring Web, Spring Security OAuth2 Resource Server (JWT), Spring Data JPA / Hibernate, Jakarta Validation
- Maven
- MySQL 8.x
- HTML5, CSS3, and vanilla JavaScript using the Fetch API

The app serves its frontend and REST API from one Spring Boot process at `http://127.0.0.1:8080`. It has no cloud, external AI, CDN, or third-party API dependency.

## Prerequisites

- JDK 21. Verify with `java -version`.
- Maven 3.9 or later. Verify with `mvn -version`.
- MySQL Server 8.x running locally.
- A modern browser.

## MySQL Setup (Windows)

If MySQL is not installed, install MySQL Community Server 8.x using the official [MySQL Installer for Windows](https://dev.mysql.com/downloads/installer/). Select MySQL Server, configure it as a Windows service (the standard service name is `MySQL80`), retain the local default port `3306`, and set an administrator password during setup. Keep that password private; Career Arc should connect as the dedicated database user created below.

Start the local MySQL service from an elevated PowerShell window if it is stopped:

```powershell
Get-Service MySQL80
Start-Service MySQL80
```

Connect as a MySQL administrator using MySQL Workbench or the installed MySQL Shell/client, then create the local database and a dedicated application user. Choose a private local password and use the same value in `DB_PASSWORD` below:

```sql
CREATE DATABASE career_arc_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'career_arc'@'localhost' IDENTIFIED BY 'ChangeThisLocalDbPassword!';
GRANT ALL PRIVILEGES ON career_arc_db.* TO 'career_arc'@'localhost';
FLUSH PRIVILEGES;
```

The example password is a local placeholder. Do not reuse it outside a private development database. If the database already exists, create or update only the local user and grants.

## Configure and Run

Configure the database credentials and a persistent JWT signing secret as described below, then from the project root run:

```powershell
mvn spring-boot:run
```

Open `http://127.0.0.1:8080`. Spring Boot creates or updates the normalized schema through Hibernate and initializes the local demo catalog when the database is empty. The initializer is idempotent; it does not duplicate demo accounts, skills, career roles, resources, recommendations, or the seeded roadmap on restart.

Configuration can be overridden with environment variables:

- `DB_URL` defaults to `jdbc:mysql://127.0.0.1:3306/career_arc_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`.
- `DB_USERNAME` defaults to `root`.
- `DB_PASSWORD` defaults to empty.
- `JWT_SECRET` is required. It must be Base64 encoding of at least 32 random bytes; keep it private and stable between app restarts.
- `JWT_COOKIE_SECURE` defaults to `false` for local HTTP development. Set it to `true` when serving the app over HTTPS.
- `PORT` defaults to `8080`.
- `APP_DEMO_SEED` defaults to `true`. Set it to `false` to disable local presentation accounts and sample data.
- Matching weights default to skills `55`, assessment `25`, interests `15`, and academic context `5`. Override with `CAREER_MATCH_SKILL_WEIGHT`, `CAREER_MATCH_ASSESSMENT_WEIGHT`, `CAREER_MATCH_INTEREST_WEIGHT`, and `CAREER_MATCH_ACADEMIC_WEIGHT` for local experiments.

Credentials and signing keys are not stored in source files. Stop the server with `Ctrl+C`.

### Persist local settings on Windows

Save the database credentials and a randomly generated signing key as environment variables for your Windows user. Open PowerShell as the same Windows user that runs Career Arc and enter:

```powershell
[Environment]::SetEnvironmentVariable("DB_USERNAME", "career_arc", "User")
$securePassword = Read-Host "Enter the career_arc MySQL password" -AsSecureString
$password = [System.Net.NetworkCredential]::new("", $securePassword).Password
[Environment]::SetEnvironmentVariable("DB_PASSWORD", $password, "User")
$env:DB_USERNAME = "career_arc"
$env:DB_PASSWORD = $password
$jwtSecret = [Environment]::GetEnvironmentVariable("JWT_SECRET", "User")
if ([string]::IsNullOrWhiteSpace($jwtSecret)) {
    $randomBytes = New-Object byte[] 32
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($randomBytes)
    $jwtSecret = [Convert]::ToBase64String($randomBytes)
    [Environment]::SetEnvironmentVariable("JWT_SECRET", $jwtSecret, "User")
    $rng.Dispose()
}
$env:JWT_SECRET = $jwtSecret
Remove-Variable password, securePassword, jwtSecret, randomBytes, rng -ErrorAction SilentlyContinue
```

The password and key are saved in your Windows user environment settings, not in the project. Windows environment variables are not encrypted secrets, so use this only on a trusted, private computer. The command creates the signing key only if one is not already saved; keep it stable because changing it invalidates access tokens. `$env:JWT_SECRET` makes it available immediately in the current terminal. For future terminals, fully restart VS Code if its terminal does not inherit the newly saved user variable. Then run `mvn spring-boot:run` from the project directory.

## Local Demo Accounts

These accounts are seeded for local presentation only. They use known demo passwords and must not be exposed on a shared or public system.

| Role | Email | Password |
| --- | --- | --- |
| Student | `student@careerarc.local` | `CareerArcDemo123!` |
| Mentor | `mentor@careerarc.local` | `CareerArcMentor123!` |
| Administrator | `admin@careerarc.local` | `CareerArcAdmin123!` |

The seeded student has an academic profile, stored assessment, skills, four career roles, persisted recommendations, a selected Software Engineer target, skill-gap records, local learning resources, an assigned mentor, and roadmap milestones in completed, in-progress, and not-started states.

## Application Modules

- **Student:** profile management, assessment and history, technical/soft skills, ranked career matches with reasons, target selection, skill-gap analysis, roadmap generation and milestone progress, resource filtering, dashboard, and mentor feedback.
- **Mentor:** assigned-student list, profile and progress review, assessment/recommendation/gap/roadmap summaries, feedback, and alternative career/resource suggestions.
- **Administrator:** account and role management, explicit mentor assignments, career-role and required-skill management, skill catalog and prerequisites, learning resource management, and database-backed aggregate reports.

Authentication uses BCrypt-hashed passwords, 15-minute HS256 JWT access tokens, and seven-day HttpOnly/SameSite=Strict refresh cookies rotated on use. Refresh-token hashes are stored in MySQL; token reuse revokes its token family. Access tokens remain in browser memory only, and backend role authorization applies to every protected route. Mentor access additionally checks a persisted mentor-to-student assignment.

## REST API Overview

- Authentication: `/api/auth` (`register`, `login`, `me`, `refresh`, `logout`)
- Student workflow: `/api/student` (dashboard, profile, assessment, skills, recommendations, target role, gaps, roadmap, milestones, feedback)
- Read-only catalogs: `/api/catalog` (career roles, skills, filtered learning resources)
- Mentor: `/api/mentor` (assigned students, reviews, feedback)
- Administrator: `/api/admin` (users, assignments, roles, role requirements, skills, resources, reports)

All API endpoints are same-origin. Student records are scoped to the authenticated student, and mentor record access is checked against explicit assignments in the service layer.

## Build and Tests

```powershell
mvn test
mvn package
```

Tests cover weighted recommendation scores and explanations, proficiency gap states, prerequisite ordering, progress calculation, input validation, H2-backed startup seeding idempotence, demo login and dashboard data, and backend role restrictions. H2 is test-scoped only; the running application uses MySQL.

## Reset Demo Data

Stop the app first. In MySQL Workbench or another local MySQL client, run:

```sql
DROP DATABASE career_arc_db;
CREATE DATABASE career_arc_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Start Career Arc again. Hibernate recreates the schema and the local demo initializer restores the sample records. The MySQL application user must retain its database privileges.

## Troubleshooting

- **Connection refused:** confirm `MySQL80` is running and that the host/port in `DB_URL` match the local server.
- **Access denied:** check `DB_USERNAME`, `DB_PASSWORD`, the MySQL account host (`localhost`), and grants on `career_arc_db`.
- **Student demo login shows “You do not have permission”:** stop the app, confirm `APP_DEMO_SEED` is not `false`, then restart it so the initializer can restore the demo account role. Sign out and sign back in as `student@careerarc.local` afterward. If the issue remains, check the `user_role` for that email in the `app_user` table; it should be `STUDENT`.
- **Unknown database:** create `career_arc_db` using the SQL above, or use a database user allowed to create it.
- **Port 8080 is busy:** set a different port for the current PowerShell session, for example `$env:PORT = "8081"`, and open `http://127.0.0.1:8081`.
- **Demo data does not appear:** confirm `APP_DEMO_SEED` is not set to `false`, check the application startup log, and verify that Hibernate can create/update the schema.
- **Maven is not recognized:** install Maven or add its `bin` directory to `PATH`, then open a new terminal.

## Project Layout

```text
src/main/java/local/careerarc/       Spring Boot application, entities, repositories, services, controllers
src/main/resources/application.properties
src/main/resources/static/            Same-origin HTML, CSS, and vanilla JavaScript
src/test/java/local/careerarc/       Business-rule and H2-backed application tests
pom.xml
```