package mg.fikaliako.api.service

import mg.fikaliako.api.model.EstablishmentRating
import mg.fikaliako.api.model.ReviewStatus
import mg.fikaliako.api.repository.EstablishmentRatingRepository
import mg.fikaliako.api.repository.ReviewRepository
import mg.fikaliako.api.util.RatingFormulas
import mg.fikaliako.api.util.RatingFormulas.WeightedNote
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Maintains the denormalised establishment_ratings snapshot (project book ch. 4.5, 4.6):
 * plain per-criterion averages for the establishment page, plus the Bayesian note and
 * the recent-review count that discovery ordering and rankings read.
 */
@Service
class RatingAggregationService(
  private val reviewRepository: ReviewRepository,
  private val ratingRepository: EstablishmentRatingRepository,
  private val clock: Clock,
) {
  /**
   * Nightly full recompute — rankings are refreshed each night (ch. 4.6). Scheduled
   * in Indian/Antananarivo so the run lands outside the meal-window availability
   * objective (ch. 9).
   */
  @Scheduled(cron = "\${fikaliako.jobs.rating-aggregation-cron:0 0 3 * * *}", zone = "Indian/Antananarivo")
  @Transactional
  fun recomputeAll() {
    val globalMean = globalMean()
    val ids =
      buildSet {
        addAll(reviewRepository.establishmentIdsWithReviews(ReviewStatus.published))
        // Also revisit establishments whose reviews were since hidden, so stale
        // snapshots fall back to zero instead of surviving forever.
        addAll(ratingRepository.establishmentIds())
      }
    ids.forEach { recomputeOne(it, globalMean) }
  }

  /** Refresh one establishment right after a review lands, so its page shows fresh averages. */
  @Transactional
  fun recompute(establishmentId: UUID) {
    recomputeOne(establishmentId, globalMean())
  }

  private fun globalMean(): BigDecimal? = reviewRepository.averageGlobalNote(ReviewStatus.published)?.let { BigDecimal.valueOf(it) }

  private fun recomputeOne(
    establishmentId: UUID,
    globalMean: BigDecimal?,
  ) {
    val now = clock.instant()
    val reviews = reviewRepository.findByEstablishmentIdAndStatus(establishmentId, ReviewStatus.published)
    val rating = ratingRepository.findById(establishmentId).orElseGet { EstablishmentRating(establishmentId) }
    rating.reviewCount = reviews.size
    rating.recentReviewCount = reviews.count { ageDays(it.createdAt, now) <= RECENT_WINDOW_DAYS }
    rating.avgQuality = RatingFormulas.mean(reviews.map { BigDecimal(it.ratingQuality.toInt()) })
    rating.avgPrice = RatingFormulas.mean(reviews.map { BigDecimal(it.ratingPrice.toInt()) })
    rating.avgCleanliness = RatingFormulas.mean(reviews.map { BigDecimal(it.ratingCleanliness.toInt()) })
    rating.avgSpeed = RatingFormulas.mean(reviews.map { BigDecimal(it.ratingSpeed.toInt()) })
    rating.avgWelcome = RatingFormulas.mean(reviews.map { BigDecimal(it.ratingWelcome.toInt()) })
    rating.avgGlobal = RatingFormulas.mean(reviews.map { it.globalNote })
    rating.bayesianNote =
      globalMean?.let { mean ->
        RatingFormulas.bayesianNote(
          reviews.map { WeightedNote(it.globalNote, RatingFormulas.recencyWeight(ageDays(it.createdAt, now))) },
          mean,
        )
      }
    rating.computedAt = now
    ratingRepository.save(rating)
  }

  private fun ageDays(
    createdAt: Instant?,
    now: Instant,
  ): Long = Duration.between(createdAt ?: now, now).toDays()

  companion object {
    /** Window defining a "recent" review — the discovery-score signal of ch. 4.2. */
    const val RECENT_WINDOW_DAYS = 90L
  }
}
