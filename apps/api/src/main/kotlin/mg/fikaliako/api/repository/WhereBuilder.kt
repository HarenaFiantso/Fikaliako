package mg.fikaliako.api.repository

import jakarta.persistence.Query
import mg.fikaliako.api.endpoint.rest.model.EstablishmentFilters
import mg.fikaliako.api.util.OpeningHours
import java.time.Instant

/**
 * Assembles the WHERE clause shared by the native discovery queries. Every
 * project-book filter (ch. 4.3) maps to one parameterised clause so the twelve
 * boolean filters, budget, type, cuisine and payment all resolve in one query.
 */
internal class WhereBuilder(
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

  /** Expands a value list into named parameters, e.g. `IN (:type0, :type1)`. */
  fun inList(
    prefix: String,
    values: Collection<String>,
  ): String =
    values
      .mapIndexed { i, value -> ":$prefix$i".also { params["$prefix$i"] = value } }
      .joinToString(", ")

  fun applyFilters(f: EstablishmentFilters) {
    if (f.types.isNotEmpty()) {
      add("CAST(e.type AS text) IN (${inList("type", f.types)})")
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
