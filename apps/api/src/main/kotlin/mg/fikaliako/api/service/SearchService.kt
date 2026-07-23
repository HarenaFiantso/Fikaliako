package mg.fikaliako.api.service

import mg.fikaliako.api.endpoint.rest.model.EstablishmentFilters
import mg.fikaliako.api.endpoint.rest.model.EstablishmentSummary
import mg.fikaliako.api.endpoint.rest.model.SearchInterpretation
import mg.fikaliako.api.endpoint.rest.model.SearchPage
import mg.fikaliako.api.model.exception.BadRequestException
import mg.fikaliako.api.repository.EstablishmentRepository
import mg.fikaliako.api.repository.GeoSearchContext
import mg.fikaliako.api.util.OffsetCursor
import mg.fikaliako.api.util.TextNormalization
import org.springframework.stereotype.Service
import java.time.Clock

/**
 * Search (project book ch. 4.2): the four combinable modes behind one endpoint.
 * Text goes through typo-tolerant, FR/MG synonym-aware matching; the smart
 * interpreter first turns natural-language phrases ("j'ai faim", "pas cher")
 * into filters, and a filter-only smart query is ranked by the deterministic
 * discovery score instead of text relevance. Budget and distance arrive as
 * plain parameters. Explicit query parameters always win over interpreted ones.
 */
@Service
class SearchService(
  private val repository: EstablishmentRepository,
  private val clock: Clock,
) {
  fun search(
    rawQuery: String,
    filters: EstablishmentFilters,
    lat: Double?,
    lng: Double?,
    radiusM: Double?,
    limit: Int?,
    cursorValue: String?,
  ): SearchPage {
    val query = TextNormalization.normalize(rawQuery)
    if (query.isEmpty()) throw BadRequestException("q must not be blank.")
    if (query.length > MAX_QUERY_LENGTH) throw BadRequestException("q must be at most $MAX_QUERY_LENGTH characters.")
    val cappedLimit = clampLimit(limit)
    val offset = cursorValue?.let { OffsetCursor.decode(it) } ?: 0
    val now = clock.instant()

    val interpretation = SmartQueryInterpreter.interpret(query)
    val merged = mergeFilters(filters, interpretation)
    var geo = resolveGeo(lat, lng, radiusM)
    if (geo != null && geo.radiusM == null && interpretation.radiusM != null) {
      geo = geo.copy(radiusM = interpretation.radiusM)
    }

    val residual = interpretation.residualQuery
    val rows =
      if (interpretation.isSmart && residual.isEmpty()) {
        repository.searchDiscovery(geo, merged, cappedLimit + 1, offset, now)
      } else {
        val expansion = SearchSynonyms.expand(residual)
        repository.searchText(
          residual,
          expansion.cuisines,
          expansion.types,
          merged,
          geo,
          cappedLimit + 1,
          offset,
          now,
        )
      }
    val page = rows.take(cappedLimit)
    val next = if (rows.size > cappedLimit) OffsetCursor.encode(offset + cappedLimit) else null
    return SearchPage(page, next, interpretationEcho(interpretation, merged, geo))
  }

  private fun mergeFilters(
    filters: EstablishmentFilters,
    interpretation: SmartQueryInterpreter.Interpretation,
  ): EstablishmentFilters =
    if (!interpretation.isSmart) {
      filters
    } else {
      filters.copy(
        types = filters.types.ifEmpty { interpretation.types },
        maxPrice = filters.maxPrice ?: interpretation.maxPriceAr,
        payment = filters.payment ?: interpretation.payment,
        amenities = (filters.amenities + interpretation.amenities).distinct(),
        openNow = filters.openNow || interpretation.openNow,
      )
    }

  private fun interpretationEcho(
    interpretation: SmartQueryInterpreter.Interpretation,
    merged: EstablishmentFilters,
    geo: GeoSearchContext?,
  ): SearchInterpretation? =
    if (!interpretation.isSmart) {
      null
    } else {
      SearchInterpretation(
        intents = interpretation.intents,
        openNow = merged.openNow.takeIf { it },
        maxPriceAr = merged.maxPrice,
        types = merged.types.takeIf { it.isNotEmpty() },
        amenities = merged.amenities.takeIf { it.isNotEmpty() },
        payment = merged.payment,
        radiusM = geo?.radiusM,
        ordering = if (interpretation.residualQuery.isEmpty()) ORDERING_DISCOVERY else ORDERING_RELEVANCE,
      )
    }

  private fun resolveGeo(
    lat: Double?,
    lng: Double?,
    radiusM: Double?,
  ): GeoSearchContext? {
    if (lat == null && lng == null) {
      if (radiusM != null) throw BadRequestException("radius requires lat and lng.")
      return null
    }
    if (lat == null || lng == null) throw BadRequestException("lat and lng must be provided together.")
    if (lat < -90 || lat > 90) throw BadRequestException("lat must be in [-90, 90].")
    if (lng < -180 || lng > 180) throw BadRequestException("lng must be in [-180, 180].")
    radiusM?.let {
      if (it <= 0 || it > MAX_RADIUS_M) throw BadRequestException("radius must be in (0, $MAX_RADIUS_M].")
    }
    return GeoSearchContext(lat, lng, radiusM)
  }

  private fun clampLimit(limit: Int?): Int {
    val value = limit ?: DEFAULT_LIMIT
    if (value < 1) throw BadRequestException("limit must be at least 1.")
    return value.coerceAtMost(MAX_PAGE_SIZE)
  }

  companion object {
    const val MAX_QUERY_LENGTH = 120
    const val DEFAULT_LIMIT = 20
    const val MAX_PAGE_SIZE = 100

    /** Book ch. 4.2 — the distance mode goes up to 10 km. */
    const val MAX_RADIUS_M = 10_000.0

    const val ORDERING_RELEVANCE = "relevance"
    const val ORDERING_DISCOVERY = "discovery_score"
  }
}
