package mg.fikaliako.api.repository

import jakarta.persistence.Query
import jakarta.persistence.Tuple
import mg.fikaliako.api.endpoint.rest.model.EstablishmentSummary
import mg.fikaliako.api.endpoint.rest.model.GeoPoint
import java.math.BigDecimal
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Shared column list and row mapping for the native discovery queries
 * (nearby, list, text search): one place defines what an
 * [EstablishmentSummary] looks like in SQL.
 */
internal object SummaryRowMapping {
  const val POINT = "CAST(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) AS geography)"

  const val SUMMARY_COLUMNS =
    "e.id AS id, e.slug AS slug, e.name AS name, CAST(e.type AS text) AS type, " +
      "ST_Y(CAST(e.position AS geometry)) AS lat, ST_X(CAST(e.position AS geometry)) AS lng, " +
      "e.avg_price_ar AS avg_price_ar, e.verified AS verified, CAST(e.status AS text) AS status, " +
      "r.avg_global AS rating_avg, COALESCE(r.review_count, 0) AS rating_count"

  @Suppress("UNCHECKED_CAST")
  fun tuples(query: Query): List<Tuple> = query.resultList as List<Tuple>

  fun toSummary(
    t: Tuple,
    withDistance: Boolean,
  ) = EstablishmentSummary(
    id = t["id"] as UUID,
    slug = t["slug"] as String,
    name = t["name"] as String,
    type = t["type"] as String,
    position = GeoPoint((t["lat"] as Number).toDouble(), (t["lng"] as Number).toDouble()),
    avgPriceAr = (t["avg_price_ar"] as Number?)?.toInt(),
    verified = t["verified"] as Boolean,
    status = t["status"] as String,
    ratingAvg = t["rating_avg"] as BigDecimal?,
    ratingCount = (t["rating_count"] as Number).toInt(),
    distanceM = if (withDistance) (t["distance_m"] as Number?)?.toDouble() else null,
  )

  fun instantOf(value: Any?): Instant =
    when (value) {
      is Instant -> value
      is OffsetDateTime -> value.toInstant()
      is java.sql.Timestamp -> value.toInstant()
      else -> error("Unexpected timestamp type: ${value?.javaClass}")
    }
}
