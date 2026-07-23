package mg.fikaliako.api.endpoint.rest.model

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin

data class GeoPoint(
  @field:DecimalMin("-90.0") @field:DecimalMax("90.0")
  val lat: Double,

  @field:DecimalMin("-180.0") @field:DecimalMax("180.0")
  val lng: Double,
)

data class Page<T>(
  val items: List<T>,
  val nextCursor: String? = null,
)
