package mg.fikaliako.api.service

import mg.fikaliako.api.endpoint.rest.model.EstablishmentFilters
import mg.fikaliako.api.endpoint.rest.model.EstablishmentSummary
import mg.fikaliako.api.endpoint.rest.model.GeoPoint
import mg.fikaliako.api.model.Establishment
import mg.fikaliako.api.model.EstablishmentStatus
import mg.fikaliako.api.model.EstablishmentType
import mg.fikaliako.api.model.OpeningHoursEntity
import mg.fikaliako.api.model.exception.BadRequestException
import mg.fikaliako.api.model.exception.NotFoundException
import mg.fikaliako.api.repository.EstablishmentListRow
import mg.fikaliako.api.repository.EstablishmentRatingRepository
import mg.fikaliako.api.repository.EstablishmentRepository
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EstablishmentServiceTest {
  private val now = Instant.parse("2026-07-06T09:00:00Z")
  private val clock = Clock.fixed(now, ZoneOffset.UTC)
  private val repo = Mockito.mock(EstablishmentRepository::class.java)
  private val ratingRepo = Mockito.mock(EstablishmentRatingRepository::class.java)
  private val service = EstablishmentService(repo, ratingRepo, clock)

  private val filters = EstablishmentFilters()
  private val estId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001")

  private fun summary(id: String) =
    EstablishmentSummary(
      id = UUID.fromString(id),
      slug = "s-$id",
      name = "E",
      type = "gargotte",
      position = GeoPoint(-18.9, 47.5),
      avgPriceAr = 2500,
      verified = true,
      status = "active",
      ratingAvg = null,
      ratingCount = 0,
    )

  @Test
  fun `nearby rejects out-of-range coordinates`() {
    assertFailsWith<BadRequestException> { service.nearby(91.0, 47.5, null, filters, null) }
    assertFailsWith<BadRequestException> { service.nearby(-18.9, 200.0, null, filters, null) }
  }

  @Test
  fun `nearby rejects a radius beyond the cap`() {
    assertFailsWith<BadRequestException> { service.nearby(-18.9, 47.5, 9999.0, filters, null) }
  }

  @Test
  fun `nearby applies the default radius and clamps the limit`() {
    val expected = listOf(summary("aaaaaaaa-0000-0000-0000-000000000001"))
    Mockito.`when`(repo.searchNearby(-18.9, 47.5, 1000.0, filters, 200, now)).thenReturn(expected)
    val page = service.nearby(-18.9, 47.5, null, filters, 500)
    assertEquals(expected, page.items)
    assertNull(page.nextCursor)
  }

  @Test
  fun `list emits a next cursor only when a further page exists`() {
    val rowA = EstablishmentListRow(summary("aaaaaaaa-0000-0000-0000-000000000001"), now)
    val rowB = EstablishmentListRow(summary("aaaaaaaa-0000-0000-0000-000000000002"), now.minusSeconds(1))
    val rowC = EstablishmentListRow(summary("aaaaaaaa-0000-0000-0000-000000000003"), now.minusSeconds(2))

    Mockito.`when`(repo.searchList(filters, 3, null, now)).thenReturn(listOf(rowA, rowB, rowC))
    val page = service.list(filters, 2, null)
    assertEquals(2, page.items.size)
    assertTrue(page.nextCursor != null)

    Mockito.`when`(repo.searchList(filters, 3, null, now)).thenReturn(listOf(rowA, rowB))
    val last = service.list(filters, 2, null)
    assertEquals(2, last.items.size)
    assertNull(last.nextCursor)
  }

  @Test
  fun `detail maps the entity and computes open_now from opening hours`() {
    Mockito.`when`(repo.findBySlug("chez")).thenReturn(openMondayMiddayEntity())
    Mockito.`when`(ratingRepo.findById(estId)).thenReturn(Optional.empty())
    val detail = service.detail("chez")
    assertEquals("chez", detail.slug)
    assertEquals(GeoPoint(-18.9, 47.5), detail.position)
    assertTrue(detail.openNow)
  }

  @Test
  fun `detail throws when the establishment is missing`() {
    Mockito.`when`(repo.findBySlug("missing")).thenReturn(null)
    assertFailsWith<NotFoundException> { service.detail("missing") }
  }

  private fun openMondayMiddayEntity(): Establishment =
    Establishment(
      id = estId,
      name = "Chez Bao",
      slug = "chez",
      type = EstablishmentType.gargotte,
      position = GeometryFactory().createPoint(Coordinate(47.5, -18.9)),
      city = "Antananarivo",
      status = EstablishmentStatus.active,
      open24h = false,
      createdAt = now,
      updatedAt = now,
      openingHours =
        mutableListOf(
          OpeningHoursEntity(
            id = 1,
            dayOfWeek = 0,
            opensAt = LocalTime.parse("07:00"),
            closesAt = LocalTime.parse("21:00"),
          ),
        ),
    )
}
