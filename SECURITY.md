# Security Policy

Fikaliako handles phone numbers, credentials, and user location — security and privacy reports are taken seriously and are genuinely appreciated.

## Reporting a vulnerability

**Please do not open a public issue for security problems.**

Report privately instead, either way:

- **GitHub**: use [private vulnerability reporting](https://github.com/HarenaFiantso/Fikaliako/security/advisories/new) ("Report a vulnerability" on the Security tab), or
- **Email**: <alipirbay@gmail.com> with `[SECURITY]` in the subject.

Include what you can: affected endpoint or component, reproduction steps, impact, and any proof-of-concept. You can expect an acknowledgment within a few days; please allow a reasonable window for a fix before any public disclosure. Good-faith research on your own local instance is welcome — please don't test against instances you don't own, attempt denial of service, or access other people's data.

## Supported versions

The project is pre-release (early bootstrap). Only the latest `main` is supported; there are no maintained release lines yet.

## Scope

Especially interesting reports, given the project's security model (project book ch. 7.3) and privacy commitments (ch. 9):

- Authentication and session flaws: JWT validation, refresh-token rotation/revocation, OTP issuance and verification (rate limits, code lifetime, account-existence disclosure)
- Authorization bypasses across roles (`user`, `business`, `moderator`, `admin`) or between establishments and their managers
- Injection of any kind (the geo queries are native SQL — parameter handling matters)
- Privacy-guarantee violations: user location persisted as a trajectory, consultation history that is not purgeable, or published statistics below k-anonymity ≥ 20
- Upload handling (planned photos pipeline: magic-byte checks, re-encoding)

## Known dev-mode behaviors (not vulnerabilities)

These are deliberate local-development defaults; production deployments **must** override them:

- OTP codes are written to the server log (`LoggingSmsSender`) — there is no real SMS gateway yet.
- `application.yml` ships a dev-only JWT secret; set `AUTH_JWT_SECRET` in any real deployment.
- CORS defaults to `http://localhost:3000`; set `CORS_ALLOWED_ORIGINS` per environment.
- The local Docker Compose Postgres/Redis credentials are throwaway.

A report that one of these defaults reached a production deployment, however, is very much in scope.
