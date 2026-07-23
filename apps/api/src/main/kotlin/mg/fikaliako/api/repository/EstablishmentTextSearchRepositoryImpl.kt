package mg.fikaliako.api.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.Tuple
import mg.fikaliako.api.endpoint.rest.model.EstablishmentFilters
import mg.fikaliako.api.endpoint.rest.model.EstablishmentSummary
import mg.fikaliako.api.repository.SummaryRowMapping.POINT
import mg.fikaliako.api.repository.SummaryRowMapping.SUMMARY_COLUMNS
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * pg_trgm adapter for [EstablishmentTextSearchRepository]. `word_similarity`
 * scores the query against the best-matching words of each field, which
 * absorbs typos ("romzava" still finds "Romazava"). Fields are weighted —
 * name above cuisine labels above district above city — and a synonym hit on
 * cuisine or type scores a fixed 0.75 so "romazava" surfaces Malagasy
 * gargottes whose names never mention the dish.
 *
 * The catalogue is a few thousand rows (book ch. 15), so scoring scans the
 * active establishments; when volume outgrows this, the Meilisearch adapter
 * (ch. 7.1) replaces this class behind the port.
 */
@Repository
class EstablishmentTextSearchRepositoryImpl(
  @PersistenceContext private val em: EntityManager,
) : EstablishmentTextSearchRepository {
  override fun searchText(
    query: String,
    synonymCuisines: Set<String>,
    synonymTypes: Set<String>,
    filters: EstablishmentFilters,
    geo: GeoSearchContext?,
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

    val relevanceArms =
      buildList {
        add("word_similarity(:q, e.name)")
        add(
          "COALESCE((SELECT MAX(GREATEST(word_similarity(:q, c.label_fr), word_similarity(:q, c.label_mg))) " +
            "FROM establishment_cuisines ec JOIN cuisines c ON c.code = ec.cuisine_code " +
            "WHERE ec.establishment_id = e.id), 0) * 0.9",
        )
        add("word_similarity(:q, COALESCE(e.district, '')) * 0.8")
        add("word_similarity(:q, e.city) * 0.5")
        if (synonymCuisines.isNotEmpty()) {
          add(
            "CASE WHEN EXISTS (SELECT 1 FROM establishment_cuisines sec " +
              "WHERE sec.establishment_id = e.id " +
              "AND sec.cuisine_code IN (${where.inList("synCuisine", synonymCuisines)})) " +
              "THEN $SYNONYM_SCORE ELSE 0 END",
          )
        }
        if (synonymTypes.isNotEmpty()) {
          add(
            "CASE WHEN CAST(e.type AS text) IN (${where.inList("synType", synonymTypes)}) " +
              "THEN $SYNONYM_SCORE ELSE 0 END",
          )
        }
      }

    val distanceExpr = if (geo != null) "ST_Distance(e.position, $POINT)" else "CAST(NULL AS double precision)"
    val sql =
      """
      SELECT ranked.* FROM (
          SELECT $SUMMARY_COLUMNS,
                 r.bayesian_note AS rating_bayesian,
                 $distanceExpr AS distance_m,
                 GREATEST(${relevanceArms.joinToString(", ")}) AS relevance
          FROM establishments e
          LEFT JOIN establishment_ratings r ON r.establishment_id = e.id
          WHERE ${where.sql()}
      ) ranked
      WHERE ranked.relevance >= $RELEVANCE_THRESHOLD
      ORDER BY ranked.relevance DESC, ranked.rating_bayesian DESC NULLS LAST,
               ranked.distance_m ASC NULLS LAST, ranked.id
      LIMIT :limit OFFSET :offset
      """.trimIndent()

    val jpaQuery =
      em
        .createNativeQuery(sql, Tuple::class.java)
        .setParameter("q", query)
        .setParameter("limit", limit)
        .setParameter("offset", offset)
    if (geo != null) {
      jpaQuery.setParameter("lat", geo.lat).setParameter("lng", geo.lng)
      geo.radiusM?.let { jpaQuery.setParameter("radius", it) }
    }
    where.bind(jpaQuery)
    return SummaryRowMapping.tuples(jpaQuery).map { SummaryRowMapping.toSummary(it, withDistance = geo != null) }
  }

  private companion object {
    /** Minimum relevance for a row to count as a hit at all. */
    const val RELEVANCE_THRESHOLD = 0.30

    /** Fixed score for a synonym-driven cuisine/type match — above noise, below a direct name hit. */
    const val SYNONYM_SCORE = 0.75
  }
}
