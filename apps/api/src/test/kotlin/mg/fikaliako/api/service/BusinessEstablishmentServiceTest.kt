package mg.fikaliako.api.service
import mg.fikaliako.api.endpoint.rest.model.Amenities
import mg.fikaliako.api.endpoint.rest.model.AmenitiesUpdate
import mg.fikaliako.api.endpoint.rest.model.BusinessEstablishmentUpdate
import mg.fikaliako.api.endpoint.rest.model.EstablishmentDetail
import mg.fikaliako.api.endpoint.rest.model.GeoPoint
import mg.fikaliako.api.endpoint.rest.model.OpeningHoursUpdate
import mg.fikaliako.api.endpoint.rest.model.OpeningIntervalInput
import mg.fikaliako.api.endpoint.rest.model.RatingSummary
import mg.fikaliako.api.model.Establishment
import mg.fikaliako.api.model.EstablishmentManager
import mg.fikaliako.api.model.EstablishmentManagerId
import mg.fikaliako.api.model.EstablishmentStatus
import mg.fikaliako.api.model.exception.ForbiddenException
import mg.fikaliako.api.model.exception.NotFoundException
import mg.fikaliako.api.repository.EstablishmentManagerRepository
import mg.fikaliako.api.repository.EstablishmentRatingRepository
import mg.fikaliako.api.repository.EstablishmentRepository
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.mockito.Mockito
import java.time.Instant
import java.time.LocalTime
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BusinessEstablishmentServiceTest {
  private val managers = Mockito.mock(EstablishmentManagerRepository::class.java)
  private val establishments = Mockito.mock(EstablishmentRepository::class.java)
  private val ratings = Mockito.mock(EstablishmentRatingRepository::class.java)
  private val establishmentService = Mockito.mock(EstablishmentService::class.java)
  private val service = BusinessEstablishmentService(managers, establishments, ratings, establishmentService)

  private val userId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001")
  private val estId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001")
  private val geometryFactory = GeometryFactory()

  private fun establishment() =
    Establishment(
      id = estId,
      name = "Gargotte Chez Bao",
      slug = "gargotte-chez-bao",
      position = geometryFactory.createPoint(Coordinate(47.5210, -18.9092)),
      status = EstablishmentStatus.ACTIVE,
    )

  private fun detailStub() =
    EstablishmentDetail(
      id = estId,
      slug = "gargotte-chez-bao",
      name = "Gargotte Chez Bao",
      type = "gargotte",
      position = GeoPoint(-18.9092, 47.5210),
      address = null,
      district = null,
      city = "Antananarivo",
      phone = null,
      whatsapp = null,
      facebookUrl = null,
      website = null,
      avgPriceAr = 2500,
      verified = true,
      status = "active",
      openNow = true,
      amenities = Amenities(false, false, false, false, false, false, false, false, false, false, false),
      openingHours = emptyList(),
      paymentMethods = emptyList(),
      cuisines = emptyList(),
      rating = RatingSummary(0, null, null, null, null, null, null, null),
      createdAt = Instant.parse("2026-07-01T00:00:00Z"),
      updatedAt = Instant.parse("2026-07-01T00:00:00Z"),
    )

  private fun grantManagement() {
    Mockito
      .`when`(managers.existsById(EstablishmentManagerId(estId, userId)))
      .thenReturn(true)
  }

  @Test
  fun `lists the establishments the account manages`() {
    val link = EstablishmentManager(id = EstablishmentManagerId(estId, userId), establishment = establishment())
    Mockito.`when`(managers.findAllForUser(userId)).thenReturn(listOf(link))
    Mockito.`when`(ratings.findAllById(listOf(estId))).thenReturn(emptyList())

    val page = service.listManaged(userId)

    assertEquals(1, page.items.size)
    assertEquals("gargotte-chez-bao", page.items[0].slug)
    assertEquals(null, page.nextCursor)
  }

  @Test
  fun `a business account cannot touch an establishment it does not manage`() {
    Mockito.`when`(establishments.findById(estId)).thenReturn(Optional.of(establishment()))
    Mockito.`when`(managers.existsById(EstablishmentManagerId(estId, userId))).thenReturn(false)

    assertFailsWith<ForbiddenException> {
      service.updateProfile(userId, false, estId, BusinessEstablishmentUpdate(name = "Nope"))
    }
  }

  @Test
  fun `an admin bypasses the manager link`() {
    val stored = establishment()
    Mockito.`when`(establishments.findById(estId)).thenReturn(Optional.of(stored))
    Mockito.`when`(establishmentService.detail(estId.toString())).thenReturn(detailStub())

    service.updateProfile(userId, true, estId, BusinessEstablishmentUpdate(name = "Renamed"))

    assertEquals("Renamed", stored.name)
  }

  @Test
  fun `updating a missing establishment is a 404`() {
    Mockito.`when`(establishments.findById(estId)).thenReturn(Optional.empty())
    assertFailsWith<NotFoundException> {
      service.updateProfile(userId, false, estId, BusinessEstablishmentUpdate())
    }
  }

  @Test
  fun `patch only touches provided fields and maps amenities and status`() {
    val stored = establishment()
    Mockito.`when`(establishments.findById(estId)).thenReturn(Optional.of(stored))
    grantManagement()
    Mockito.`when`(establishmentService.detail(estId.toString())).thenReturn(detailStub())

    service.updateProfile(
      userId,
      false,
      estId,
      BusinessEstablishmentUpdate(
        phone = " +261 34 00 000 01 ",
        avgPriceAr = 4000,
        status = "closed",
        amenities = AmenitiesUpdate(wifi = true, terrace = true),
      ),
    )

    assertEquals("Gargotte Chez Bao", stored.name)
    assertEquals("+261 34 00 000 01", stored.phone)
    assertEquals(4000, stored.avgPriceAr)
    assertEquals(EstablishmentStatus.CLOSED, stored.status)
    assertTrue(stored.wifi)
    assertTrue(stored.terrace)
    assertEquals(false, stored.delivery)
    Mockito.verify(establishments).save(stored)
  }

  @Test
  fun `opening hours are fully replaced`() {
    val stored = establishment()
    stored.openingHours.add(
      mg.fikaliako.api.model
        .OpeningHoursEntity(id = 1, establishment = stored, dayOfWeek = 0),
    )
    Mockito.`when`(establishments.findById(estId)).thenReturn(Optional.of(stored))
    grantManagement()
    Mockito.`when`(establishmentService.detail(estId.toString())).thenReturn(detailStub())

    service.replaceOpeningHours(
      userId,
      false,
      estId,
      OpeningHoursUpdate(
        intervals =
          listOf(
            OpeningIntervalInput(1, "07:00", "14:00"),
            OpeningIntervalInput(1, "18:00", "21:30"),
          ),
      ),
    )

    assertEquals(2, stored.openingHours.size)
    assertEquals(LocalTime.of(18, 0), stored.openingHours[1].opensAt)
    assertEquals(1, stored.openingHours[1].dayOfWeek.toInt())
  }
}
