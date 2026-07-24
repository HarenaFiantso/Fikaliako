package mg.fikaliako.api.service

import mg.fikaliako.api.model.AuditLogEntry
import mg.fikaliako.api.model.Contribution
import mg.fikaliako.api.model.ContributionStatus
import mg.fikaliako.api.model.ContributionType
import mg.fikaliako.api.model.Establishment
import mg.fikaliako.api.model.EstablishmentStatus
import mg.fikaliako.api.model.EstablishmentType
import mg.fikaliako.api.model.UserAccount
import mg.fikaliako.api.model.exception.BadRequestException
import mg.fikaliako.api.model.exception.ConflictException
import mg.fikaliako.api.model.exception.NotFoundException
import mg.fikaliako.api.repository.AuditLogRepository
import mg.fikaliako.api.repository.ContributionRepository
import mg.fikaliako.api.repository.EstablishmentRepository
import mg.fikaliako.api.repository.UserAccountRepository
import mg.fikaliako.api.util.Cursor
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.springframework.data.domain.PageRequest
import tools.jackson.databind.json.JsonMapper
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ModerationServiceTest {
  private val now = Instant.parse("2026-07-20T10:00:00Z")
  private val contributionRepo = Mockito.mock(ContributionRepository::class.java)
  private val establishmentRepo = Mockito.mock(EstablishmentRepository::class.java)
  private val userRepo = Mockito.mock(UserAccountRepository::class.java)
  private val auditRepo = Mockito.mock(AuditLogRepository::class.java)
  private val service =
    ModerationService(
      contributionRepo,
      establishmentRepo,
      userRepo,
      auditRepo,
      JsonMapper.builder().build(),
      Clock.fixed(now, ZoneOffset.UTC),
    )

  private val moderatorId = UUID.fromString("cccccccc-0000-0000-0000-000000000001")
  private val authorId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001")
  private val contributionId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001")

  private fun contribution(
    status: ContributionStatus = ContributionStatus.pending,
    type: ContributionType = ContributionType.create,
    payload: String =
      """{"name":"Chez Bao","type":"gargotte","lat":-18.91,"lng":47.52,"city":"Antananarivo","avg_price_ar":3000}""",
  ) = Contribution(
    id = contributionId,
    authorId = authorId,
    type = type,
    payload = payload,
    status = status,
    createdAt = now.minusSeconds(3_600),
  )

  private fun author() = UserAccount(id = authorId, displayName = "Naina")

  @Test
  fun `the queue lists pending contributions oldest first with author names`() {
    Mockito
      .`when`(contributionRepo.findByStatusOrderByCreatedAtAscIdAsc(ContributionStatus.pending, PageRequest.of(0, 21)))
      .thenReturn(listOf(contribution()))
    Mockito.`when`(userRepo.findAllById(listOf(authorId))).thenReturn(listOf(author()))

    val page = service.queue(statusValue = null, limit = 20, cursorValue = null)

    assertEquals(1, page.items.size)
    val item = page.items.first()
    assertEquals(contributionId, item.id)
    assertEquals("create", item.type)
    assertEquals("pending", item.status)
    assertEquals("Naina", item.authorName)
    assertEquals("Chez Bao", item.payload["name"])
    assertEquals(3000, (item.payload["avg_price_ar"] as Number).toInt())
    assertNull(page.nextCursor)
  }

  @Test
  fun `a full page yields a cursor resuming after the last row`() {
    val first = contribution()
    val second =
      Contribution(
        id = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002"),
        authorId = authorId,
        type = ContributionType.create,
        payload = """{"name":"Mofo gasy","type":"street_vendor","lat":-18.9,"lng":47.5}""",
        status = ContributionStatus.pending,
        createdAt = now.minusSeconds(1_800),
      )
    Mockito
      .`when`(contributionRepo.findByStatusOrderByCreatedAtAscIdAsc(ContributionStatus.pending, PageRequest.of(0, 2)))
      .thenReturn(listOf(first, second))
    Mockito.`when`(userRepo.findAllById(listOf(authorId))).thenReturn(listOf(author()))

    val page = service.queue(statusValue = null, limit = 1, cursorValue = null)

    assertEquals(1, page.items.size)
    val cursor = Cursor.decode(requireNotNull(page.nextCursor))
    assertEquals(requireNotNull(first.createdAt), cursor.createdAt)
    assertEquals(contributionId, cursor.id)

    Mockito
      .`when`(
        contributionRepo.findByStatusAfter(
          ContributionStatus.pending,
          cursor.createdAt,
          cursor.id,
          PageRequest.of(0, 2),
        ),
      ).thenReturn(listOf(second))
    val nextPage = service.queue(statusValue = null, limit = 1, cursorValue = page.nextCursor)
    assertEquals(second.id, nextPage.items.first().id)
    assertNull(nextPage.nextCursor)
  }

  @Test
  fun `an unknown status is rejected`() {
    val error = assertThrows<BadRequestException> { service.queue("weird", null, null) }
    assertEquals(true, error.message!!.contains("weird"))
  }

  @Test
  fun `approving a create proposal materialises an active establishment`() {
    Mockito.`when`(contributionRepo.findById(contributionId)).thenReturn(Optional.of(contribution()))
    Mockito.`when`(userRepo.findAllById(listOf(authorId))).thenReturn(listOf(author()))

    val item = service.approve(moderatorId, contributionId)

    val captor = ArgumentCaptor.forClass(Establishment::class.java)
    Mockito.verify(establishmentRepo).save(captor.capture())
    val saved = captor.value
    assertEquals("Chez Bao", saved.name)
    assertEquals("chez-bao", saved.slug)
    assertEquals(EstablishmentType.gargotte, saved.type)
    assertEquals(EstablishmentStatus.active, saved.status)
    assertEquals(47.52, requireNotNull(saved.position).x)
    assertEquals(-18.91, requireNotNull(saved.position).y)
    assertEquals("Antananarivo", saved.city)
    assertEquals(3000, saved.avgPriceAr)

    assertEquals("applied", item.status)
    assertEquals(saved.id, item.establishmentId)
    assertEquals("Naina", item.authorName)

    val auditCaptor = ArgumentCaptor.forClass(AuditLogEntry::class.java)
    Mockito.verify(auditRepo).save(auditCaptor.capture())
    assertEquals("contribution.approved", auditCaptor.value.action)
    assertEquals(contributionId, auditCaptor.value.entityId)
    assertEquals(moderatorId, auditCaptor.value.actorId)
  }

  @Test
  fun `a taken slug gets a numeric suffix`() {
    Mockito.`when`(contributionRepo.findById(contributionId)).thenReturn(Optional.of(contribution()))
    Mockito.`when`(userRepo.findAllById(listOf(authorId))).thenReturn(listOf(author()))
    Mockito
      .`when`(establishmentRepo.findBySlug("chez-bao"))
      .thenReturn(Establishment(id = UUID.randomUUID(), slug = "chez-bao"))

    service.approve(moderatorId, contributionId)

    val captor = ArgumentCaptor.forClass(Establishment::class.java)
    Mockito.verify(establishmentRepo).save(captor.capture())
    assertEquals("chez-bao-2", captor.value.slug)
  }

  @Test
  fun `approving a resolved contribution is a conflict`() {
    Mockito
      .`when`(contributionRepo.findById(contributionId))
      .thenReturn(Optional.of(contribution(status = ContributionStatus.rejected)))

    assertThrows<ConflictException> { service.approve(moderatorId, contributionId) }
    Mockito.verifyNoInteractions(establishmentRepo)
  }

  @Test
  fun `only create contributions can be approved`() {
    Mockito
      .`when`(contributionRepo.findById(contributionId))
      .thenReturn(Optional.of(contribution(type = ContributionType.correction)))

    assertThrows<BadRequestException> { service.approve(moderatorId, contributionId) }
    Mockito.verifyNoInteractions(establishmentRepo)
  }

  @Test
  fun `rejecting records the trimmed reason and audits it`() {
    val contribution = contribution()
    Mockito.`when`(contributionRepo.findById(contributionId)).thenReturn(Optional.of(contribution))
    Mockito.`when`(userRepo.findAllById(listOf(authorId))).thenReturn(listOf(author()))

    val item = service.reject(moderatorId, contributionId, "  Duplicate of Chez Bao Analakely  ")

    assertEquals("rejected", item.status)
    assertEquals("Duplicate of Chez Bao Analakely", item.resolutionNote)
    assertEquals(ContributionStatus.rejected, contribution.status)
    assertEquals(moderatorId, contribution.resolvedBy)
    assertEquals(now, contribution.resolvedAt)

    val auditCaptor = ArgumentCaptor.forClass(AuditLogEntry::class.java)
    Mockito.verify(auditRepo).save(auditCaptor.capture())
    assertEquals("contribution.rejected", auditCaptor.value.action)
    assertEquals(true, auditCaptor.value.details!!.contains("Duplicate of Chez Bao Analakely"))
  }

  @Test
  fun `a blank reason is stored as no note`() {
    Mockito.`when`(contributionRepo.findById(contributionId)).thenReturn(Optional.of(contribution()))
    Mockito.`when`(userRepo.findAllById(listOf(authorId))).thenReturn(listOf(author()))

    val item = service.reject(moderatorId, contributionId, "   ")

    assertNull(item.resolutionNote)
  }

  @Test
  fun `rejecting a resolved contribution is a conflict`() {
    Mockito
      .`when`(contributionRepo.findById(contributionId))
      .thenReturn(Optional.of(contribution(status = ContributionStatus.applied)))

    assertThrows<ConflictException> { service.reject(moderatorId, contributionId, null) }
  }

  @Test
  fun `an unknown contribution is a 404`() {
    Mockito.`when`(contributionRepo.findById(contributionId)).thenReturn(Optional.empty())

    assertThrows<NotFoundException> { service.get(contributionId) }
    assertThrows<NotFoundException> { service.approve(moderatorId, contributionId) }
    assertThrows<NotFoundException> { service.reject(moderatorId, contributionId, null) }
  }
}
