# FirstClub Membership Program

Backend service for FirstClub's tiered membership program.

> **Demo data, users, passwords, cURL examples:** [DUMMYINFO.md](DUMMYINFO.md)  
> **Full API contracts (success + failure):** [APICONTRACTS.md](APICONTRACTS.md)

## Prerequisites

- **Java 21**
- **Docker**

## Reset Database (after schema changes)(Do this to start mysql and phpmyadmin and make sure any Container management application like Rancher Desktop or Docker Desktop is configured, up and running)

```bash
cd docker
docker compose down -v
docker compose up -d
```
## If `docker compose up` doesnot work, Do - export DOCKER_HOST=unix:///Users/mayank.nln/.rd/docker.sock (add your local address of docker.sock file)

## Run

```bash
mvn clean install
./mvnw spring-boot:run
```

- Health: http://localhost:8080/actuator/health
- Swagger: http://localhost:8080/swagger-ui/index.html
- API-Docs(copy its content and import in postman): http://localhost:8080/v3/api-docs

## Database configuration

MySQL is the default database, configured via `application-mysql.yml` and `SPRING_PROFILES_ACTIVE=mysql`.

Env vars: `DB_NAME`, `DB_URL`, `DB_USER`, `DB_PASSWORD` — see [DUMMYINFO.md](DUMMYINFO.md).

The profile-based layout is ready for adding other databases later (new `application-<profile>.yml` + driver dependency) without changing services or repositories.

## Architecture

```
controller → service (ResponseEntity) → JPA repository → DB (profile-selected)
                ↓
     YAML catalog (plans/tiers/benefits — internal config)
     Strategy / State / Factory / Observer / Builder
     JWT interceptor (cookie-based auth)
```

## Design Patterns

| Pattern | Usage |
|---------|-------|
| **Strategy** | `TierEligibilityEvaluator` — order count, order value, cohort |
| **State** | `MembershipState` — ACTIVE, CANCELLED, EXPIRED transitions |
| **Factory** | `TierEvaluatorFactory`, `MembershipStateFactory` |
| **Abstract Factory** | `MembershipDomainFactory` |
| **Observer** | `MembershipEventPublisher` → `MembershipAuditListener` |
| **Builder** | Lombok `@Builder` on entities and DTOs |
