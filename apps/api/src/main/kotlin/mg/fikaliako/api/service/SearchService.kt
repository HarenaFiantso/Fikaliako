package mg.fikaliako.api.service

import mg.fikaliako.api.endpoint.rest.model.EstablishmentFilters
import mg.fikaliako.api.endpoint.rest.model.EstablishmentSummary
import mg.fikaliako.api.endpoint.rest.model.Page
import mg.fikaliako.api.model.exception.BadRequestException
import mg.fikaliako.api.repository.EstablishmentRepository
import mg.fikaliako.api.repository.GeoSearchContext
import mg.fikaliako.api.util.OffsetCursor
import mg.fikaliako.api.util.TextNormalization
import org.springframework.stereotype.Service
import java.time.Clock

/**
 * Text search (project book ch. 4.2): typo-tolerant, FR/MG synonym-aware,
 * combinable with every ch. 4.3 filter and an optional geographic context.
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
  ): Page<EstablishmentSummary> {
    val query = TextNormalization.normalize(rawQuery)
    if (query.isEmpty()) throw BadRequestException("q must not be blank.")
    if (query.length > MAX_QUERY_LENGTH) throw BadRequestException("q must be at most $MAX_QUERY_LENGTH characters.")
    val geo = resolveGeo(lat, lng, radiusM)
    val cappedLimit = clampLimit(limit)
    val offset = cursorValue?.let { OffsetCursor.decode(it) } ?: 0
    val expansion = SearchSynonyms.expand(query)
    val rows =
      repository.searchText(
        query,
        expansion.cuisines,
        expansion.types,
        filters,
        geo,
        cappedLimit + 1,
        offset,
        clock.instant(),
      )
    val page = rows.take(cappedLimit)
    val next = if (rows.size > cappedLimit) OffsetCursor.encode(offset + cappedLimit) else null
    return Page(page, next)
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
  }
}
