package mg.fikaliako.api.service
import mg.fikaliako.api.endpoint.rest.model.BusinessEstablishmentUpdate
import mg.fikaliako.api.endpoint.rest.model.EstablishmentDetail
import mg.fikaliako.api.endpoint.rest.model.EstablishmentSummary
import mg.fikaliako.api.endpoint.rest.model.OpeningHoursUpdate
import mg.fikaliako.api.endpoint.rest.model.Page
import mg.fikaliako.api.model.Establishment
import mg.fikaliako.api.model.EstablishmentManagerId
import mg.fikaliako.api.model.EstablishmentStatus
import mg.fikaliako.api.model.OpeningHoursEntity
import mg.fikaliako.api.model.exception.BadRequestException
import mg.fikaliako.api.model.exception.ForbiddenException
import mg.fikaliako.api.model.exception.NotFoundException
import mg.fikaliako.api.repository.EstablishmentManagerRepository
import mg.fikaliako.api.repository.EstablishmentRatingRepository
import mg.fikaliako.api.repository.EstablishmentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime
import java.time.format.DateTimeParseException
import java.util.UUID

// The premium account's own corner (restaurateur space, brought forward from
// the book's V2 for BUSINESS roles): manage the profile of establishments a
// manager link grants. Admins pass every ownership check.
@Service
class BusinessEstablishmentService(
  private val managers: EstablishmentManagerRepository,
  private val establishments: EstablishmentRepository,
  private val ratings: EstablishmentRatingRepository,
  private val establishmentService: EstablishmentService,
) {
  @Transactional(readOnly = true)
  fun listManaged(userId: UUID): Page<EstablishmentSummary> {
    val managed = managers.findAllForUser(userId).map { requireNotNull(it.establishment) }
    val ratingsById =
      ratings
        .findAllById(managed.mapNotNull { it.id })
        .associateBy { it.establishmentId }
    // a business manages a handful of establishments: one page, no cursor
    return Page(managed.map { it.toSummary(ratingsById[it.id]) })
  }

  @Transactional
  fun updateProfile(
    userId: UUID,
    isAdmin: Boolean,
    establishmentId: UUID,
    patch: BusinessEstablishmentUpdate,
  ): EstablishmentDetail {
    val establishment = requireManaged(userId, isAdmin, establishmentId)
    patch.name?.let { establishment.name = it.trim() }
    patch.address?.let { establishment.address = it.trim() }
    patch.district?.let { establishment.district = it.trim() }
    patch.phone?.let { establishment.phone = it.trim() }
    patch.whatsapp?.let { establishment.whatsapp = it.trim() }
    patch.facebookUrl?.let { establishment.facebookUrl = it.trim() }
    patch.website?.let { establishment.website = it.trim() }
    patch.avgPriceAr?.let { establishment.avgPriceAr = it }
    patch.status?.let { establishment.status = EstablishmentStatus.valueOf(it.uppercase()) }
    patch.amenities?.let { a ->
      a.delivery?.let { establishment.delivery = it }
      a.parking?.let { establishment.parking = it }
      a.wifi?.let { establishment.wifi = it }
      a.wheelchairAccess?.let { establishment.wheelchairAccess = it }
      a.airConditioning?.let { establishment.airConditioning = it }
      a.terrace?.let { establishment.terrace = it }
      a.familyFriendly?.let { establishment.familyFriendly = it }
      a.romantic?.let { establishment.romantic = it }
      a.studentFriendly?.let { establishment.studentFriendly = it }
      a.scenicView?.let { establishment.scenicView = it }
      a.open24h?.let { establishment.open24h = it }
    }
    establishments.save(establishment)
    return establishmentService.detail(establishmentId.toString())
  }

  @Transactional
  fun replaceOpeningHours(
    userId: UUID,
    isAdmin: Boolean,
    establishmentId: UUID,
    update: OpeningHoursUpdate,
  ): EstablishmentDetail {
    val establishment = requireManaged(userId, isAdmin, establishmentId)
    val replacement =
      update.intervals.map {
        OpeningHoursEntity(
          establishment = establishment,
          dayOfWeek = it.dayOfWeek.toShort(),
          opensAt = parseTime(it.opensAt),
          closesAt = parseTime(it.closesAt),
        )
      }
    establishment.openingHours.clear()
    establishment.openingHours.addAll(replacement)
    establishments.save(establishment)
    return establishmentService.detail(establishmentId.toString())
  }

  private fun requireManaged(
    userId: UUID,
    isAdmin: Boolean,
    establishmentId: UUID,
  ): Establishment {
    val establishment =
      establishments.findById(establishmentId).orElseThrow {
        NotFoundException("Establishment '$establishmentId' not found.")
      }
    if (!isAdmin && !managers.existsById(EstablishmentManagerId(establishmentId, userId))) {
      throw ForbiddenException("Your account does not manage this establishment.")
    }
    return establishment
  }

  private fun parseTime(value: String): LocalTime =
    try {
      LocalTime.parse(value)
    } catch (e: DateTimeParseException) {
      // Bean Validation's HH:mm pattern runs first; this is the safety net
      throw BadRequestException("Invalid time '$value' (expected HH:mm).", e)
    }
}
