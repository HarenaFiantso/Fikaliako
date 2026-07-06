# Fikaliako API

Spring Boot backend (Kotlin, JVM 21) for Fikaliako — a modular monolith as defined in chapter 7 of the project book.

## Modules (packages under `mg.fikaliako.api`)

| Package | Role |
| --- | --- |
| `establishments` | Listings, opening hours, attributes |
| `search` | Search & geospatial (PostGIS, Meilisearch) |
| `community` | Reviews, contributions, moderation |
| `notifications` | Notifications & promotions (V2) |
| `accounts` | Users, SMS OTP, JWT |
| `config` | Cross-cutting configuration (security, OpenAPI) |

Only `establishments` and `config` exist so far; the other modules are created as features land.

## Getting started

```bash
# from the monorepo root
pnpm turbo dev --filter=api
# or directly
cd apps/api && ./gradlew bootRun
```

`bootRun` automatically starts PostgreSQL/PostGIS and Redis from the `compose.yaml`
at the monorepo root (Spring Boot Docker Compose support) — a running Docker/Podman
daemon is required. The database schema is managed by Flyway
(`src/main/resources/db/migration`).

- API: `http://localhost:8080/v1/ping`
- OpenAPI: `http://localhost:8080/v1/openapi` · Swagger UI: `http://localhost:8080/v1/docs`
- Health: `http://localhost:8080/actuator/health`

## Build & tests

```bash
pnpm turbo build --filter=api   # ./gradlew build (JDK 21 auto-provisioned via foojay)
pnpm turbo test --filter=api    # ./gradlew test — in-memory H2, no Docker required
```

## Configuration

All settings have local defaults and are overridable via environment variables:
`DB_URL`, `DB_USER`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `SERVER_PORT`.

Note: the project book (July 2026) mentions Spring Boot 3; the project is generated
on Spring Boot 4.1 (current stable line, the only one offered by Spring Initializr),
with the same architecture and JVM 21 as required by chapter 7.
