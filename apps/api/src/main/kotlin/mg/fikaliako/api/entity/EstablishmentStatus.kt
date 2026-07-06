package mg.fikaliako.api.entity

/**
 * Establishment lifecycle (project book ch. 6.1). Names match the Postgres
 * `establishment_status` enum labels for Hibernate's NAMED_ENUM mapping.
 */
@Suppress("ktlint:standard:enum-entry-name-case")
enum class EstablishmentStatus {
    active,
    closed,
    pending,
}
