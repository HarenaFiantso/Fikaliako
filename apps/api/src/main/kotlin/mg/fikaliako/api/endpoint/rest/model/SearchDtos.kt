package mg.fikaliako.api.endpoint.rest.model

/**
 * Echo of what the deterministic smart-search rules (project book ch. 4.2)
 * actually applied, so clients can tell the user "showing open places within
 * 1 km under 5 000 Ar". Present only when at least one intent matched.
 */
data class SearchInterpretation(
  val intents: List<String>,
  val openNow: Boolean? = null,
  val maxPriceAr: Int? = null,
  val types: List<String>? = null,
  val amenities: List<String>? = null,
  val payment: String? = null,
  val radiusM: Double? = null,
  val ordering: String,
)

data class SearchPage(
  val items: List<EstablishmentSummary>,
  val nextCursor: String? = null,
  val interpretation: SearchInterpretation? = null,
)
