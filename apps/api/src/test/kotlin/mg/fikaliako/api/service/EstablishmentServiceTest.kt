package mg.fikaliako.api.service

import mg.fikaliako.api.exception.BadRequestException
import mg.fikaliako.api.exception.NotFoundException
import mg.fikaliako.api.model.Amenities
import mg.fikaliako.api.model.EstablishmentDetail
import mg.fikaliako.api.model.EstablishmentFilters
import mg.fikaliako.api.model.EstablishmentSummary
import mg.fikaliako.api.model.GeoPoint
import mg.fikaliako.api.model.OpeningInterval
import mg.fikaliako.api.model.RatingSummary
import mg.fikaliako.api.repository.EstablishmentRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EstablishmentServiceTest {
    // Monday 2026-07-06, 12:00 in Antananarivo (== 09:00Z).
    private val now = Instant.parse("2026-07-06T09:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val repo = Mockito.mock(EstablishmentRepository::class.java)
    private val service = EstablishmentService(repo, clock)

    private val filters = EstablishmentFilters()

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
        // Default radius 1000 m, limit clamped from 500 to the 200 clustering cap.
        Mockito.`when`(repo.findNearby(-18.9, 47.5, 1000.0, filters, 200, now)).thenReturn(expected)
        val page = service.nearby(-18.9, 47.5, null, filters, 500)
        assertEquals(expected, page.items)
        assertNull(page.nextCursor)
    }

    @Test
    fun `list emits a next cursor only when a further page exists`() {
        val rowA = EstablishmentRepository.ListRow(summary("aaaaaaaa-0000-0000-0000-000000000001"), now)
        val rowB = EstablishmentRepository.ListRow(summary("aaaaaaaa-0000-0000-0000-000000000002"), now.minusSeconds(1))
        val rowC = EstablishmentRepository.ListRow(summary("aaaaaaaa-0000-0000-0000-000000000003"), now.minusSeconds(2))

        // Over-fetch of 3 for a page size of 2 → there is a next page.
        Mockito.`when`(repo.findList(filters, 3, null, now)).thenReturn(listOf(rowA, rowB, rowC))
        val page = service.list(filters, 2, null)
        assertEquals(2, page.items.size)
        assertTrue(page.nextCursor != null)

        // Exactly the page size returned → no next page.
        Mockito.`when`(repo.findList(filters, 3, null, now)).thenReturn(listOf(rowA, rowB))
        val last = service.list(filters, 2, null)
        assertEquals(2, last.items.size)
        assertNull(last.nextCursor)
    }

    @Test
    fun `detail computes open_now from opening hours`() {
        Mockito.`when`(repo.findDetail("chez")).thenReturn(detailOpenMondayMidday())
        assertTrue(service.detail("chez").openNow)
    }

    @Test
    fun `detail throws when the establishment is missing`() {
        Mockito.`when`(repo.findDetail("missing")).thenReturn(null)
        assertFailsWith<NotFoundException> { service.detail("missing") }
    }

    private fun detailOpenMondayMidday(): EstablishmentDetail =
        EstablishmentDetail(
            id = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001"),
            slug = "chez",
            name = "Chez Bao",
            type = "gargotte",
            position = GeoPoint(-18.9, 47.5),
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
            openNow = false,
            amenities =
                Amenities(
                    delivery = false,
                    parking = false,
                    wifi = false,
                    wheelchairAccess = false,
                    airConditioning = false,
                    terrace = false,
                    familyFriendly = false,
                    romantic = false,
                    studentFriendly = false,
                    scenicView = false,
                    open24h = false,
                ),
            openingHours = listOf(OpeningInterval(0, "07:00", "21:00")),
            paymentMethods = emptyList(),
            cuisines = emptyList(),
            rating = RatingSummary(0, null, null, null, null, null, null, null),
            createdAt = now,
            updatedAt = now,
        )
}
