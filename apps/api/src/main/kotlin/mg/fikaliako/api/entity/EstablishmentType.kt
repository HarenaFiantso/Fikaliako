package mg.fikaliako.api.entity

/**
 * Establishment kind (project book ch. 6.1). Constant names match the Postgres
 * `establishment_type` enum labels verbatim, so Hibernate's NAMED_ENUM mapping
 * round-trips without a converter — hence the non-idiomatic lowercase names.
 */
@Suppress("ktlint:standard:enum-entry-name-case")
enum class EstablishmentType {
    restaurant,
    gargotte,
    cafe,
    snack,
    food_truck,
    street_vendor,
    pastry_shop,
    bar_restaurant,
    hotel_restaurant,
}
