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
    assertNull(page.interpretation)
  }

  @Test
  fun `j'ai faim near a position becomes a discovery-ranked open-now query within 1 km`() {
    val geo = GeoSearchContext(-18.91, 47.52, 1000.0)
    val smartFilters = filters.copy(openNow = true)
    Mockito
      .`when`(
        repo.searchDiscovery(geo, smartFilters, SearchService.DEFAULT_LIMIT + 1, 0, now),
      ).thenReturn(listOf(summary(1)))

    val page = service.search("j'ai faim", filters, -18.91, 47.52, null, null, null)

    assertEquals(1, page.items.size)
    val interpretation = page.interpretation!!
    assertEquals(listOf("hungry"), interpretation.intents)
    assertEquals(true, interpretation.openNow)
    assertEquals(1000.0, interpretation.radiusM)
    assertEquals(SearchService.ORDERING_DISCOVERY, interpretation.ordering)
  }

  @Test
  fun `a dish with a smart phrase keeps text-searching the dish under the interpreted budget`() {
    val smartFilters = filters.copy(maxPrice = 5000)
    Mockito
      .`when`(
        repo.searchText(
          "pizza",
          setOf("fast_food"),
          emptySet(),
          smartFilters,
          null,
          SearchService.DEFAULT_LIMIT + 1,
          0,
          now,
        ),
      ).thenReturn(listOf(summary(1)))

    val page = service.search("pizza pas cher", filters, null, null, null, null, null)

    assertEquals(1, page.items.size)
    val interpretation = page.interpretation!!
    assertEquals(5000, interpretation.maxPriceAr)
    assertEquals(SearchService.ORDERING_RELEVANCE, interpretation.ordering)
  }

  @Test
  fun `explicit parameters win over interpreted ones`() {
    val explicit = EstablishmentFilters(maxPrice = 3000)
    Mockito
      .`when`(
        repo.searchDiscovery(null, explicit, SearchService.DEFAULT_LIMIT + 1, 0, now),
      ).thenReturn(emptyList())

    val page = service.search("pas cher", explicit, null, null, null, null, null)
    assertEquals(3000, page.interpretation!!.maxPriceAr)
  }
}
