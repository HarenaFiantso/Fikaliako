package mg.fikaliako.api.service
import mg.fikaliako.api.model.Review
import mg.fikaliako.api.model.ReviewStatus
import mg.fikaliako.api.model.UserAccount
import mg.fikaliako.api.model.exception.BadRequestException
import mg.fikaliako.api.model.exception.NotFoundException
import mg.fikaliako.api.repository.EstablishmentRepository
import mg.fikaliako.api.repository.ReviewRepository
import mg.fikaliako.api.util.Cursor
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.data.domain.Limit
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ReviewServiceTest {
  private val now = Instant.parse("2026-07-06T09:00:00Z")
  private val reviewRepo = Mockito.mock(ReviewRepository::class.java)
  private val establishmentRepo = Mockito.mock(EstablishmentRepository::class.java)
  private val service = ReviewService(reviewRepo, establishmentRepo)

  private val estId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001")

  private fun review(
    id: String,
    createdAt: Instant,
  ): Review =
    Review(
      id = UUID.fromString(id),
      author = UserAccount(UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001"), "Naina"),
      ratingQuality = 5,
      ratingPrice = 4,
      ratingCleanliness = 4,
      ratingSpeed = 3,
      ratingWelcome = 5,
      globalNote = BigDecimal("4.4"),
      comment = "Tsara be",
      createdAt = createdAt,
    )

  @Test
  fun `throws when the establishment is missing`() {
    Mockito.`when`(establishmentRepo.existsById(estId)).thenReturn(false)
    assertFailsWith<NotFoundException> { service.listForEstablishment(estId, null, null) }
  }

  @Test
  fun `rejects a non-positive limit`() {
    Mockito.`when`(establishmentRepo.existsById(estId)).thenReturn(true)
    assertFailsWith<BadRequestException> { service.listForEstablishment(estId, 0, null) }
  }

  @Test
  fun `maps reviews and emits a next cursor only when a further page exists`() {
    Mockito.`when`(establishmentRepo.existsById(estId)).thenReturn(true)
    val rows =
      listOf(
        review("cccccccc-0000-0000-0000-000000000001", now),
        review("cccccccc-0000-0000-0000-000000000002", now.minusSeconds(1)),
        review("cccccccc-0000-0000-0000-000000000003", now.minusSeconds(2)),
      )
    Mockito
      .`when`(reviewRepo.findPublished(estId, ReviewStatus.PUBLISHED, Limit.of(3)))
      .thenReturn(rows)

    val page = service.listForEstablishment(estId, 2, null)
    assertEquals(2, page.items.size)
    assertNotNull(page.nextCursor)
    val first = page.items.first()
    assertEquals("Naina", first.authorName)
    assertEquals(5, first.ratingQuality)
    assertEquals(BigDecimal("4.4"), first.globalNote)
    assertEquals("Tsara be", first.comment)

    Mockito
      .`when`(reviewRepo.findPublished(estId, ReviewStatus.PUBLISHED, Limit.of(3)))
      .thenReturn(rows.take(2))
    assertNull(service.listForEstablishment(estId, 2, null).nextCursor)
  }

  @Test
  fun `resumes after the cursor`() {
    Mockito.`when`(establishmentRepo.existsById(estId)).thenReturn(true)
    val cursorId = UUID.fromString("cccccccc-0000-0000-0000-000000000002")
    val cursor = Cursor(now, cursorId).encode()
    Mockito
      .`when`(
        reviewRepo.findPublishedAfter(
          estId,
          ReviewStatus.PUBLISHED,
          now,
          cursorId,
          Limit.of(ReviewService.DEFAULT_LIMIT + 1),
        ),
      ).thenReturn(listOf(review("cccccccc-0000-0000-0000-000000000003", now.minusSeconds(2))))

    val page = service.listForEstablishment(estId, null, cursor)
    assertEquals(1, page.items.size)
    assertNull(page.nextCursor)
  }
}
