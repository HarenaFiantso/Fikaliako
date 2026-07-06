package mg.fikaliako.api.repository

import mg.fikaliako.api.model.Amenities
import mg.fikaliako.api.model.EstablishmentDetail
import mg.fikaliako.api.model.EstablishmentFilters
import mg.fikaliako.api.model.EstablishmentSummary
import mg.fikaliako.api.model.GeoPoint
import mg.fikaliako.api.model.OpeningInterval
import mg.fikaliako.api.model.RatingSummary
import mg.fikaliako.api.model.ReferentialItem
import mg.fikaliako.api.util.Cursor
import mg.fikaliako.api.util.OpeningHours
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.Instant
import java.util.UUID

@Repository
class EstablishmentRepository(
    private val jdbc: JdbcClient,
) {
    fun findNearby(
        lat: Double,
        lng: Double,
        radiusM: Double,
        filters: EstablishmentFilters,
        limit: Int,
        now: Instant,
    ): List<EstablishmentSummary> {
        val where = WhereBuilder(now)
        where.add("e.status = 'active'")
        where.add(
            "ST_DWithin(e.position, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radius)",
        )
        where.applyFilters(filters)

        val sql =
            """
            SELECT $SUMMARY_COLUMNS,
                   ST_Distance(e.position, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography) AS distance_m
            FROM establishments e
            LEFT JOIN establishment_ratings r ON r.establishment_id = e.id
            WHERE ${where.sql()}
            ORDER BY e.avg_price_ar ASC NULLS LAST, distance_m ASC
            LIMIT :limit
            """.trimIndent()

        val spec =
            jdbc
                .sql(sql)
                .param("lat", lat)
                .param("lng", lng)
                .param("radius", radiusM)
                .param("limit", limit)
        where.bind(spec)
        return spec.query { rs, _ -> mapSummary(rs, withDistance = true) }.list()
    }

    data class ListRow(
        val summary: EstablishmentSummary,
        val createdAt: Instant,
    )

    fun findList(
        filters: EstablishmentFilters,
        limit: Int,
        cursor: Cursor?,
        now: Instant,
    ): List<ListRow> {
        val where = WhereBuilder(now)
        where.add("e.status = 'active'")
        where.applyFilters(filters)
        if (cursor != null) {
            where.add("(e.created_at, e.id) < (:cursorCreatedAt, :cursorId)")
            where.param("cursorCreatedAt", cursor.createdAt.atOffset(java.time.ZoneOffset.UTC))
            where.param("cursorId", cursor.id)
        }

        val sql =
            """
            SELECT $SUMMARY_COLUMNS, e.created_at
            FROM establishments e
            LEFT JOIN establishment_ratings r ON r.establishment_id = e.id
            WHERE ${where.sql()}
            ORDER BY e.created_at DESC, e.id DESC
            LIMIT :limit
            """.trimIndent()

        val spec = jdbc.sql(sql).param("limit", limit)
        where.bind(spec)
        return spec
            .query { rs, _ ->
                ListRow(mapSummary(rs, withDistance = false), rs.getTimestamp("created_at").toInstant())
            }.list()
    }

    fun findDetail(idOrSlug: String): EstablishmentDetail? {
        val uuid = idOrSlug.toUuidOrNull()
        val sql =
            """
            SELECT e.*, ST_Y(e.position::geometry) AS lat, ST_X(e.position::geometry) AS lng,
                   e.type::text AS type_text, e.status::text AS status_text
            FROM establishments e
            WHERE ${if (uuid != null) "e.id = :key" else "e.slug = :key"}
            """.trimIndent()
        val base =
            jdbc
                .sql(sql)
                .param("key", uuid ?: idOrSlug)
                .query { rs, _ -> mapDetailBase(rs) }
                .optional()
                .orElse(null) ?: return null

        return base.copy(
            openingHours = findOpeningHours(base.id),
            paymentMethods = findPaymentMethods(base.id),
            cuisines = findCuisines(base.id),
            rating = findRating(base.id),
        )
    }

    private fun findOpeningHours(id: UUID): List<OpeningInterval> =
        jdbc
            .sql(
                """
                SELECT day_of_week, to_char(opens_at, 'HH24:MI') AS opens_at,
                       to_char(closes_at, 'HH24:MI') AS closes_at
                FROM opening_hours WHERE establishment_id = :id
                ORDER BY day_of_week, opens_at
                """.trimIndent(),
            ).param("id", id)
            .query { rs, _ ->
                OpeningInterval(rs.getInt("day_of_week"), rs.getString("opens_at"), rs.getString("closes_at"))
            }.list()

    private fun findPaymentMethods(id: UUID): List<ReferentialItem> =
        jdbc
            .sql(
                """
                SELECT p.code, p.label_fr, p.label_mg
                FROM establishment_payment_methods epm
                JOIN payment_methods p ON p.code = epm.payment_method_code
                WHERE epm.establishment_id = :id
                ORDER BY p.sort_order
                """.trimIndent(),
            ).param("id", id)
            .query { rs, _ -> mapReferential(rs) }
            .list()

    private fun findCuisines(id: UUID): List<ReferentialItem> =
        jdbc
            .sql(
                """
                SELECT c.code, c.label_fr, c.label_mg
                FROM establishment_cuisines ec
                JOIN cuisines c ON c.code = ec.cuisine_code
                WHERE ec.establishment_id = :id
                ORDER BY c.sort_order
                """.trimIndent(),
            ).param("id", id)
            .query { rs, _ -> mapReferential(rs) }
            .list()

    private fun findRating(id: UUID): RatingSummary =
        jdbc
            .sql(
                """
                SELECT review_count, avg_global, avg_quality, avg_price,
                       avg_cleanliness, avg_speed, avg_welcome, bayesian_note
                FROM establishment_ratings WHERE establishment_id = :id
                """.trimIndent(),
            ).param("id", id)
            .query { rs, _ ->
                RatingSummary(
                    count = rs.getInt("review_count"),
                    avgGlobal = rs.getBigDecimal("avg_global"),
                    avgQuality = rs.getBigDecimal("avg_quality"),
                    avgPrice = rs.getBigDecimal("avg_price"),
                    avgCleanliness = rs.getBigDecimal("avg_cleanliness"),
                    avgSpeed = rs.getBigDecimal("avg_speed"),
                    avgWelcome = rs.getBigDecimal("avg_welcome"),
                    bayesianNote = rs.getBigDecimal("bayesian_note"),
                )
            }.optional()
            .orElse(RatingSummary(0, null, null, null, null, null, null, null))

    private fun mapSummary(
        rs: ResultSet,
        withDistance: Boolean,
    ) = EstablishmentSummary(
        id = rs.getObject("id", UUID::class.java),
        slug = rs.getString("slug"),
        name = rs.getString("name"),
        type = rs.getString("type"),
        position = GeoPoint(rs.getDouble("lat"), rs.getDouble("lng")),
        avgPriceAr = rs.getObject("avg_price_ar") as Int?,
        verified = rs.getBoolean("verified"),
        status = rs.getString("status"),
        ratingAvg = rs.getBigDecimal("rating_avg"),
        ratingCount = rs.getInt("rating_count"),
        distanceM = if (withDistance) rs.getDouble("distance_m") else null,
    )

    private fun mapReferential(rs: ResultSet) =
        ReferentialItem(rs.getString("code"), rs.getString("label_fr"), rs.getString("label_mg"))

    private fun mapDetailBase(rs: ResultSet) =
        EstablishmentDetail(
            id = rs.getObject("id", UUID::class.java),
            slug = rs.getString("slug"),
            name = rs.getString("name"),
            type = rs.getString("type_text"),
            position = GeoPoint(rs.getDouble("lat"), rs.getDouble("lng")),
            address = rs.getString("address"),
            district = rs.getString("district"),
            city = rs.getString("city"),
            phone = rs.getString("phone"),
            whatsapp = rs.getString("whatsapp"),
            facebookUrl = rs.getString("facebook_url"),
            website = rs.getString("website"),
            avgPriceAr = rs.getObject("avg_price_ar") as Int?,
            verified = rs.getBoolean("verified"),
            status = rs.getString("status_text"),
            openNow = false,
            amenities =
                Amenities(
                    delivery = rs.getBoolean("delivery"),
                    parking = rs.getBoolean("parking"),
                    wifi = rs.getBoolean("wifi"),
                    wheelchairAccess = rs.getBoolean("wheelchair_access"),
                    airConditioning = rs.getBoolean("air_conditioning"),
                    terrace = rs.getBoolean("terrace"),
                    familyFriendly = rs.getBoolean("family_friendly"),
                    romantic = rs.getBoolean("romantic"),
                    studentFriendly = rs.getBoolean("student_friendly"),
                    scenicView = rs.getBoolean("scenic_view"),
                    open24h = rs.getBoolean("open_24h"),
                ),
            openingHours = emptyList(),
            paymentMethods = emptyList(),
            cuisines = emptyList(),
            rating = RatingSummary(0, null, null, null, null, null, null, null),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            updatedAt = rs.getTimestamp("updated_at").toInstant(),
        )

    companion object {
        private const val SUMMARY_COLUMNS =
            "e.id, e.slug, e.name, e.type::text AS type, " +
                "ST_Y(e.position::geometry) AS lat, ST_X(e.position::geometry) AS lng, " +
                "e.avg_price_ar, e.verified, e.status::text AS status, " +
                "r.avg_global AS rating_avg, COALESCE(r.review_count, 0) AS rating_count"
    }
}

private fun String.toUuidOrNull(): UUID? =
    try {
        UUID.fromString(this)
    } catch (_: IllegalArgumentException) {
        null
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
            add("e.type = :type::establishment_type")
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

    fun bind(spec: JdbcClient.StatementSpec) {
        params.forEach { (k, v) -> spec.param(k, v) }
    }
}
