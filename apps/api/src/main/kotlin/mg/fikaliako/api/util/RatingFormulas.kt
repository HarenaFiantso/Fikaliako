package mg.fikaliako.api.util

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Deterministic rating math for the aggregation job (project book ch. 4.5, 4.6).
 *
 * Plain averages feed the establishment page; the Bayesian note — recency-weighted
 * and damped by review volume — is the only ordering key rankings may use, so an
 * establishment with two 5-star reviews cannot outrank a long-standing 4.6.
 */
object RatingFormulas {
  /**
   * Confidence mass pulling low-volume establishments toward the global mean
   * (ch. 4.6 — "note bayésienne tenant compte du volume d'avis"). An
   * establishment needs about this much accumulated review weight before its
   * own note dominates.
   */
  val CONFIDENCE_WEIGHT: BigDecimal = BigDecimal(10)

  /** Recency tiers: newer reviews weigh more in rankings (ch. 4.5). */
  fun recencyWeight(ageDays: Long): BigDecimal =
    when {
      ageDays <= 30 -> BigDecimal("1.00")
      ageDays <= 90 -> BigDecimal("0.85")
      ageDays <= 365 -> BigDecimal("0.70")
      else -> BigDecimal("0.50")
    }

  /** Plain average, or null when there is nothing to average. */
  fun mean(notes: List<BigDecimal>): BigDecimal? =
    notes
      .takeIf { it.isNotEmpty() }
      ?.let { list -> list.reduce(BigDecimal::add).divide(BigDecimal(list.size), 2, RoundingMode.HALF_UP) }

  /**
   * Bayesian note: (C·m + Σ wᵢ·noteᵢ) / (C + Σ wᵢ) where m is the global mean
   * across all published reviews and wᵢ the recency weight of each review.
   */
  fun bayesianNote(
    weightedNotes: List<WeightedNote>,
    globalMean: BigDecimal,
  ): BigDecimal? {
    if (weightedNotes.isEmpty()) return null
    val weightSum = weightedNotes.map { it.weight }.reduce(BigDecimal::add)
    val weightedSum = weightedNotes.map { it.note * it.weight }.reduce(BigDecimal::add)
    return (CONFIDENCE_WEIGHT * globalMean + weightedSum)
      .divide(CONFIDENCE_WEIGHT + weightSum, 2, RoundingMode.HALF_UP)
  }

  data class WeightedNote(
    val note: BigDecimal,
    val weight: BigDecimal,
  )
}
