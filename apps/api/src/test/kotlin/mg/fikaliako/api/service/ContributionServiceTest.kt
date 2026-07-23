package mg.fikaliako.api.service

import mg.fikaliako.api.endpoint.rest.model.EstablishmentProposal
import mg.fikaliako.api.endpoint.rest.model.GeoPoint
import mg.fikaliako.api.model.Contribution
import mg.fikaliako.api.model.ContributionStatus
import mg.fikaliako.api.model.ContributionType
import mg.fikaliako.api.model.exception.BadRequestException
import mg.fikaliako.api.model.exception.UnauthorizedException
import mg.fikaliako.api.repository.ContributionRepository
import mg.fikaliako.api.repository.UserAccountRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import tools.jackson.databind.json.JsonMapper
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ContributionServiceTest {
  private val now = Instant.parse("2026-07-06T09:00:00Z")
  private val contributionRepo = Mockito.mock(ContributionRepository::class.java)
  private val userRepo = Mockito.mock(UserAccountRepository::class.java)
  private val service =
    ContributionService(contributionRepo, userRepo, JsonMapper.builder().build(), Clock.fixed(now, ZoneOffset.UTC))

  private val authorId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001")

  private fun proposal() =
    EstablishmentProposal(
      name = " Chez Bao ",
      type = "gargotte",
      position = GeoPoint(-18.91, 47.52),
      avgPriceAr = 3000,
    )

  @Test
  fun `a proposal is queued as a pending create contribution`() {
    Mockito.`when`(userRepo.existsById(authorId)).thenReturn(true)

    val receipt = service.proposeEstablishment(authorId, proposal())

    val captor = ArgumentCaptor.forClass(Contribution::class.java)
    Mockito.verify(contributionRepo).save(captor.capture())
    val saved = captor.value
    assertEquals(ContributionType.create, saved.type)
    assertEquals(ContributionStatus.pending, saved.status)
    assertEquals(authorId, saved.authorId)
    assertNull(saved.establishmentId)
    assertEquals(now, saved.createdAt)
    assertTrue(saved.payload.contains("\"name\":\"Chez Bao\""))
    assertTrue(saved.payload.contains("\"type\":\"gargotte\""))
    assertTrue(saved.payload.contains("\"lat\":-18.91"))
    assertTrue(saved.payload.contains("\"lng\":47.52"))
    assertTrue(saved.payload.contains("\"avg_price_ar\":3000"))
    assertTrue(saved.payload.contains("\"city\":\"Antananarivo\""))

    assertEquals(saved.id, receipt.id)
    assertEquals("pending", receipt.status)
    assertEquals(now, receipt.createdAt)
  }

  @Test
  fun `blank optional fields are dropped from the payload`() {
    Mockito.`when`(userRepo.existsById(authorId)).thenReturn(true)

    service.proposeEstablishment(
      authorId,
      proposal().copy(address = "  ", district = "", city = " Antsirabe ", comment = " Mofo gasy tsara "),
    )

    val captor = ArgumentCaptor.forClass(Contribution::class.java)
    Mockito.verify(contributionRepo).save(captor.capture())
    val payload = captor.value.payload
    assertFalse(payload.contains("address"))
    assertFalse(payload.contains("district"))
    assertTrue(payload.contains("\"city\":\"Antsirabe\""))
    assertTrue(payload.contains("\"comment\":\"Mofo gasy tsara\""))
  }

  @Test
  fun `an unknown establishment type is rejected`() {
    Mockito.`when`(userRepo.existsById(authorId)).thenReturn(true)

    val error =
      assertThrows<BadRequestException> {
        service.proposeEstablishment(authorId, proposal().copy(type = "spaceship"))
      }
    assertTrue(error.message!!.contains("spaceship"))
  }

  @Test
  fun `a purged account cannot propose`() {
    Mockito.`when`(userRepo.existsById(authorId)).thenReturn(false)

    assertThrows<UnauthorizedException> { service.proposeEstablishment(authorId, proposal()) }
    Mockito.verifyNoInteractions(contributionRepo)
  }
}
