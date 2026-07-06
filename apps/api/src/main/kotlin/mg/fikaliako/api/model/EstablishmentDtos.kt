package mg.fikaliako.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import mg.fikaliako.api.model.GeoPoint
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class EstablishmentSummary(
  val id: UUID,
  val slug: String,
  val name: String,
  val type: String,
  val position: GeoPoint,
  val avgPriceAr: Int?,
  val verified: Boolean,
  val status: String,
  val ratingAvg: BigDecimal?,
  val ratingCount: Int,
  val distanceM: Double? = null,
)

data class Amenities(
  val delivery: Boolean,
  val parking: Boolean,
  val wifi: Boolean,
  val wheelchairAccess: Boolean,
  val airConditioning: Boolean,
  val terrace: Boolean,
  val familyFriendly: Boolean,
  val romantic: Boolean,
  val studentFriendly: Boolean,
  val scenicView: Boolean,
  @get:JsonProperty("open_24h") val open24h: Boolean,
)

data class OpeningInterval(
  val dayOfWeek: Int,
  val opensAt: String,
  val closesAt: String,
)

data class ReferentialItem(
  val code: String,
  val labelFr: String,
  val labelMg: String,
)

data class RatingSummary(
  val count: Int,
  val avgGlobal: BigDecimal?,
  val avgQuality: BigDecimal?,
  val avgPrice: BigDecimal?,
  val avgCleanliness: BigDecimal?,
  val avgSpeed: BigDecimal?,
  val avgWelcome: BigDecimal?,
  val bayesianNote: BigDecimal?,
)

data class EstablishmentDetail(
  val id: UUID,
  val slug: String,
  val name: String,
  val type: String,
  val position: GeoPoint,
  val address: String?,
  val district: String?,
  val city: String,
  val phone: String?,
  val whatsapp: String?,
  val facebookUrl: String?,
  val website: String?,
  val avgPriceAr: Int?,
  val verified: Boolean,
  val status: String,
  val openNow: Boolean,
  val amenities: Amenities,
  val openingHours: List<OpeningInterval>,
  val paymentMethods: List<ReferentialItem>,
  val cuisines: List<ReferentialItem>,
  val rating: RatingSummary,
  val createdAt: Instant,
  val updatedAt: Instant,
)
