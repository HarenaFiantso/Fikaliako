package mg.fikaliako.api.service

import mg.fikaliako.api.endpoint.rest.model.Amenities
import mg.fikaliako.api.endpoint.rest.model.EstablishmentDetail
import mg.fikaliako.api.endpoint.rest.model.EstablishmentFilters
import mg.fikaliako.api.endpoint.rest.model.EstablishmentSummary
import mg.fikaliako.api.endpoint.rest.model.GeoPoint
import mg.fikaliako.api.endpoint.rest.model.OpeningInterval
import mg.fikaliako.api.endpoint.rest.model.Page
import mg.fikaliako.api.endpoint.rest.model.RatingSummary
import mg.fikaliako.api.endpoint.rest.model.ReferentialItem
import mg.fikaliako.api.model.Establishment
import mg.fikaliako.api.model.EstablishmentRating
import mg.fikaliako.api.model.exception.BadRequestException
import mg.fikaliako.api.model.exception.NotFoundException
import mg.fikaliako.api.repository.EstablishmentRatingRepository
import mg.fikaliako.api.repository.EstablishmentRepository
import mg.fikaliako.api.util.Cursor
import mg.fikaliako.api.util.OpeningHours
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class EstablishmentService(
  private val repository: EstablishmentRepository,
  private val ratingRepository: EstablishmentRatingRepository,
  private val clock: Clock,
) {
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
    val items = repository.searchNearby(lat, lng, radius, filters, cappedLimit, clock.instant())
    return Page(items)
  }

  fun list(
    filters: EstablishmentFilters,
    limit: Int?,
    cursorValue: String?,
  ): Page<EstablishmentSummary> {
    val cappedLimit = clampLimit(limit, MAX_PAGE_SIZE)
    val cursor = cursorValue?.let { Cursor.decode(it) }
    val rows = repository.searchList(filters, cappedLimit + 1, cursor, clock.instant())
    val page = rows.take(cappedLimit)
    val next =
      if (rows.size > cappedLimit) {
        page.lastOrNull()?.let { Cursor(it.createdAt, it.summary.id).encode() }
      } else {
        null
      }
    return Page(page.map { it.summary }, next)
  }

  @Transactional(readOnly = true)
  fun detail(idOrSlug: String): EstablishmentDetail {
    val establishment =
      findByIdOrSlug(idOrSlug) ?: throw NotFoundException("Establishment '$idOrSlug' not found.")
    val rating = establishment.id?.let { ratingRepository.findById(it).orElse(null) }
    return toDetail(establishment, rating)
  }

  private fun findByIdOrSlug(idOrSlug: String): Establishment? {
    val uuid =
      try {
        UUID.fromString(idOrSlug)
      } catch (_: IllegalArgumentException) {
        null
      }
    return if (uuid != null) repository.findById(uuid).orElse(null) else repository.findBySlug(idOrSlug)
  }

  private fun toDetail(
    e: Establishment,
    rating: EstablishmentRating?,
  ): EstablishmentDetail {
    val hours =
      e.openingHours.map {
        OpeningInterval(it.dayOfWeek.toInt(), TIME_FORMAT.format(it.opensAt), TIME_FORMAT.format(it.closesAt))
      }
    return EstablishmentDetail(
      id = requireNotNull(e.id),
      slug = e.slug,
      name = e.name,
      type = e.type.name.lowercase(),
      position = GeoPoint(requireNotNull(e.position).y, requireNotNull(e.position).x),
      address = e.address,
      district = e.district,
      city = e.city,
      phone = e.phone,
      whatsapp = e.whatsapp,
      facebookUrl = e.facebookUrl,
      website = e.website,
      avgPriceAr = e.avgPriceAr,
      verified = e.verified,
      status = e.status.name.lowercase(),
      openNow = OpeningHours.isOpenNow(e.open24h, hours, clock.instant()),
      amenities =
        Amenities(
          delivery = e.delivery,
          parking = e.parking,
          wifi = e.wifi,
          wheelchairAccess = e.wheelchairAccess,
          airConditioning = e.airConditioning,
          terrace = e.terrace,
          familyFriendly = e.familyFriendly,
          romantic = e.romantic,
          studentFriendly = e.studentFriendly,
          scenicView = e.scenicView,
          open24h = e.open24h,
        ),
      openingHours = hours,
      paymentMethods = e.paymentMethods.map { ReferentialItem(it.code, it.labelFr, it.labelMg) },
      cuisines = e.cuisines.map { ReferentialItem(it.code, it.labelFr, it.labelMg) },
      rating =
        rating?.let {
          RatingSummary(
            count = it.reviewCount,
            avgGlobal = it.avgGlobal,
            avgQuality = it.avgQuality,
            avgPrice = it.avgPrice,
            avgCleanliness = it.avgCleanliness,
            avgSpeed = it.avgSpeed,
            avgWelcome = it.avgWelcome,
            bayesianNote = it.bayesianNote,
          )
        } ?: RatingSummary(0, null, null, null, null, null, null, null),
      createdAt = requireNotNull(e.createdAt),
      updatedAt = requireNotNull(e.updatedAt),
    )
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

    /** Book ch. 4.2 — the distance mode goes up to 10 km. */
    const val MAX_RADIUS_M = 10_000.0
    const val DEFAULT_LIMIT = 50
    const val MAX_PAGE_SIZE = 100

    const val MAX_NEARBY_LIMIT = 200

    private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
  }
}
