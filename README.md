# J-Dep Analyzer 2

Maven dependency analyzer - Java + Spring Boot edition (migrated from Python/FastAPI).

## Requirements

- Java 17+
- Maven 3.8+

## Quick Start

### Run with SQLite (default)

```powershell
# Windows
.\mvnw spring-boot:run

# Linux/macOS
./mvnw spring-boot:run
```

Open: http://localhost:8080

### Run with CloudSQL PostgreSQL

```powershell
$env:SPRING_PROFILES_ACTIVE = "postgresql"
$env:JDEP_DB_HOST = "your-project:region:instance"
$env:JDEP_DB_NAME = "jdep"
$env:JDEP_DB_USER = "sa-name@project.iam"
$env:GOOGLE_APPLICATION_CREDENTIALS = "C:\path\to\service-account.json"

.\mvnw spring-boot:run
```

## Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | `sqlite` or `postgresql` | sqlite |
| `JDEP_DB_HOST` | CloudSQL instance connection name | - |
| `JDEP_DB_NAME` | Database name | jdep |
| `JDEP_DB_USER` | Database user | - |
| `GOOGLE_APPLICATION_CREDENTIALS` | Path to GCP service account JSON | - |

## Development

```powershell
# Build
.\mvnw clean package

# Run tests
.\mvnw test

# Run API integration tests
.\mvnw test -Dtest="com.jdepanalyzer.api.*"

# Run with dev mode
.\mvnw spring-boot:run
```
