package mg.fikaliako.api.model

/** Immutable filter set shared by list and nearby queries (book ch. 4.3). */
data class EstablishmentFilters(
    val type: String? = null,
    val minPrice: Int? = null,
    val maxPrice: Int? = null,
    val cuisine: String? = null,
    /** A payment code (e.g. `mvola`) or the literal `mobile` (any operator). */
    val payment: String? = null,
    /** Boolean amenity columns that must all be true; validated against a whitelist. */
    val amenities: List<String> = emptyList(),
    val openNow: Boolean = false,
) {
    companion object {
        /** Whitelisted boolean columns — guards the dynamic amenity filter against injection. */
        val AMENITY_COLUMNS =
            setOf(
                "delivery",
                "parking",
                "wifi",
                "wheelchair_access",
                "air_conditioning",
                "terrace",
                "family_friendly",
                "romantic",
                "student_friendly",
                "scenic_view",
                "open_24h",
            )
    }
}
