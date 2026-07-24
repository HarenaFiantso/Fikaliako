package mg.fikaliako.api.endpoint.rest.model

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class EstablishmentProposal(
  @field:NotBlank
  @field:Size(min = 2, max = 120)
  val name: String = "",

  @field:NotBlank
  val type: String = "",

  @field:NotNull
  @field:Valid
  val position: GeoPoint? = null,

  @field:Size(max = 255)
  val address: String? = null,

  @field:Size(max = 120)
  val district: String? = null,

  @field:Size(max = 120)
  val city: String? = null,

  @field:Size(max = 30)
  val phone: String? = null,

  @field:PositiveOrZero
  val avgPriceAr: Int? = null,

  @field:Size(max = 500)
  val comment: String? = null,
)

data class ContributionReceipt(
  val id: UUID,
  val status: String,
  val createdAt: Instant,
)

data class ContributionItem(
  val id: UUID,
  val type: String,
  val status: String,
  val payload: Map<String, Any?>,
  val establishmentId: UUID?,
  val authorName: String?,
  val confirmationsCount: Int,
  val resolutionNote: String?,
  val createdAt: Instant,
  val resolvedAt: Instant?,
)

data class RejectContributionRequest(
  @field:Size(max = 500)
  val reason: String? = null,
)
