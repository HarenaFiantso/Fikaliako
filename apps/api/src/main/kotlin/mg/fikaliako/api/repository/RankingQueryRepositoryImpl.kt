package mg.fikaliako.api.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.Tuple
import mg.fikaliako.api.endpoint.rest.model.EstablishmentSummary
import mg.fikaliako.api.repository.SummaryRowMapping.SUMMARY_COLUMNS
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.ZoneOffset

@Repository
class RankingQueryRepositoryImpl(
  @PersistenceContext private val em: EntityManager,
) : RankingQueryRepository {
  override fun topByBayesian(
    type: String?,
    cuisineCode: String?,
    minReviews: Int,
    limit: Int,
  ): List<EstablishmentSummary> {
    val clauses = StringBuilder()
    if (type != null) clauses.append(" AND CAST(e.type AS text) = :type")
    if (cuisineCode != null) {
      clauses.append(
        " AND EXISTS (SELECT 1 FROM establishment_cuisines ec" +
          " WHERE ec.establishment_id = e.id AND ec.cuisine_code = :cuisine)",
      )
    }
    val sql =
      """
      SELECT $SUMMARY_COLUMNS
      FROM establishments e
      JOIN establishment_ratings r ON r.establishment_id = e.id
      WHERE e.status = 'active' AND r.review_count >= :minReviews$clauses
      ORDER BY r.bayesian_note DESC NULLS LAST, r.review_count DESC, e.id
      LIMIT :limit
      """.trimIndent()
    val query =
      em
        .createNativeQuery(sql, Tuple::class.java)
        .setParameter("minReviews", minReviews)
        .setParameter("limit", limit)
    type?.let { query.setParameter("type", it) }
    cuisineCode?.let { query.setParameter("cuisine", it) }
    return SummaryRowMapping.tuples(query).map { SummaryRowMapping.toSummary(it, withDistance = false) }
  }

  override fun topByValue(
    minReviews: Int,
    limit: Int,
  ): List<EstablishmentSummary> {
    val sql =
      """
      SELECT $SUMMARY_COLUMNS
      FROM establishments e
      JOIN establishment_ratings r ON r.establishment_id = e.id
      WHERE e.status = 'active' AND r.review_count >= :minReviews
      ORDER BY (r.review_count / (r.review_count + 10.0)) * (0.5 * r.avg_price + 0.5 * r.avg_quality) DESC,
               r.review_count DESC, e.id
      LIMIT :limit
      """.trimIndent()
    val query =
      em
        .createNativeQuery(sql, Tuple::class.java)
        .setParameter("minReviews", minReviews)
        .setParameter("limit", limit)
    return SummaryRowMapping.tuples(query).map { SummaryRowMapping.toSummary(it, withDistance = false) }
  }

  override fun revelations(
    since: Instant,
    minReviews: Int,
    limit: Int,
  ): List<EstablishmentSummary> {
    val sql =
      """
      SELECT $SUMMARY_COLUMNS
      FROM establishments e
      JOIN establishment_ratings r ON r.establishment_id = e.id
      WHERE e.status = 'active' AND r.review_count >= :minReviews AND e.created_at >= :since
      ORDER BY r.bayesian_note DESC NULLS LAST, e.created_at DESC, e.id
      LIMIT :limit
      """.trimIndent()
    val query =
      em
        .createNativeQuery(sql, Tuple::class.java)
        .setParameter("minReviews", minReviews)
        .setParameter("since", since.atOffset(ZoneOffset.UTC))
        .setParameter("limit", limit)
    return SummaryRowMapping.tuples(query).map { SummaryRowMapping.toSummary(it, withDistance = false) }
  }
}
