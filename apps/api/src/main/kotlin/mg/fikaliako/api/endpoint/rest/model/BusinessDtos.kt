package mg.fikaliako.api.endpoint.rest.model

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

// PATCH /v1/business/establishments/{id} — the fields a premium (BUSINESS)
// account may edit on its own establishment. Absent fields stay untouched.
// Position and verification are excluded on purpose: pin moves go through the
// community/moderation loop (book ch. 4.8), verification is a moderator act.
data class BusinessEstablishmentUpdate(
  @field:Size(min = 2, max = 120)
  val name: String? = null,

  @field:Size(max = 255)
  val address: String? = null,

  @field:Size(max = 120)
  val district: String? = null,

  @field:Size(max = 30)
  val phone: String? = null,

  @field:Size(max = 30)
  val whatsapp: String? = null,

  @field:Size(max = 255)
  val facebookUrl: String? = null,

  @field:Size(max = 255)
  val website: String? = null,

  @field:Min(0)
  val avgPriceAr: Int? = null,

  // an owner can close or reopen, but cannot self-approve out of 'pending'
  @field:Pattern(regexp = "^(active|closed)$", message = "must be 'active' or 'closed'")
  val status: String? = null,

  @field:Valid
  val amenities: AmenitiesUpdate? = null,
)

data class AmenitiesUpdate(
  val delivery: Boolean? = null,
  val parking: Boolean? = null,
  val wifi: Boolean? = null,
  val wheelchairAccess: Boolean? = null,
  val airConditioning: Boolean? = null,
  val terrace: Boolean? = null,
  val familyFriendly: Boolean? = null,
  val romantic: Boolean? = null,
  val studentFriendly: Boolean? = null,
  val scenicView: Boolean? = null,
  val open24h: Boolean? = null,
)

// PUT /v1/business/establishments/{id}/opening-hours — full replacement;
// multiple intervals per day are allowed (book ch. 6.1), day 0 = Monday
data class OpeningIntervalInput(
  @field:Min(0)
  @field:Max(6)
  val dayOfWeek: Int = 0,

  @field:Pattern(regexp = "^([01][0-9]|2[0-3]):[0-5][0-9]$", message = "must be HH:mm")
  val opensAt: String = "",

  @field:Pattern(regexp = "^([01][0-9]|2[0-3]):[0-5][0-9]$", message = "must be HH:mm")
  val closesAt: String = "",
)

data class OpeningHoursUpdate(
  @field:Valid
  val intervals: List<OpeningIntervalInput> = emptyList(),
)

// One manager of an establishment (admin back-office view)
data class ManagerItem(
  val userId: UUID,
  val displayName: String,
  val phone: String,
  val grantedAt: Instant,
)
