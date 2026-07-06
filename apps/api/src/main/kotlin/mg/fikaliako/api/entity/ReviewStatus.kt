package mg.fikaliako.api.entity

/** Review moderation state; names match the Postgres `review_status` enum. */
@Suppress("ktlint:standard:enum-entry-name-case")
enum class ReviewStatus {
    published,
    hidden,
    flagged,
}
