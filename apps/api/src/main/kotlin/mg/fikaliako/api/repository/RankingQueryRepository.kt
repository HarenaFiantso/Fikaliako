package mg.fikaliako.api.repository

import mg.fikaliako.api.endpoint.rest.model.EstablishmentSummary
import java.time.Instant

/**
 * Read model for the thematic tops (project book ch. 4.6). Every ordering key
 * comes from the establishment_ratings snapshot the nightly job maintains, so
 * rankings are stable between two aggregation runs — "recalculés chaque nuit".
 */
interface RankingQueryRepository {
  /** Best establishments by Bayesian note, optionally narrowed to a type or a cuisine. */
  fun topByBayesian(
    type: String?,
    cuisineCode: String?,
    minReviews: Int,
    limit: Int,
  ): List<EstablishmentSummary>

  /**
   * Best value for money: the price-fairness and quality criterion notes
   * blended half-half, damped by review volume (n / (n + 10)) so two lucky
   * reviews cannot crown a winner.
   */
  fun topByValue(
    minReviews: Int,
    limit: Int,
  ): List<EstablishmentSummary>

  /** Recently added establishments that already convinced reviewers — "révélations du mois". */
  fun revelations(
    since: Instant,
    minReviews: Int,
    limit: Int,
  ): List<EstablishmentSummary>
}
