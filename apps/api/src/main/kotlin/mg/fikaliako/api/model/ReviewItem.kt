package mg.fikaliako.api.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

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
