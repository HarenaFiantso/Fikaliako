package mg.fikaliako.api.service

import mg.fikaliako.api.endpoint.rest.model.EstablishmentSummary
import mg.fikaliako.api.endpoint.rest.model.GeoPoint
import mg.fikaliako.api.model.Cuisine
import mg.fikaliako.api.model.exception.NotFoundException
import mg.fikaliako.api.repository.CuisineRepository
import mg.fikaliako.api.repository.RankingQueryRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RankingServiceTest {
  private val now = Instant.parse("2026-07-06T09:00:00Z")
  private val rankingRepo = Mockito.mock(RankingQueryRepository::class.java)
  private val cuisineRepo = Mockito.mock(CuisineRepository::class.java)
  private val service = RankingService(rankingRepo, cuisineRepo, Clock.fixed(now, ZoneOffset.UTC))

  private val summary =
    EstablishmentSummary(
      id = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001"),
      slug = "mama-fara",
      name = "Chez Mama Fara",
      type = "gargotte",
      position = GeoPoint(-18.9, 47.5),
      avgPriceAr = 4000,
      verified = true,
      status = "active",
      ratingAvg = null,
      ratingCount = 12,
    )

  @Test
  fun `catalog lists the fixed tops plus one per cuisine`() {
    Mockito
      .`when`(cuisineRepo.findAllByOrderBySortOrder())
      .thenReturn(listOf(Cuisine("malagasy", "Malgache", "Sakafo gasy", 1)))

    val ids = service.catalog().items.map { it.id }
    assertEquals(
      listOf("top-rated", "best-gargottes", "best-value", "revelations", "cuisine-malagasy"),
      ids,
    )
  }

  @Test
  fun `best gargottes narrows by type`() {
    Mockito.`when`(cuisineRepo.findAllByOrderBySortOrder()).thenReturn(emptyList())
    Mockito
      .`when`(
        rankingRepo.topByBayesian("gargotte", null, RankingService.MIN_REVIEWS, RankingService.TOP_SIZE),
      ).thenReturn(listOf(summary))

    val page = service.top("best-gargottes")
    assertEquals("best-gargottes", page.topic.id)
    assertEquals(listOf(summary), page.items)
  }

  @Test
  fun `a cuisine top exists only for referenced cuisines`() {
    Mockito
      .`when`(cuisineRepo.findAllByOrderBySortOrder())
      .thenReturn(listOf(Cuisine("chinese", "Chinoise", "Sinoa", 5)))
    Mockito
      .`when`(
        rankingRepo.topByBayesian(null, "chinese", RankingService.MIN_REVIEWS, RankingService.TOP_SIZE),
      ).thenReturn(listOf(summary))

    assertEquals(listOf(summary), service.top("cuisine-chinese").items)
    assertFailsWith<NotFoundException> { service.top("cuisine-japanese") }
  }

  @Test
  fun `revelations look one sliding month back`() {
    Mockito.`when`(cuisineRepo.findAllByOrderBySortOrder()).thenReturn(emptyList())
    val since = now.minus(Duration.ofDays(RankingService.REVELATION_WINDOW_DAYS))
    Mockito
      .`when`(
        rankingRepo.revelations(since, RankingService.REVELATION_MIN_REVIEWS, RankingService.TOP_SIZE),
      ).thenReturn(listOf(summary))

    val page = service.top("revelations")
    assertEquals(listOf(summary), page.items)
    assertTrue(page.topic.titleFr.isNotBlank())
  }

  @Test
  fun `an unknown topic is a 404`() {
    Mockito.`when`(cuisineRepo.findAllByOrderBySortOrder()).thenReturn(emptyList())
    assertFailsWith<NotFoundException> { service.top("best-sushi") }
  }
}
