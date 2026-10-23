<style>
@import url('https://fonts.googleapis.com/css2?family=Poppins:wght@600;700&family=Inter:wght@400;500;600&display=swap');

body, p, li, td, th, blockquote { font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; }

h1, h2, h3 { font-family: 'Poppins', 'Inter', sans-serif; }
h1 { color: #4338ca; font-weight: 700; letter-spacing: -0.02em; }
h2 { color: #4f46e5; font-weight: 600; letter-spacing: -0.01em; }
h3 { color: #0891b2; font-weight: 600; }

.mermaid, .mermaid svg { max-width: 50% !important; width: 50% !important; height: auto !important; display: block; margin: 0 auto; }

table { border-collapse: collapse !important; width: 100%; margin: 1rem 0; }
th, td { border: 1px solid #cbd5e1 !important; padding: 0.6rem 0.9rem; text-align: left; vertical-align: top; }
th { background: #f1f5f9; }
tr:nth-child(even) td { background: #f8fafc; }
</style>

# End-to-End CI/CD Pipeline for a Calculator REST API

**CSE 816 — Software Production Engineering — Mini-Project Report**

<div style="height:5px;background:linear-gradient(90deg,#6366f1,#06b6d4,#10b981);border-radius:3px;margin:0.75rem 0 1.25rem 0;"></div>

![Java](https://img.shields.io/badge/Java-22-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.2-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white)
![Jenkins](https://img.shields.io/badge/Jenkins-CI%2FCD-D24939?style=flat-square&logo=jenkins&logoColor=white)
![Build](https://img.shields.io/badge/pipeline-passing-success?style=flat-square)

<div style="background:#f8fafc;border:1px solid #e2e8f0;border-left:4px solid #6366f1;border-radius:8px;padding:0.9rem 1.2rem;margin:1rem 0;">
<strong>Author:</strong> Dheeraj Murthy &nbsp;•&nbsp; <strong>Date:</strong> 23 October 2026<br/>
<strong>Repository:</strong> <a href="https://github.com/Dheeraj-Murthy/spe-miniproject">github.com/Dheeraj-Murthy/spe-miniproject</a><br/>
<strong>Docker Hub:</strong> <a href="https://hub.docker.com/r/dheerajmurthy/calculator-api">hub.docker.com/r/dheerajmurthy/calculator-api</a>
</div>

### Contents

1. [Overview](#1-overview)
2. [Architecture](#2-architecture)
3. [Pipeline stage descriptions](#3-pipeline-stage-descriptions)
4. [Containerization and deployment](#4-containerization-and-deployment)
5. [Challenges faced](#5-challenges-faced)
6. [Conclusion](#6-conclusion)
7. [Appendix: Screenshots](#7-appendix-screenshots)


## 1. Overview

<div style="height:3px;background:linear-gradient(90deg,#6366f1,#06b6d4);border-radius:2px;width:4rem;margin-bottom:1rem;"></div>

This report covers the implementation of a Spring Boot Calculator REST API (`com.dheeraj.miniproject`) and the full DevOps pipeline wrapped around it: **Git/GitHub** for version control, **Jenkins** for CI/CD, **Docker** for containerization, and **Docker Compose** for deployment — matching the scope defined in the CSE 816 mini-project problem statement.

The application itself is intentionally simple; the engineering is in the pipeline around it. A push to `main` is checked out, built, tested, containerized, pushed to Docker Hub, and redeployed with zero manual steps.

<div style="page-break-before: always; padding-top: 2.5rem;"></div>

## 2. Architecture

<div style="height:3px;background:linear-gradient(90deg,#6366f1,#06b6d4);border-radius:2px;width:4rem;margin-bottom:1rem;"></div>

```mermaid
%%{init: {
  "theme": "base",
  "themeVariables": {
    "background": "#ffffff",
    "primaryColor": "#eef2ff",
    "primaryTextColor": "#1e293b",
    "primaryBorderColor": "#6366f1",
    "secondaryColor": "#ecfeff",
    "tertiaryColor": "#fff7ed",
    "lineColor": "#64748b",
    "clusterBkg": "#f8fafc",
    "clusterBorder": "#cbd5e1",
    "edgeLabelBackground": "#ffffff",
    "fontFamily": "Helvetica, Arial, sans-serif",
    "fontSize": "12px"
  },
  "flowchart": { "useMaxWidth": true, "htmlLabels": true, "nodeSpacing": 18, "rankSpacing": 22 }
}}%%
flowchart TD
    GH["GitHub Repo\nspe-miniproject"] -->|git push| WH{"Webhook\nvia ngrok tunnel"}
    WH --> J

    subgraph J["Jenkins - Declarative Pipeline"]
        direction TB
        S1[Checkout] --> S2[Build] --> S3[Test] --> S4["Docker Build"] --> S5[Push] --> S6[Deploy]
    end

    J -->|"docker compose up -d"| CP

    subgraph CP["Docker Compose Stack - calculator-net bridge"]
        direction TB
        NG["nginx proxy\n:8081"] --> API["calculator-api\n:8082\nHEALTHCHECK /actuator/health"]
    end

    classDef github fill:#a4a9fe,stroke:#0d1117,color:#ffffff,stroke-width:2px
    classDef jenkins fill:#4f7a96,stroke:#2d4a5c,color:#ffffff,stroke-width:2px
    classDef docker fill:#2496ed,stroke:#066da5,color:#ffffff,stroke-width:2px
    classDef webhook fill:#fef3c7,stroke:#d97706,color:#78350f,stroke-width:2px

    class GH github
    class S1,S2,S3,S4,S5,S6 jenkins
    class NG,API docker
    class WH webhook
```

Jenkins and the deployed application run as separate local services, both reachable on `localhost` but on distinct ports (`:8080` for Jenkins, `:8082`/`:8081` for the app stack) to avoid port collisions during local development and demo.

<div style="page-break-before: always; padding-top: 2.5rem;"></div>

## 3. Pipeline stage descriptions

<div style="height:3px;background:linear-gradient(90deg,#6366f1,#06b6d4);border-radius:2px;width:4rem;margin-bottom:1rem;"></div>

| Stage | What it does |
|---|---|
| **Checkout** | Pulls the latest `main` commit, triggered by a GitHub push webhook (`githubPush()`) tunneled to local Jenkins via `ngrok` |
| **Build** | `mvn clean package -DskipTests` — compiles and packages the executable jar; tests deliberately skipped here so Build and Test fail independently |
| **Test** | `mvn test` — 5 JUnit 5 unit tests (`CalculatorService`: add/subtract/multiply/divide/divide-by-zero) + 3 MockMvc integration tests against real `/api/v1/*` endpoints; results published from `target/surefire-reports/*.xml`, a failure stops the build before any image is built |
| **Docker Build** | Builds the multi-stage image, tagged `${BUILD_NUMBER}` and `latest` |
| **Push** | Logs into Docker Hub via the Jenkins-managed `dockerhub-credentials` entry (never hard-coded) and pushes both tags |
| **Deploy** | Sets `DOCKER_IMAGE` to the freshly pushed tag, runs `docker compose up -d --remove-orphans`, then polls `/actuator/health` until it responds |

Build status (success/failure) triggers an email from the pipeline's `post` block. Compose's own idempotent reconciliation — a re-run only recreates containers whose config changed — satisfies the re-runnable/idempotent requirement without extra scripting.


## 4. Containerization and deployment

<div style="height:3px;background:linear-gradient(90deg,#6366f1,#06b6d4);border-radius:2px;width:4rem;margin-bottom:1rem;"></div>

**Dockerfile** is a two-stage build: a `maven:3.9-eclipse-temurin-22` stage compiles and packages the jar, and a slim `eclipse-temurin:22-jre-alpine` stage copies only the built jar. The runtime stage runs as a dedicated non-root `spring` user, declares `EXPOSE 8080`, and carries its own `HEALTHCHECK` hitting `/actuator/health`. `.dockerignore` excludes `target/`, `.git/`, and IDE/OS cruft to keep the build context small and caching effective.

**docker-compose.yml** brings up two services on a shared bridge network (`calculator-net`): `calculator-api` (the app, `restart: unless-stopped`, environment-driven JVM options, a Compose-level healthcheck mirroring the Dockerfile's) and `nginx` (a reverse proxy satisfying the "second component" requirement), which waits for `calculator-api`'s `service_healthy` condition before starting. A single `docker compose up -d` launches the full stack.

<div style="page-break-before: always; padding-top: 2.5rem;"></div>

## 5. Challenges faced

<div style="height:3px;background:linear-gradient(90deg,#6366f1,#06b6d4);border-radius:2px;width:4rem;margin-bottom:1rem;"></div>

Several real issues surfaced while wiring this up, beyond the application logic itself.

> **Spring Boot 4 package relocation**
> The generated project pulled Spring Boot 4.1.2, which renamed several test-support packages — `AutoConfigureMockMvc` moved out of the old `org.springframework.boot.test.autoconfigure.web.servlet` package into a new `org.springframework.boot.webmvc.test.autoconfigure` one. The integration test failed to compile against documentation written for Boot 3.x until the import was corrected by inspecting the actual jar contents.

> **JDK version mismatch risk between build and runtime**
> The pom targets Java 22; the Dockerfile originally used Java 21 base images for both build and runtime stages, which would have produced class files the runtime JRE couldn't load. Caught before running by verifying Java 22 Temurin images existed and switching both stages to match.

> **Port collision between Jenkins and the application**
> Homebrew's `jenkins-lts` formula hardcodes port 8080, which is also where the app's `docker-compose.yml` originally mapped `calculator-api`. Worse, `brew services restart` regenerates the launch agent plist from the formula on every restart, silently reverting a manual port change. Resolved by leaving Jenkins on its default and remapping the container's host-side port instead (`8082:8080`), which doesn't fight Homebrew's defaults.

> **Minimal PATH/JAVA_HOME under launchd**
> Homebrew starts Jenkins via `launchd`, which provides a much smaller `PATH` than an interactive shell — `mvn` and the correct `java` were invisible to pipeline `sh` steps. Fixed by pinning `JAVA_HOME` and prepending the relevant bin directories to `PATH` explicitly in the Jenkinsfile's `environment` block.

> **Missing Jenkins credential crashing the whole pipeline early**
> Before the `dockerhub-credentials` entry existed in the Jenkins credential store, the `environment { credentials(...) }` binding failed before any stage ran, which also broke the `post` blocks (lost node context, undefined `STUDENT_EMAIL`) — the real error was buried under secondary stack traces and took some digging to isolate.

> **Docker Hub token scope**
> The first access token was created as read-only, so `docker push` authenticated successfully but failed with `unauthorized: access token has insufficient scopes`. Regenerating the token with read/write scope fixed it.


<div style="page-break-before: always; padding-top: 2.5rem;"></div>

> **Gmail SMTP authentication**
> Email notifications initially failed with `535 5.7.8 Username and Password not accepted` — Gmail rejects a normal account password for SMTP once 2-Step Verification is involved; it requires a generated App Password instead. A follow-up slip sent the first successful notification to the wrong address (a stale email from an unrelated context) before it was corrected to the right inbox.

> **No public endpoint for a GitHub webhook**
> Since Jenkins runs locally, GitHub's webhook delivery can't reach it directly; `ngrok http 8080` provides a temporary public tunnel, with the caveat that the free-tier URL changes on every restart and the webhook payload URL has to be re-pointed accordingly.


## 6. Conclusion

<div style="height:3px;background:linear-gradient(90deg,#6366f1,#06b6d4);border-radius:2px;width:4rem;margin-bottom:1rem;"></div>

The pipeline satisfies all required stages — checkout, build, test (with published JUnit results), Docker build, push to Docker Hub via managed credentials, and Compose-based deploy with a health check — triggered automatically on a GitHub push. Most of the friction in building this came not from the application code but from the surrounding environment: tool version drift (Spring Boot 4's package moves), local port/service conflicts, and credential/scope misconfiguration — exactly the class of problem this course is meant to surface.

<div style="page-break-before: always; padding-top: 2.5rem;"></div>

## 7. Appendix: Screenshots

<div style="height:3px;background:linear-gradient(90deg,#6366f1,#06b6d4);border-radius:2px;width:4rem;margin-bottom:1rem;"></div>

<div style="padding-top: 2.5rem; page-break-inside: avoid; break-inside: avoid;">
<img src="Screen%20Shot%202026-10-23%20at%2021.41.27.jpg" alt="GitHub repository file listing" style="max-width:100%;max-height:32vh;display:block;margin:0.5rem auto;border-radius:10px;border:1px solid #e2e8f0;box-shadow:0 6px 20px rgba(0,0,0,0.15);" />
<p style="text-align:center;font-size:0.8rem;color:#64748b;margin:0.4rem 0 1.5rem 0;"><strong>Fig. 1:</strong> GitHub repository — source, Jenkinsfile, Dockerfile, docker-compose.yml with per-feature commit history</p>
<img src="Screen%20Shot%202026-10-23%20at%2021.41.18.jpg" alt="Git log and branches" style="max-width:100%;max-height:32vh;display:block;margin:0.5rem auto;border-radius:10px;border:1px solid #e2e8f0;box-shadow:0 6px 20px rgba(0,0,0,0.15);" />
<p style="text-align:center;font-size:0.8rem;color:#64748b;margin:0.4rem 0 0 0;"><strong>Fig. 2:</strong> Git commit history — incremental commits plus a feature branch (feature/report-and-docs) off main</p>
</div>

<div style="page-break-before: always; padding-top: 2.5rem; page-break-inside: avoid; break-inside: avoid;">
<img src="Screen%20Shot%202026-10-23%20at%2021.41.41.jpg" alt="GitHub webhook settings" style="max-width:100%;max-height:75vh;display:block;margin:0.5rem auto;border-radius:10px;border:1px solid #e2e8f0;box-shadow:0 6px 20px rgba(0,0,0,0.15);" />
<p style="text-align:center;font-size:0.8rem;color:#64748b;margin:0.4rem 0 0 0;"><strong>Fig. 3:</strong> GitHub webhook configuration — payload URL tunneled through ngrok to local Jenkins, triggering on push</p>
</div>

<div style="page-break-before: always; padding-top: 2.5rem; page-break-inside: avoid; break-inside: avoid;">
<img src="Screen%20Shot%202026-10-23%20at%2021.42.39.jpg" alt="Jenkins pipeline, all stages green" style="max-width:100%;max-height:32vh;display:block;margin:0.5rem auto;border-radius:10px;border:1px solid #e2e8f0;box-shadow:0 6px 20px rgba(0,0,0,0.15);" />
<p style="text-align:center;font-size:0.8rem;color:#64748b;margin:0.4rem 0 1.5rem 0;"><strong>Fig. 4:</strong> Jenkins pipeline — full green run, all stages (Checkout SCM → Checkout → Build → Test → Docker Build → Push → Deploy → Post Actions)</p>
<img src="Screen%20Shot%202026-10-23%20at%2021.41.01.jpg" alt="Jenkins Push stage console output" style="max-width:100%;max-height:32vh;display:block;margin:0.5rem auto;border-radius:10px;border:1px solid #e2e8f0;box-shadow:0 6px 20px rgba(0,0,0,0.15);" />
<p style="text-align:center;font-size:0.8rem;color:#64748b;margin:0.4rem 0 0 0;"><strong>Fig. 5:</strong> Jenkins Push stage — Docker Hub login and image push via the dockerhub-credentials Jenkins credential</p>
</div>

<div style="page-break-before: always; padding-top: 2.5rem; page-break-inside: avoid; break-inside: avoid;">
<img src="Screen%20Shot%202026-10-23%20at%2021.41.51.jpg" alt="Docker Hub repository listing" style="max-width:100%;max-height:32vh;display:block;margin:0.5rem auto;border-radius:10px;border:1px solid #e2e8f0;box-shadow:0 6px 20px rgba(0,0,0,0.15);" />
<p style="text-align:center;font-size:0.8rem;color:#64748b;margin:0.4rem 0 1.5rem 0;"><strong>Fig. 6:</strong> Docker Hub — calculator-api image pushed and public</p>
<img src="Screen%20Shot%202026-10-23%20at%2021.59.08.jpg" alt="docker ps output" style="max-width:100%;max-height:22vh;display:block;margin:0.5rem auto;border-radius:10px;border:1px solid #e2e8f0;box-shadow:0 6px 20px rgba(0,0,0,0.15);" />
<p style="text-align:center;font-size:0.8rem;color:#64748b;margin:0.4rem 0 0 0;"><strong>Fig. 7:</strong> docker ps showing both containers running, calculator-api reporting (healthy)</p>
</div>

<div style="page-break-before: always; padding-top: 2.5rem; page-break-inside: avoid; break-inside: avoid;">
<img src="Screen%20Shot%202026-10-23%20at%2021.55.58.jpg" alt="Actuator health endpoint" style="max-width:100%;max-height:75vh;display:block;margin:0.5rem auto;border-radius:10px;border:1px solid #e2e8f0;box-shadow:0 6px 20px rgba(0,0,0,0.15);" />
<p style="text-align:center;font-size:0.8rem;color:#64748b;margin:0.4rem 0 0 0;"><strong>Fig. 8:</strong> /actuator/health reporting all components UP</p>
</div>

<div style="page-break-before: always; padding-top: 2.5rem; page-break-inside: avoid; break-inside: avoid;">
<img src="Screen%20Shot%202026-10-23%20at%2021.56.24.jpg" alt="Sample calculator API response" style="max-width:100%;max-height:32vh;display:block;margin:0.5rem auto;border-radius:10px;border:1px solid #e2e8f0;box-shadow:0 6px 20px rgba(0,0,0,0.15);" />
<p style="text-align:center;font-size:0.8rem;color:#64748b;margin:0.4rem 0 1.5rem 0;"><strong>Fig. 9:</strong> Sample API response — GET /api/v1/add?a=2&amp;b=3</p>
<img src="Screen%20Shot%202026-10-23%20at%2022.00.08.jpg" alt="Build success email notification" style="max-width:100%;max-height:32vh;display:block;margin:0.5rem auto;border-radius:10px;border:1px solid #e2e8f0;box-shadow:0 6px 20px rgba(0,0,0,0.15);" />
<p style="text-align:center;font-size:0.8rem;color:#64748b;margin:0.4rem 0 0 0;"><strong>Fig. 10:</strong> Email notification on successful build, sent via the pipeline's post block</p>
</div>
