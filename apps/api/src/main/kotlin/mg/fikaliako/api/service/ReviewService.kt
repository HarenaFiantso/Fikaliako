package mg.fikaliako.api.service
import mg.fikaliako.api.endpoint.rest.model.Page
import mg.fikaliako.api.endpoint.rest.model.ReviewInput
import mg.fikaliako.api.endpoint.rest.model.ReviewItem
import mg.fikaliako.api.model.Establishment
import mg.fikaliako.api.model.Review
import mg.fikaliako.api.model.ReviewStatus
import mg.fikaliako.api.model.exception.BadRequestException
import mg.fikaliako.api.model.exception.ConflictException
import mg.fikaliako.api.model.exception.NotFoundException
import mg.fikaliako.api.model.exception.UnauthorizedException
import mg.fikaliako.api.repository.EstablishmentRepository
import mg.fikaliako.api.repository.ReviewRepository
import mg.fikaliako.api.repository.UserAccountRepository
import mg.fikaliako.api.util.Cursor
import org.springframework.data.domain.Limit
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.util.UUID

@Service
class ReviewService(
  private val reviewRepository: ReviewRepository,
  private val establishmentRepository: EstablishmentRepository,
  private val userRepository: UserAccountRepository,
  private val clock: Clock,
) {
  @Transactional
  fun create(
    establishmentId: UUID,
    authorId: UUID,
    input: ReviewInput,
  ): ReviewItem {
    val establishment =
      establishmentRepository.findById(establishmentId).orElseThrow {
        NotFoundException("Establishment '$establishmentId' not found.")
      }
    val author =
      userRepository.findById(authorId).orElseThrow {
        UnauthorizedException("Account no longer exists.")
      }
    if (reviewRepository.existsByEstablishmentIdAndAuthorId(establishmentId, authorId)) {
      throw ConflictException("You have already reviewed this establishment.")
    }
    val review =
      Review(
        id = UUID.randomUUID(),
        establishment = establishment,
        author = author,
        ratingQuality = input.ratingQuality.toShort(),
        ratingPrice = input.ratingPrice.toShort(),
        ratingCleanliness = input.ratingCleanliness.toShort(),
        ratingSpeed = input.ratingSpeed.toShort(),
        ratingWelcome = input.ratingWelcome.toShort(),
        comment = input.comment?.trim()?.takeIf { it.isNotEmpty() },
        status = ReviewStatus.PUBLISHED,
        createdAt = clock.instant(),
      )
    // global_note stays DB-generated; the echo computes the same weighted mean
    // (quality ×2, weights sum to 6 — book ch. 4.4)
    review.globalNote = weightedGlobalNote(input)
    reviewRepository.save(review)
    return toItem(review)
  }

  private fun weightedGlobalNote(input: ReviewInput): BigDecimal =
    BigDecimal(
      input.ratingQuality * 2 + input.ratingPrice + input.ratingCleanliness +
        input.ratingSpeed + input.ratingWelcome,
    ).divide(BigDecimal(6), 2, RoundingMode.HALF_UP)

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
