package mg.fikaliako.api.endpoint.rest.model

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

// POST /v1/establishments/{id}/reviews — five criteria, 1–5 each (book ch. 4.4)
data class ReviewInput(
  @field:Min(1) @field:Max(5)
  val ratingQuality: Int = 0,

  @field:Min(1) @field:Max(5)
  val ratingPrice: Int = 0,

  @field:Min(1) @field:Max(5)
  val ratingCleanliness: Int = 0,

  @field:Min(1) @field:Max(5)
  val ratingSpeed: Int = 0,

  @field:Min(1) @field:Max(5)
  val ratingWelcome: Int = 0,

  @field:Size(max = 2000)
  val comment: String? = null,
)

data class ReviewItem(
  val id: UUID,
  val authorName: String,
  val ratingQuality: Int,
  val ratingPrice: Int,
  val ratingCleanliness: Int,
  val ratingSpeed: Int,
  val ratingWelcome: Int,
  val globalNote: BigDecimal,
  val comment: String?,
  val createdAt: Instant,
)
