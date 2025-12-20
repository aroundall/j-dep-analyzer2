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

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | Dashboard with upload + graph |
| `/dependencies/list` | GET | Dependencies pair list |
| `/visualize/{id}` | GET | Detail view for artifact |
| `/export` | GET | Export page |
| `/api/upload` | POST | Upload POM files |
| `/api/artifacts` | GET | List artifacts (JSON) |
| `/api/graph/data` | GET | Graph data (Cytoscape.js format) |
| `/export/{table}.csv` | GET | Download table as CSV |

## Development

```powershell
# Build
.\mvnw clean package

# Run tests
.\mvnw test

# Run with dev mode
.\mvnw spring-boot:run
```

## Migration from Python Version

This is a complete rewrite of [j-dep-analyzer](../README.md) using:

| Python | Java |
|--------|------|
| FastAPI | Spring Boot 3.2 |
| SQLModel | Spring Data JPA |
| Jinja2 | Thymeleaf |
| NetworkX | JGraphT |
| pg8000 + cloud-sql-python-connector | JDBC + postgres-socket-factory |
| Alembic | Flyway |
| lxml | Java DOM/XPath |
