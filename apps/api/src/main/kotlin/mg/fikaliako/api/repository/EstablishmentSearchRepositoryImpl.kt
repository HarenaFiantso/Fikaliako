package mg.fikaliako.api.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.Tuple
import mg.fikaliako.api.endpoint.rest.model.EstablishmentFilters
import mg.fikaliako.api.endpoint.rest.model.EstablishmentSummary
import mg.fikaliako.api.repository.SummaryRowMapping.POINT
import mg.fikaliako.api.repository.SummaryRowMapping.SUMMARY_COLUMNS
import mg.fikaliako.api.util.Cursor
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.ZoneOffset

@Repository
class EstablishmentSearchRepositoryImpl(
  @PersistenceContext private val em: EntityManager,
) : EstablishmentSearchRepository {
  override fun searchNearby(
    lat: Double,
    lng: Double,
    radiusM: Double,
    filters: EstablishmentFilters,
    limit: Int,
    now: Instant,
  ): List<EstablishmentSummary> {
    val where = WhereBuilder(now)
    where.add("e.status = 'active'")
    where.add("ST_DWithin(e.position, $POINT, :radius)")
    where.applyFilters(filters)

    val sql =
      """
      SELECT $SUMMARY_COLUMNS, ST_Distance(e.position, $POINT) AS distance_m
      FROM establishments e
      LEFT JOIN establishment_ratings r ON r.establishment_id = e.id
      WHERE ${where.sql()}
      ORDER BY e.avg_price_ar ASC NULLS LAST, distance_m ASC
      LIMIT :limit
      """.trimIndent()

    val query =
      em
        .createNativeQuery(sql, Tuple::class.java)
        .setParameter("lat", lat)
        .setParameter("lng", lng)
        .setParameter("radius", radiusM)
        .setParameter("limit", limit)
    where.bind(query)
    return SummaryRowMapping.tuples(query).map { SummaryRowMapping.toSummary(it, withDistance = true) }
  }

  override fun searchList(
    filters: EstablishmentFilters,
    limit: Int,
    cursor: Cursor?,
    now: Instant,
  ): List<EstablishmentListRow> {
    val where = WhereBuilder(now)
    where.add("e.status = 'active'")
    where.applyFilters(filters)
    if (cursor != null) {
      where.add("(e.created_at, e.id) < (:cursorCreatedAt, :cursorId)")
      where.param("cursorCreatedAt", cursor.createdAt.atOffset(ZoneOffset.UTC))
      where.param("cursorId", cursor.id)
    }

    val sql =
      """
      SELECT $SUMMARY_COLUMNS, e.created_at AS created_at
      FROM establishments e
      LEFT JOIN establishment_ratings r ON r.establishment_id = e.id
      WHERE ${where.sql()}
      ORDER BY e.created_at DESC, e.id DESC
      LIMIT :limit
      """.trimIndent()

    val query = em.createNativeQuery(sql, Tuple::class.java).setParameter("limit", limit)
    where.bind(query)
    return SummaryRowMapping.tuples(query).map {
      EstablishmentListRow(
        SummaryRowMapping.toSummary(it, withDistance = false),
        SummaryRowMapping.instantOf(it["created_at"]),
      )
    }
  }

  override fun searchDiscovery(
    geo: GeoSearchContext?,
    filters: EstablishmentFilters,
    limit: Int,
    offset: Int,
    now: Instant,
  ): List<EstablishmentSummary> {
    val where = WhereBuilder(now)
    where.add("e.status = 'active'")
    where.applyFilters(filters)
    if (geo?.radiusM != null) {
      where.add("ST_DWithin(e.position, $POINT, :radius)")
    }

    // Ch. 4.2 management rule — a deterministic score mixing the (Bayesian)
    // note, the recent review volume and the budget adequacy. `:maxPrice` is
    // already bound by the budget filter when one applies.
    val budgetFit =
      if (filters.maxPrice != null) {
        "CASE WHEN e.avg_price_ar IS NULL THEN 0 " +
          "ELSE GREATEST(0, CAST(:maxPrice - e.avg_price_ar AS double precision)) / :maxPrice END"
      } else {
        "0"
      }
    val score =
      "0.5 * COALESCE(r.bayesian_note, 0) / 5 " +
        "+ 0.3 * LEAST(COALESCE(r.recent_review_count, 0), 20) / 20.0 " +
        "+ 0.2 * ($budgetFit)"

    val distanceExpr = if (geo != null) "ST_Distance(e.position, $POINT)" else "CAST(NULL AS double precision)"
    val sql =
      """
      SELECT $SUMMARY_COLUMNS, $distanceExpr AS distance_m, ($score) AS discovery_score
      FROM establishments e
      LEFT JOIN establishment_ratings r ON r.establishment_id = e.id
      WHERE ${where.sql()}
      ORDER BY discovery_score DESC, distance_m ASC NULLS LAST, e.id
      LIMIT :limit OFFSET :offset
      """.trimIndent()

    val query =
      em
        .createNativeQuery(sql, Tuple::class.java)
        .setParameter("limit", limit)
        .setParameter("offset", offset)
    if (geo != null) {
      query.setParameter("lat", geo.lat).setParameter("lng", geo.lng)
      geo.radiusM?.let { query.setParameter("radius", it) }
    }
    where.bind(query)
    return SummaryRowMapping.tuples(query).map { SummaryRowMapping.toSummary(it, withDistance = geo != null) }
  }
}
