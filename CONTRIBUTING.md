# Contributing to Fikaliako

Misaotra for your interest! Fikaliako is a geolocated food-discovery platform for Madagascar, and contributions of all kinds are welcome — code, Malagasy/French translations, bug reports, and data-model feedback from people who know the Antananarivo food scene.

This guide covers how to contribute. For what the project is and how to run it, start with the [README](README.md).

## Ground rules

- **The project book is the spec.** Every v1 feature decision traces to a section of _Fikaliako, Livre de Projet v1.0_ (referenced in code comments as "project book ch. N"). The MVP scope is a contract (book ch. 5.2): adding something to the MVP means removing something equivalent. If your idea is outside the current scope (menus, reservations, push notifications, public API, EN locale…), open an issue to discuss it before writing code — it may belong to V2/V3.
- **Two languages, no more**: Kotlin in `apps/api`, TypeScript everywhere else.
- **English everywhere** in code, comments, and identifiers. Product vocabulary stays in domain terms: gargotte, ariary (Ar), laoka, romazava, mofo gasy…
- Be kind and constructive in issues and reviews — this project follows the [Code of Conduct](CODE_OF_CONDUCT.md). We're building for a community; act like part of one.

## Getting set up

```bash
git clone https://github.com/HarenaFiantso/Fikaliako.git fikaliako
cd fikaliako
pnpm install
pnpm dev        # API on :8080 (auto-starts PostGIS + Redis via Docker), web on :3000
```

Prerequisites, environment variables, and troubleshooting live in the [README](README.md#prerequisites). You only need Node ≥ 18, pnpm 9, and Docker — the JDK downloads itself.

## Making changes

### Branches and commits

- Branch from `main`: `feat/<topic>`, `fix/<topic>`, `docs/<topic>`.
- Commits follow [Conventional Commits](https://www.conventionalcommits.org/) with the package as scope, matching the existing history:

  ```
  feat(api): consumer account surface — profile, favorites, review posting
  feat(web): auth screens — login, register, verify-phone
  fix(api): survive real Postgres — enum label binding
  docs(api): contract v0.3.0 — authentication surfaces
  ```

  Scopes in use: `api`, `web`, `client` (the `@fikaliako/api-client` package), `ui`, or none for repo-wide changes.

- Keep commits focused; a reviewer should be able to read one commit as one idea.

### Before you push

```bash
pnpm format          # Prettier (TS/web) + Spotless ktlint (Kotlin)
pnpm lint            # ESLint across JS/TS packages
pnpm check-types     # tsc --noEmit across TS packages
pnpm turbo test --filter=api   # API tests (H2, no Docker needed)
```

Formatting is enforced, not optional — unformatted Kotlin fails `gradlew build`.

## Area-specific guides

### API (`apps/api`)

- **Packaging is layered**, not feature-modular: put new code in the matching layer package — `endpoint/rest/controller`, `endpoint/rest/model` (wire DTOs), `model` (JPA entities), `repository`, `service`, `util`, `config`. The book's modules (establishments, search, community, notifications, accounts) are conceptual boundaries inside each layer.
- **Contract first.** The OpenAPI spec at `apps/api/src/main/resources/static/v1/openapi.yaml` is hand-managed and wins over code. Implement controllers against it and amend it in the same change; mark not-yet-implemented endpoints "(planned)". After editing:

  ```bash
  pnpm --package=@redocly/cli dlx redocly lint apps/api/src/main/resources/static/v1/openapi.yaml
  pnpm --filter @fikaliako/api-client generate   # regenerate the typed client
  ```

  Commit the spec and the regenerated `packages/api-client/src/schema.ts` together.

- **Flyway owns the schema.** Add a new `V<n>__*.sql` under `src/main/resources/db/migration/` — never edit an applied migration, never let Hibernate generate DDL. Identifiers are English translations of the book's French data dictionary.
- **Geospatial rule**: proximity queries filter with `ST_DWithin` so they stay on the GiST index — never raw distance computation.
- **Tests**: aim for ≥ 70 % coverage on business logic (book ch. 9 makes this contractual). `./gradlew test --tests 'mg.fikaliako.api.SomeTest'` runs a single class.

### Web (`apps/web`)

- **File names are kebab-case** (`login-form.tsx`, `auth-store.ts`).
- Stack conventions: TanStack Query for data fetching (hooks in `hooks/`), Zustand for client state (`lib/auth/*-store.ts`), Zod for validation (`lib/validation/`), Motion for animations, shadcn-style primitives in `components/ui/`.
- Styling is Tailwind CSS v4 — design tokens live in `app/globals.css`; use the semantic tokens (`bg-primary`, `text-muted-foreground`…), not raw colors.
- Keep comments minimal: only document genuinely non-obvious logic.

### Translations (`apps/web/messages/`)

French (`fr.json`) is the complete reference catalog. `mg.json` and `en.json` deep-merge onto it, so **partial translations are fine** — translate the keys you're sure of and omit the rest; missing keys fall back to French. Malagasy improvements are especially welcome: the current `mg.json` covers only the basics.

## Pull requests

1. Make sure `pnpm format:check`, `pnpm lint`, `pnpm check-types`, and the API tests pass.
2. If you touched the API contract: spec linted, client regenerated, both committed together.
3. Describe **what** changed and **why** — reference the project book chapter or the issue that motivated it.
4. Screenshots (light and dark) for anything visual.
5. Every PR gets a code review before merging (book ch. 9).

## Reporting bugs and proposing features

Open a GitHub issue with:

- **Bugs**: what you did, what you expected, what happened — plus logs or screenshots. For API issues, the `X-Correlation-Id` response header helps trace the request.
- **Features**: the problem it solves and, if you can, where it fits in the book's roadmap (MVP / V2 / V3). Scope changes need a versioned revision of the book, not ad-hoc drift.

## License

By contributing, you agree that your contributions are licensed under the [Apache License 2.0](LICENSE), the same license as the project.
