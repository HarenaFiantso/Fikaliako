package mg.fikaliako.api.service

import mg.fikaliako.api.endpoint.rest.model.EstablishmentFilters
import mg.fikaliako.api.endpoint.rest.model.EstablishmentSummary
import mg.fikaliako.api.endpoint.rest.model.GeoPoint
import mg.fikaliako.api.model.exception.BadRequestException
import mg.fikaliako.api.repository.EstablishmentRepository
import mg.fikaliako.api.repository.GeoSearchContext
import mg.fikaliako.api.util.OffsetCursor
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SearchServiceTest {
  private val now = Instant.parse("2026-07-06T09:00:00Z")
  private val repo = Mockito.mock(EstablishmentRepository::class.java)
  private val service = SearchService(repo, Clock.fixed(now, ZoneOffset.UTC))

  private val filters = EstablishmentFilters()

  private fun summary(n: Int) =
    EstablishmentSummary(
      id = UUID.fromString("aaaaaaaa-0000-0000-0000-00000000000$n"),
      slug = "s$n",
      name = "E$n",
      type = "gargotte",
      position = GeoPoint(-18.9, 47.5),
      avgPriceAr = 3000,
      verified = false,
      status = "active",
      ratingAvg = null,
      ratingCount = 0,
    )

  @Test
  fun `rejects a blank or oversized query`() {
    assertFailsWith<BadRequestException> { service.search("   ", filters, null, null, null, null, null) }
    assertFailsWith<BadRequestException> { service.search("x".repeat(121), filters, null, null, null, null, null) }
  }

  @Test
  fun `rejects inconsistent geographic parameters`() {
    assertFailsWith<BadRequestException> { service.search("vary", filters, -18.9, null, null, null, null) }
    assertFailsWith<BadRequestException> { service.search("vary", filters, null, null, 1000.0, null, null) }
    assertFailsWith<BadRequestException> { service.search("vary", filters, -18.9, 47.5, 20_000.0, null, null) }
    assertFailsWith<BadRequestException> { service.search("vary", filters, 91.0, 47.5, null, null, null) }
  }

  @Test
  fun `normalizes the query and passes the synonym expansion to the port`() {
    Mockito
      .`when`(
        repo.searchText(
          "romazava",
          setOf("malagasy"),
          emptySet(),
          filters,
          null,
          SearchService.DEFAULT_LIMIT + 1,
          0,
          now,
        ),
      ).thenReturn(listOf(summary(1)))

    val page = service.search("  Romazava ", filters, null, null, null, null, null)
    assertEquals(1, page.items.size)
    assertNull(page.nextCursor)
  }

  @Test
  fun `pages by offset cursor`() {
    Mockito
      .`when`(
        repo.searchText("vary", setOf("malagasy"), emptySet(), filters, null, 3, 0, now),
      ).thenReturn(listOf(summary(1), summary(2), summary(3)))

    val first = service.search("vary", filters, null, null, null, 2, null)
    assertEquals(2, first.items.size)
    assertEquals(2, OffsetCursor.decode(first.nextCursor!!))

    Mockito
      .`when`(
        repo.searchText("vary", setOf("malagasy"), emptySet(), filters, null, 3, 2, now),
      ).thenReturn(listOf(summary(3)))

    val second = service.search("vary", filters, null, null, null, 2, first.nextCursor)
    assertEquals(1, second.items.size)
    assertNull(second.nextCursor)
  }

  @Test
  fun `forwards the geographic context`() {
    val geo = GeoSearchContext(-18.91, 47.52, 1000.0)
    Mockito
      .`when`(
        repo.searchText("sakafo", emptySet(), emptySet(), filters, geo, SearchService.DEFAULT_LIMIT + 1, 0, now),
      ).thenReturn(emptyList())

    val page = service.search("sakafo", filters, -18.91, 47.52, 1000.0, null, null)
    assertEquals(0, page.items.size)
  }
}
