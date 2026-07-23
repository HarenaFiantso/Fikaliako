package mg.fikaliako.api.service

import mg.fikaliako.api.model.EstablishmentRating
import mg.fikaliako.api.model.Review
import mg.fikaliako.api.model.ReviewStatus
import mg.fikaliako.api.repository.EstablishmentRatingRepository
import mg.fikaliako.api.repository.ReviewRepository
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RatingAggregationServiceTest {
  private val now = Instant.parse("2026-07-06T09:00:00Z")
  private val reviewRepo = Mockito.mock(ReviewRepository::class.java)
  private val ratingRepo = Mockito.mock(EstablishmentRatingRepository::class.java)
  private val service = RatingAggregationService(reviewRepo, ratingRepo, Clock.fixed(now, ZoneOffset.UTC))

  private val estId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001")

  private fun review(
    quality: Int,
    global: String,
    ageDays: Long,
  ): Review =
    Review(
      id = UUID.randomUUID(),
      ratingQuality = quality.toShort(),
      ratingPrice = 4,
      ratingCleanliness = 4,
      ratingSpeed = 3,
      ratingWelcome = 5,
      globalNote = BigDecimal(global),
      createdAt = now.minus(ageDays, ChronoUnit.DAYS),
    )

  @Test
  fun `recompute aggregates averages, recent count and bayesian note`() {
    Mockito.`when`(reviewRepo.averageGlobalNote(ReviewStatus.published)).thenReturn(3.5)
    Mockito
      .`when`(reviewRepo.findByEstablishmentIdAndStatus(estId, ReviewStatus.published))
      .thenReturn(listOf(review(5, "4.50", 10), review(4, "4.00", 120)))
    Mockito.`when`(ratingRepo.findById(estId)).thenReturn(Optional.empty())

    service.recompute(estId)

    val captor = ArgumentCaptor.forClass(EstablishmentRating::class.java)
    Mockito.verify(ratingRepo).save(captor.capture())
    val saved = captor.value
    assertEquals(estId, saved.establishmentId)
    assertEquals(2, saved.reviewCount)
    assertEquals(1, saved.recentReviewCount)
    assertEquals(BigDecimal("4.50"), saved.avgQuality)
    assertEquals(BigDecimal("4.00"), saved.avgPrice)
    assertEquals(BigDecimal("4.25"), saved.avgGlobal)
    // (10*3.50 + 1.00*4.50 + 0.70*4.00) / (10 + 1.70) = 42.30 / 11.70
    assertEquals(BigDecimal("3.62"), saved.bayesianNote)
    assertEquals(now, saved.computedAt)
  }

  @Test
  fun `an establishment whose reviews all disappeared is zeroed, not deleted`() {
    Mockito.`when`(reviewRepo.averageGlobalNote(ReviewStatus.published)).thenReturn(4.0)
    Mockito
      .`when`(reviewRepo.findByEstablishmentIdAndStatus(estId, ReviewStatus.published))
      .thenReturn(emptyList())
    val existing =
      EstablishmentRating(
        establishmentId = estId,
        reviewCount = 3,
        avgGlobal = BigDecimal("4.20"),
        bayesianNote = BigDecimal("4.05"),
      )
    Mockito.`when`(ratingRepo.findById(estId)).thenReturn(Optional.of(existing))

    service.recompute(estId)

    val captor = ArgumentCaptor.forClass(EstablishmentRating::class.java)
    Mockito.verify(ratingRepo).save(captor.capture())
    val saved = captor.value
    assertEquals(0, saved.reviewCount)
    assertEquals(0, saved.recentReviewCount)
    assertNull(saved.avgGlobal)
    assertNull(saved.bayesianNote)
  }

  @Test
  fun `recomputeAll visits establishments with reviews and stale snapshots alike`() {
    val staleId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002")
    Mockito.`when`(reviewRepo.averageGlobalNote(ReviewStatus.published)).thenReturn(4.0)
    Mockito.`when`(reviewRepo.establishmentIdsWithReviews(ReviewStatus.published)).thenReturn(listOf(estId))
    Mockito.`when`(ratingRepo.establishmentIds()).thenReturn(listOf(estId, staleId))
    Mockito.`when`(reviewRepo.findByEstablishmentIdAndStatus(estId, ReviewStatus.published)).thenReturn(emptyList())
    Mockito.`when`(reviewRepo.findByEstablishmentIdAndStatus(staleId, ReviewStatus.published)).thenReturn(emptyList())
    Mockito.`when`(ratingRepo.findById(estId)).thenReturn(Optional.empty())
    Mockito.`when`(ratingRepo.findById(staleId)).thenReturn(Optional.empty())

    service.recomputeAll()

    val captor = ArgumentCaptor.forClass(EstablishmentRating::class.java)
    Mockito.verify(ratingRepo, Mockito.times(2)).save(captor.capture())
    assertEquals(setOf(estId, staleId), captor.allValues.map { it.establishmentId }.toSet())
  }
}
