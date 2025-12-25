# J-Dep Analyzer 2

Maven dependency analyzer with interactive graph visualization.

**Tech Stack**: Spring Boot + React (TypeScript) + Cytoscape.js + TailwindCSS

## Requirements

- Java 17+
- Node.js 18+ (for frontend development)
- Maven 3.8+

## Quick Start

### Development Mode (Recommended)

Run both servers for hot-reload development:

```bash
# Terminal 1: Start Spring Boot backend (port 8080)
mvn spring-boot:run

# Terminal 2: Start React frontend (port 5173)
cd frontend
npm install
npm run dev
```

Open: http://localhost:5173

The Vite dev server proxies `/api/*` requests to Spring Boot automatically.

### Production Mode

Build and run as a single JAR:

```bash
# 1. Build React frontend
cd frontend
npm install
npm run build

# 2. Copy to Spring Boot static resources
cp -r dist/* ../src/main/resources/static/

# 3. Build Spring Boot JAR
cd ..
mvn clean package -DskipTests

# 4. Run
java -jar target/j-dep-analyzer2-0.0.1-SNAPSHOT.jar
```

Open: http://localhost:8080

---

## Production Deployment

### Option A: Docker

```dockerfile
# Dockerfile
FROM eclipse-temurin:17-jre-alpine
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
# Build
docker build -t j-dep-analyzer .

# Run with SQLite
docker run -p 8080:8080 -v ./data:/data j-dep-analyzer

# Run with PostgreSQL
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=postgresql \
  -e JDEP_DB_HOST=your-project:region:instance \
  -e JDEP_DB_NAME=jdep \
  -e JDEP_DB_USER=user@project.iam \
  j-dep-analyzer
```

### Option B: GCP Cloud Run

```bash
# Build with Cloud Build
gcloud builds submit --tag gcr.io/PROJECT_ID/j-dep-analyzer

# Deploy
gcloud run deploy j-dep-analyzer \
  --image gcr.io/PROJECT_ID/j-dep-analyzer \
  --platform managed \
  --allow-unauthenticated \
  --set-env-vars SPRING_PROFILES_ACTIVE=postgresql \
  --set-env-vars JDEP_DB_HOST=project:region:instance \
  --set-env-vars JDEP_DB_NAME=jdep \
  --add-cloudsql-instances project:region:instance
```

### Option C: Traditional Server

```bash
# Copy JAR to server
scp target/*.jar user@server:/opt/jdep/

# Create systemd service
cat > /etc/systemd/system/jdep.service << EOF
[Unit]
Description=J-Dep Analyzer
After=network.target

[Service]
User=jdep
ExecStart=/usr/bin/java -jar /opt/jdep/j-dep-analyzer2.jar
WorkingDirectory=/opt/jdep
Restart=always

[Install]
WantedBy=multi-user.target
EOF

systemctl enable jdep
systemctl start jdep
```

---

## Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | `sqlite` or `postgresql` | sqlite |
| `JDEP_DB_HOST` | CloudSQL instance connection name | - |
| `JDEP_DB_NAME` | Database name | jdep |
| `JDEP_DB_USER` | Database user (IAM) | - |
| `SERVER_PORT` | HTTP port | 8080 |

### CloudSQL PostgreSQL Example

```bash
export SPRING_PROFILES_ACTIVE=postgresql
export JDEP_DB_HOST=my-project:us-central1:my-instance
export JDEP_DB_NAME=jdep
export JDEP_DB_USER=sa@my-project.iam
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json

java -jar target/j-dep-analyzer2.jar
```

---

## Development

```bash
# Run backend tests
mvn test

# Run frontend type check
cd frontend && npm run build

# Build production bundle
mvn clean package -DskipTests
```

## License

Apache 2.0
