# Calculator DevOps

A small Spring Boot REST calculator wrapped in a full CI/CD pipeline (Jenkins -> Docker Hub -> Docker Compose), built for CSE 816 Software Production Engineering.

## Stack

- Java 22, Spring Boot 4.1.2 (Web + Actuator)
- Maven
- JUnit 5 + MockMvc
- Docker (multi-stage build) + Docker Compose
- Jenkins declarative pipeline

## Build & test locally

```bash
mvn clean package      # compiles, packages the executable jar (target/*.jar)
mvn test                # runs unit + integration tests, writes JUnit XML to target/surefire-reports/
```

## Run locally (no Docker)

```bash
mvn spring-boot:run
# or
java -jar target/miniproject-0.0.1-SNAPSHOT.jar
```

App listens on `:8080`.

## Run with Docker

```bash
docker build -t calculator-api:local .
docker run -p 8080:8080 calculator-api:local
```

## Run the full stack with Docker Compose

```bash
docker compose up -d
```

This starts:
- `calculator-api` — the Spring Boot app, scaled to **2 replicas**, each with a Docker `HEALTHCHECK` against `/actuator/health`. No host port published — only reachable through the proxy.
- `nginx` (`calculator-proxy`) — reverse proxy in front of the app, host port `8081` -> container `80`. Resolves `calculator-api` through Docker's embedded DNS at request time (rather than a static upstream), so it both load-balances across the two replicas and automatically picks up new container IPs after a redeploy. Waits for `calculator-api` to report healthy before starting.

Both share the `calculator-net` bridge network.

## API

| Operation | Endpoint | Example |
|---|---|---|
| Add | `GET /api/v1/add?a=&b=` | `/api/v1/add?a=2&b=3` -> `{"operation":"add","a":2.0,"b":3.0,"result":5.0}` |
| Subtract | `GET /api/v1/subtract?a=&b=` | |
| Multiply | `GET /api/v1/multiply?a=&b=` | |
| Divide | `GET /api/v1/divide?a=&b=` | `b=0` -> HTTP 400 with structured error JSON |
| Health | `GET /actuator/health` | Spring Actuator UP/DOWN |

Invalid or missing `a`/`b` params also return HTTP 400 with a structured error body:

```json
{
  "timestamp": "...",
  "status": 400,
  "error": "Bad Request",
  "message": "Missing required parameter: b",
  "path": "/api/v1/add"
}
```

Sample calls, via the nginx proxy (the only externally reachable entry point — `calculator-api` itself publishes no host port since it's scaled to 2 replicas):

```bash
curl "http://localhost:8081/api/v1/add?a=2&b=3"
curl "http://localhost:8081/api/v1/divide?a=5&b=0"
curl "http://localhost:8081/actuator/health"
```

## CI/CD pipeline (Jenkinsfile)

Declarative pipeline, triggered via a GitHub webhook (`githubPush()`). Since Jenkins runs locally, it needs a public URL for GitHub to reach it — see setup step 6 below (ngrok).

| Stage | What it does |
|---|---|
| Checkout | Pulls latest from GitHub |
| Build | `mvn clean package` (tests skipped here) |
| Test | `mvn test`, publishes JUnit XML results, fails the build on test failure |
| Docker Build | Builds the image, tagged `${BUILD_NUMBER}` and `latest` |
| Push | Pushes both tags to Docker Hub using the Jenkins `dockerhub-credentials` entry |
| Deploy | `docker compose up -d`, polls `/actuator/health` until it responds |

Email notification (success/failure) fires from the `post` block to the student's email.

### One-time Jenkins setup

1. **Credentials**: Manage Jenkins -> Credentials -> add a "Username with password" credential, ID `dockerhub-credentials` (Docker Hub username + access token, not your account password).
2. **Email**: Manage Jenkins -> System -> configure SMTP under "E-mail Notification" / Extended Email.
3. **Job**: New Item -> Pipeline -> "Pipeline script from SCM" -> Git -> this repo's URL -> script path `Jenkinsfile`.
4. Jenkins runs locally on `:8080`; the app stack is only reachable through the nginx proxy on `:8081` to avoid the clash.
5. **GitHub Integration Plugin** must be installed for the `githubPush()` trigger.
6. **Public URL for the webhook** (local Jenkins only): run `ngrok http 8080`, take the `https://*.ngrok-free.app` URL it prints, and set it as the GitHub repo's webhook payload URL: `https://<ngrok-id>.ngrok-free.app/github-webhook/`, content type `application/json`, event: "Just the push event." ngrok must stay running for the webhook to reach Jenkins.

## Architecture

```
GitHub push
   |
   v
Jenkins (poll SCM) --> Checkout --> Build --> Test --> Docker Build --> Push (Docker Hub)
                                                                            |
                                                                            v
                                                                    Deploy stage
                                                                            |
                                                                            v
                                                          docker compose up -d
                                                                            |
                                                                            v
                                                                    nginx proxy (8081)
                                                                      /          \
                                                        calculator-api x2   (Docker DNS, resolved per-request)
                                                         \__________________/
                                                          calculator-net (bridge)
```

-- M S Dheeraj Murthy
