package mg.fikaliako.api.endpoint.rest.model

data class GeoPoint(
  val lat: Double,
  val lng: Double,
)

data class Page<T>(
  val items: List<T>,
  val nextCursor: String? = null,
)
