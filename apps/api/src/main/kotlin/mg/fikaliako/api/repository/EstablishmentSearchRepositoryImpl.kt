package mg.fikaliako.api.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.Query
import jakarta.persistence.Tuple
import mg.fikaliako.api.endpoint.rest.model.EstablishmentFilters
import mg.fikaliako.api.endpoint.rest.model.EstablishmentSummary
import mg.fikaliako.api.endpoint.rest.model.GeoPoint
import mg.fikaliako.api.util.Cursor
import mg.fikaliako.api.util.OpeningHours
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

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
    return tuples(query).map { toSummary(it, withDistance = true) }
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
    return tuples(
      query,
    ).map { EstablishmentListRow(toSummary(it, withDistance = false), instantOf(it["created_at"])) }
  }

  @Suppress("UNCHECKED_CAST")
  private fun tuples(query: Query): List<Tuple> = query.resultList as List<Tuple>

  private fun toSummary(
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
    distanceM = if (withDistance) (t["distance_m"] as Number).toDouble() else null,
  )

  private fun instantOf(value: Any?): Instant =
    when (value) {
      is Instant -> value
      is OffsetDateTime -> value.toInstant()
      is java.sql.Timestamp -> value.toInstant()
      else -> error("Unexpected created_at type: ${value?.javaClass}")
    }

  private companion object {
    const val POINT = "CAST(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) AS geography)"

    const val SUMMARY_COLUMNS =
      "e.id AS id, e.slug AS slug, e.name AS name, CAST(e.type AS text) AS type, " +
        "ST_Y(CAST(e.position AS geometry)) AS lat, ST_X(CAST(e.position AS geometry)) AS lng, " +
        "e.avg_price_ar AS avg_price_ar, e.verified AS verified, CAST(e.status AS text) AS status, " +
        "r.avg_global AS rating_avg, COALESCE(r.review_count, 0) AS rating_count"
  }
}

private class WhereBuilder(
  private val now: Instant,
) {
  private val clauses = mutableListOf<String>()
  private val params = mutableMapOf<String, Any>()

  fun add(clause: String) {
    clauses += clause
  }

  fun param(
    name: String,
    value: Any,
  ) {
    params[name] = value
  }

  fun applyFilters(f: EstablishmentFilters) {
    f.type?.let {
      add("e.type = CAST(:type AS establishment_type)")
      param("type", it)
    }
    f.minPrice?.let {
      add("e.avg_price_ar >= :minPrice")
      param("minPrice", it)
    }
    f.maxPrice?.let {
      add("e.avg_price_ar <= :maxPrice")
      param("maxPrice", it)
    }
    f.amenities.forEach { col ->
      require(col in EstablishmentFilters.AMENITY_COLUMNS) { "Unknown amenity filter: $col" }
      add("e.$col = TRUE")
    }
    f.cuisine?.let {
      add(
        "EXISTS (SELECT 1 FROM establishment_cuisines ec " +
          "WHERE ec.establishment_id = e.id AND ec.cuisine_code = :cuisine)",
      )
      param("cuisine", it)
    }
    f.payment?.let {
      if (it == "mobile") {
        add(
          "EXISTS (SELECT 1 FROM establishment_payment_methods epm " +
            "JOIN payment_methods p ON p.code = epm.payment_method_code " +
            "WHERE epm.establishment_id = e.id AND p.is_mobile_money)",
        )
      } else {
        add(
          "EXISTS (SELECT 1 FROM establishment_payment_methods epm " +
            "WHERE epm.establishment_id = e.id AND epm.payment_method_code = :payment)",
        )
        param("payment", it)
      }
    }
    if (f.openNow) applyOpenNow()
  }

  private fun applyOpenNow() {
    val local = now.atZone(OpeningHours.ZONE)
    val dow = local.dayOfWeek.value - 1
    val pdow = (dow + 6) % 7
    add(
      """
      (e.open_24h OR EXISTS (
          SELECT 1 FROM opening_hours oh WHERE oh.establishment_id = e.id AND (
              (oh.day_of_week = :dow AND oh.opens_at < oh.closes_at AND :nowTime >= oh.opens_at AND :nowTime < oh.closes_at)
              OR (oh.day_of_week = :dow AND oh.opens_at >= oh.closes_at AND :nowTime >= oh.opens_at)
              OR (oh.day_of_week = :pdow AND oh.opens_at >= oh.closes_at AND :nowTime < oh.closes_at)
          )
      ))
      """.trimIndent(),
    )
    param("dow", dow)
    param("pdow", pdow)
    param("nowTime", java.sql.Time.valueOf(local.toLocalTime().withNano(0)))
  }

  fun sql(): String = clauses.joinToString(" AND ")

  fun bind(query: Query) {
    params.forEach { (k, v) -> query.setParameter(k, v) }
  }
}
