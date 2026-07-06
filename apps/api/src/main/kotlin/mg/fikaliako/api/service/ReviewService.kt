package mg.fikaliako.api.service

import mg.fikaliako.api.entity.Review
import mg.fikaliako.api.entity.ReviewStatus
import mg.fikaliako.api.exception.BadRequestException
import mg.fikaliako.api.exception.NotFoundException
import mg.fikaliako.api.model.Page
import mg.fikaliako.api.model.ReviewItem
import mg.fikaliako.api.repository.EstablishmentRepository
import mg.fikaliako.api.repository.ReviewRepository
import mg.fikaliako.api.util.Cursor
import org.springframework.data.domain.Limit
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ReviewService(
  private val reviewRepository: ReviewRepository,
  private val establishmentRepository: EstablishmentRepository,
) {
  @Transactional(readOnly = true)
  fun listForEstablishment(
    establishmentId: UUID,
    limit: Int?,
    cursorValue: String?,
  ): Page<ReviewItem> {
    if (!establishmentRepository.existsById(establishmentId)) {
      throw NotFoundException("Establishment '$establishmentId' not found.")
    }
    val cappedLimit = clampLimit(limit)
    val cursor = cursorValue?.let { Cursor.decode(it) }
    val fetch = Limit.of(cappedLimit + 1)
    val rows =
      if (cursor == null) {
        reviewRepository.findPublished(establishmentId, ReviewStatus.PUBLISHED, fetch)
      } else {
        reviewRepository.findPublishedAfter(
          establishmentId,
          ReviewStatus.PUBLISHED,
          cursor.createdAt,
          cursor.id,
          fetch,
        )
      }
    val page = rows.take(cappedLimit)
    val next =
      if (rows.size > cappedLimit) {
        page.lastOrNull()?.let { Cursor(requireNotNull(it.createdAt), requireNotNull(it.id)).encode() }
      } else {
        null
      }
    return Page(page.map { toItem(it) }, next)
  }

  private fun toItem(r: Review): ReviewItem =
    ReviewItem(
      id = requireNotNull(r.id),
      authorName = requireNotNull(r.author).displayName,
      ratingQuality = r.ratingQuality.toInt(),
      ratingPrice = r.ratingPrice.toInt(),
      ratingCleanliness = r.ratingCleanliness.toInt(),
      ratingSpeed = r.ratingSpeed.toInt(),
      ratingWelcome = r.ratingWelcome.toInt(),
      globalNote = r.globalNote,
      comment = r.comment,
      createdAt = requireNotNull(r.createdAt),
    )

  private fun clampLimit(limit: Int?): Int {
    val value = limit ?: DEFAULT_LIMIT
    if (value < 1) throw BadRequestException("limit must be at least 1.")
    return value.coerceAtMost(MAX_PAGE_SIZE)
  }

  companion object {
    const val DEFAULT_LIMIT = 20
    const val MAX_PAGE_SIZE = 100
  }
}
