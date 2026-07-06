package mg.fikaliako.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import mg.fikaliako.api.model.GeoPoint
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** Card-level projection for list and nearby results. */
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
    /** Metres from the query point; present only on `/v1/nearby`. */
    val distanceM: Double? = null,
)

/** The twelve one-query boolean attributes (project book ch. 4.3 / 6.1). */
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
    // SNAKE_CASE can't split a letter→digit boundary; force the contract name.
    @get:JsonProperty("open_24h") val open24h: Boolean,
)

data class OpeningInterval(
    /** 0 = Monday … 6 = Sunday. */
    val dayOfWeek: Int,
    /** Local `HH:mm` (Indian/Antananarivo). */
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

/** Full establishment detail (project book ch. 8.2 — detail page). */
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
