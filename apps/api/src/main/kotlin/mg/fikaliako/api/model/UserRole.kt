package mg.fikaliako.api.model

// Mirrors the user_role Postgres enum (V3 + V9). BUSINESS is the premium
// establishment account (restaurateurs, gargotte owners…) — it manages its own
// establishments but has no moderation power.
enum class UserRole {
  USER,
  BUSINESS,
  MODERATOR,
  ADMIN,
}
