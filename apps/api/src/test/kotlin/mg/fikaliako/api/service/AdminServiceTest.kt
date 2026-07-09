package mg.fikaliako.api.service
import mg.fikaliako.api.model.AuditLogEntry
import mg.fikaliako.api.model.Establishment
import mg.fikaliako.api.model.EstablishmentManagerId
import mg.fikaliako.api.model.UserAccount
import mg.fikaliako.api.model.UserRole
import mg.fikaliako.api.model.exception.BadRequestException
import mg.fikaliako.api.model.exception.NotFoundException
import mg.fikaliako.api.repository.AuditLogRepository
import mg.fikaliako.api.repository.EstablishmentManagerRepository
import mg.fikaliako.api.repository.EstablishmentRepository
import mg.fikaliako.api.repository.ManagerRow
import mg.fikaliako.api.repository.UserAccountRepository
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AdminServiceTest {
  private val now = Instant.parse("2026-07-09T12:00:00Z")
  private val managers = Mockito.mock(EstablishmentManagerRepository::class.java)
  private val establishments = Mockito.mock(EstablishmentRepository::class.java)
  private val users = Mockito.mock(UserAccountRepository::class.java)
  private val auditLog = Mockito.mock(AuditLogRepository::class.java)
  private val service = AdminService(managers, establishments, users, auditLog, Clock.fixed(now, ZoneOffset.UTC))

  private val actorId = UUID.fromString("dddddddd-0000-0000-0000-000000000001")
  private val estId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001")
  private val businessUserId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002")

  private fun businessUser() =
    UserAccount(
      id = businessUserId,
      displayName = "Chez Bao",
      phone = "+261340000002",
      role = UserRole.BUSINESS,
      createdAt = now,
    )

  @Test
  fun `granting writes the link and an audit entry`() {
    Mockito.`when`(establishments.findById(estId)).thenReturn(Optional.of(Establishment(id = estId)))
    Mockito.`when`(users.findById(businessUserId)).thenReturn(Optional.of(businessUser()))
    Mockito.`when`(managers.existsById(EstablishmentManagerId(estId, businessUserId))).thenReturn(false)

    service.grantManager(actorId, estId, businessUserId)

    Mockito.verify(managers).save(Mockito.any())
    val captor = ArgumentCaptor.forClass(AuditLogEntry::class.java)
    Mockito.verify(auditLog).save(captor.capture())
    assertEquals("establishment_manager.granted", captor.value.action)
    assertEquals(actorId, captor.value.actorId)
    assertEquals(estId, captor.value.entityId)
    assertEquals("""{"user_id":"$businessUserId"}""", captor.value.details)
  }

  @Test
  fun `granting twice is idempotent and not re-audited`() {
    Mockito.`when`(establishments.findById(estId)).thenReturn(Optional.of(Establishment(id = estId)))
    Mockito.`when`(users.findById(businessUserId)).thenReturn(Optional.of(businessUser()))
    Mockito.`when`(managers.existsById(EstablishmentManagerId(estId, businessUserId))).thenReturn(true)

    service.grantManager(actorId, estId, businessUserId)

    Mockito.verify(managers, Mockito.never()).save(Mockito.any())
    Mockito.verifyNoInteractions(auditLog)
  }

  @Test
  fun `only business accounts can be granted management`() {
    val consumer = businessUser().apply { role = UserRole.USER }
    Mockito.`when`(establishments.findById(estId)).thenReturn(Optional.of(Establishment(id = estId)))
    Mockito.`when`(users.findById(businessUserId)).thenReturn(Optional.of(consumer))

    assertFailsWith<BadRequestException> { service.grantManager(actorId, estId, businessUserId) }
  }

  @Test
  fun `revoking is idempotent and audited only when a link existed`() {
    val id = EstablishmentManagerId(estId, businessUserId)
    Mockito.`when`(managers.existsById(id)).thenReturn(true)
    service.revokeManager(actorId, estId, businessUserId)
    Mockito.verify(managers).deleteById(id)
    Mockito.verify(auditLog).save(Mockito.any())

    Mockito.`when`(managers.existsById(id)).thenReturn(false)
    service.revokeManager(actorId, estId, businessUserId)
    Mockito.verify(managers, Mockito.times(1)).deleteById(id)
    Mockito.verify(auditLog, Mockito.times(1)).save(Mockito.any())
  }

  @Test
  fun `listing managers of an unknown establishment is a 404`() {
    Mockito.`when`(establishments.existsById(estId)).thenReturn(false)
    assertFailsWith<NotFoundException> { service.listManagers(estId) }
  }

  @Test
  fun `lists managers with their grant date`() {
    Mockito.`when`(establishments.existsById(estId)).thenReturn(true)
    Mockito
      .`when`(managers.findManagersOf(estId))
      .thenReturn(listOf(ManagerRow(businessUser(), now.minusSeconds(3600))))

    val page = service.listManagers(estId)

    assertEquals(1, page.items.size)
    assertEquals("Chez Bao", page.items[0].displayName)
    assertEquals(now.minusSeconds(3600), page.items[0].grantedAt)
  }
}
