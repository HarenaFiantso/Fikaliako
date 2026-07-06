package mg.fikaliako.api.service

import mg.fikaliako.api.exception.BadRequestException
import mg.fikaliako.api.exception.NotFoundException
import mg.fikaliako.api.model.EstablishmentDetail
import mg.fikaliako.api.model.EstablishmentFilters
import mg.fikaliako.api.model.EstablishmentSummary
import mg.fikaliako.api.model.Page
import mg.fikaliako.api.repository.EstablishmentRepository
import mg.fikaliako.api.util.Cursor
import mg.fikaliako.api.util.OpeningHours
import org.springframework.stereotype.Service
import java.time.Clock

@Service
class EstablishmentService(
    private val repository: EstablishmentRepository,
    private val clock: Clock,
) {
    /** Core geo query: open-if-asked establishments near a point, cheapest first (book ch. 6.2). */
    fun nearby(
        lat: Double,
        lng: Double,
        radiusM: Double?,
        filters: EstablishmentFilters,
        limit: Int?,
    ): Page<EstablishmentSummary> {
        validateCoordinates(lat, lng)
        val radius =
            (radiusM ?: DEFAULT_RADIUS_M).also {
                if (it <= 0 || it > MAX_RADIUS_M) throw BadRequestException("radius must be in (0, $MAX_RADIUS_M].")
            }
        val cappedLimit = clampLimit(limit, MAX_NEARBY_LIMIT)
        val items = repository.findNearby(lat, lng, radius, filters, cappedLimit, clock.instant())
        // Geo result sets are bounded by radius + limit; no cursor paging here.
        return Page(items)
    }

    fun list(
        filters: EstablishmentFilters,
        limit: Int?,
        cursorValue: String?,
    ): Page<EstablishmentSummary> {
        val cappedLimit = clampLimit(limit, MAX_PAGE_SIZE)
        val cursor = cursorValue?.let { Cursor.decode(it) }
        // Over-fetch by one to detect whether a further page exists.
        val rows = repository.findList(filters, cappedLimit + 1, cursor, clock.instant())
        val page = rows.take(cappedLimit)
        val next =
            if (rows.size > cappedLimit) {
                page.lastOrNull()?.let { Cursor(it.createdAt, it.summary.id).encode() }
            } else {
                null
            }
        return Page(page.map { it.summary }, next)
    }

    fun detail(idOrSlug: String): EstablishmentDetail {
        val detail = repository.findDetail(idOrSlug) ?: throw NotFoundException("Establishment '$idOrSlug' not found.")
        val openNow = OpeningHours.isOpenNow(detail.amenities.open24h, detail.openingHours, clock.instant())
        return detail.copy(openNow = openNow)
    }

    private fun validateCoordinates(
        lat: Double,
        lng: Double,
    ) {
        if (lat < -90 || lat > 90) throw BadRequestException("lat must be in [-90, 90].")
        if (lng < -180 || lng > 180) throw BadRequestException("lng must be in [-180, 180].")
    }

    private fun clampLimit(
        limit: Int?,
        max: Int,
    ): Int {
        val value = limit ?: DEFAULT_LIMIT
        if (value < 1) throw BadRequestException("limit must be at least 1.")
        return value.coerceAtMost(max)
    }

    companion object {
        const val DEFAULT_RADIUS_M = 1000.0
        const val MAX_RADIUS_M = 5000.0
        const val DEFAULT_LIMIT = 50
        const val MAX_PAGE_SIZE = 100

        // Client-side clustering kicks in beyond 200 points (book ch. 6.2 / 7.4).
        const val MAX_NEARBY_LIMIT = 200
    }
}
