# Fikaliako API — bootstrap notes

What was set up in `apps/api` and why. Reference: the project book v1.0 (July 2026), chapters 6–9.

## Stack

- **Kotlin 2.3 + Spring Boot 4.1** on a **JVM 21 toolchain** (book ch. 7.1 specifies Kotlin + Spring Boot 3 / JVM 21; Initializr only offers the Boot 4 stable line — same architecture, newer baseline).
- **Gradle (Kotlin DSL)** with the wrapper committed; the foojay toolchain resolver downloads JDK 21 automatically, so no local JDK 21 install is needed.
- **PostgreSQL 16 + PostGIS** via Spring Data JPA and `hibernate-spatial` (JTS) — native geospatial queries for "around me" (book ch. 6.2).
- **Flyway** owns the schema (`ddl-auto: validate`); `V1__init.sql` creates the PostGIS/pg_trgm extensions and the `establishments` + `opening_hours` tables with GiST and trigram indexes (book ch. 6.1).
- **Redis** for cache, rate limiting and OTP queues (book ch. 7.1).
- **Spring Security**, stateless: public GETs on `/v1/**`, everything else requires authentication. JWT lands with the accounts module (book ch. 7.3).
- **springdoc-openapi**: the OpenAPI contract is generated from the code and served at `/v1/openapi` (Swagger UI at `/v1/docs`), later used to generate the TypeScript client types (book ch. 7.1, "one source contract").
- **Actuator** exposes `health`, `info`, `metrics` (book ch. 9, availability target).

## Architecture

Modular monolith: a single Spring Boot deployable split into packages with clear
boundaries (`establishments`, `search`, `community`, `notifications`, `accounts`),
extractable into services later if one module runs hot (book ch. 7.1). All
application code and comments are in English; product vocabulary (gargotte,
ariary…) stays in domain terms where needed.

## Monorepo integration

- `apps/api/package.json` maps pnpm scripts to Gradle: `build`, `dev` (bootRun), `test`, `clean` — so Turborepo treats the API like any other workspace package.
- `apps/api/turbo.json` overrides cache outputs (`build/libs/**`, test reports); the root `turbo.json` gained a `test` task.
- Typical commands from the root:
  - `pnpm turbo build --filter=api`
  - `pnpm turbo dev --filter=api`
  - `pnpm turbo test --filter=api`

## Local dev services

`compose.yaml` at the **monorepo root** defines PostGIS and Redis. Spring Boot's
Docker Compose support starts them on `bootRun`:

- launched from the repo root (IDE), Boot finds `./compose.yaml` by default;
- launched via Gradle (`./gradlew bootRun`, `turbo dev`), the `bootRun` task passes
  `spring.docker.compose.file` pointing at the root file explicitly.

The PostGIS service carries the `org.springframework.boot.service-connection: postgres`
label because the image name (`postgis/postgis`) is not auto-detected as Postgres.

## Tests

`./gradlew test` needs no Docker: the context smoke test runs on in-memory H2 with
Flyway disabled (PostGIS SQL can't run on H2). Once real entities/repositories
exist, tests move to Testcontainers with a PostGIS image.

## Web client

`apps/web` (Next.js) consumes the API server-side: the home page fetches
`GET /v1/ping` (`NEXT_PUBLIC_API_URL`, default `http://localhost:8080`) and renders
the response, degrading gracefully when the API is down.
