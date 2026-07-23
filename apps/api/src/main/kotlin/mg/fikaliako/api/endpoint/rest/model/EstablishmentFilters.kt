package mg.fikaliako.api.endpoint.rest.model

data class EstablishmentFilters(
  val types: List<String> = emptyList(),
  val minPrice: Int? = null,
  val maxPrice: Int? = null,
  val cuisine: String? = null,
  val payment: String? = null,
  val amenities: List<String> = emptyList(),
  val openNow: Boolean = false,
) {
  companion object {
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
