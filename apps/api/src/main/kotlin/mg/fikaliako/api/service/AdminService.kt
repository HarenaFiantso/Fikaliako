package mg.fikaliako.api.service
import mg.fikaliako.api.endpoint.rest.model.ManagerItem
import mg.fikaliako.api.endpoint.rest.model.Page
import mg.fikaliako.api.model.AuditLogEntry
import mg.fikaliako.api.model.EstablishmentManager
import mg.fikaliako.api.model.EstablishmentManagerId
import mg.fikaliako.api.model.UserRole
import mg.fikaliako.api.model.exception.BadRequestException
import mg.fikaliako.api.model.exception.NotFoundException
import mg.fikaliako.api.repository.AuditLogRepository
import mg.fikaliako.api.repository.EstablishmentManagerRepository
import mg.fikaliako.api.repository.EstablishmentRepository
import mg.fikaliako.api.repository.UserAccountRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.util.UUID

// Admin back-office: grants BUSINESS accounts management rights over
// establishments. Every grant/revoke lands in the immutable audit log
// (book ch. 7.3).
@Service
class AdminService(
  private val managers: EstablishmentManagerRepository,
  private val establishments: EstablishmentRepository,
  private val users: UserAccountRepository,
  private val auditLog: AuditLogRepository,
  private val clock: Clock,
) {
  @Transactional(readOnly = true)
  fun listManagers(establishmentId: UUID): Page<ManagerItem> {
    if (!establishments.existsById(establishmentId)) {
      throw NotFoundException("Establishment '$establishmentId' not found.")
    }
    val items =
      managers.findManagersOf(establishmentId).map {
        ManagerItem(
          userId = requireNotNull(it.user.id),
          displayName = it.user.displayName,
          phone = it.user.phone,
          grantedAt = it.grantedAt,
        )
      }
    return Page(items)
  }

  // Idempotent PUT: granting twice is not an error and is not re-audited
  @Transactional
  fun grantManager(
    actorId: UUID,
    establishmentId: UUID,
    userId: UUID,
  ) {
    val establishment =
      establishments.findById(establishmentId).orElseThrow {
        NotFoundException("Establishment '$establishmentId' not found.")
      }
    val user = users.findById(userId).orElseThrow { NotFoundException("User '$userId' not found.") }
    if (user.role != UserRole.BUSINESS) {
      throw BadRequestException("Only business accounts can manage establishments (user role: ${user.role.name.lowercase()}).")
    }
    val id = EstablishmentManagerId(establishmentId = establishmentId, userId = userId)
    if (managers.existsById(id)) return
    val now = clock.instant()
    managers.save(
      EstablishmentManager(id = id, establishment = establishment, grantedBy = actorId, createdAt = now),
    )
    audit(actorId, "establishment_manager.granted", establishmentId, userId)
  }

  @Transactional
  fun revokeManager(
    actorId: UUID,
    establishmentId: UUID,
    userId: UUID,
  ) {
    val id = EstablishmentManagerId(establishmentId = establishmentId, userId = userId)
    if (!managers.existsById(id)) return
    managers.deleteById(id)
    audit(actorId, "establishment_manager.revoked", establishmentId, userId)
  }

  private fun audit(
    actorId: UUID,
    action: String,
    establishmentId: UUID,
    userId: UUID,
  ) {
    auditLog.save(
      AuditLogEntry(
        actorId = actorId,
        action = action,
        entityType = "establishment",
        entityId = establishmentId,
        details = """{"user_id":"$userId"}""",
        createdAt = clock.instant(),
      ),
    )
  }
}
