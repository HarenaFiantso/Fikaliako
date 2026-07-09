package mg.fikaliako.api.repository
import mg.fikaliako.api.model.EstablishmentManager
import mg.fikaliako.api.model.EstablishmentManagerId
import mg.fikaliako.api.model.UserAccount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

data class ManagerRow(
  val user: UserAccount,
  val grantedAt: Instant,
)

interface EstablishmentManagerRepository : JpaRepository<EstablishmentManager, EstablishmentManagerId> {
  @Query(
    """
        SELECT m FROM EstablishmentManager m JOIN FETCH m.establishment
        WHERE m.id.userId = :userId
        ORDER BY m.establishment.name
        """,
  )
  fun findAllForUser(
    @Param("userId") userId: UUID,
  ): List<EstablishmentManager>

  @Query(
    """
        SELECT new mg.fikaliako.api.repository.ManagerRow(u, m.createdAt)
        FROM EstablishmentManager m, UserAccount u
        WHERE u.id = m.id.userId AND m.id.establishmentId = :establishmentId
        ORDER BY m.createdAt
        """,
  )
  fun findManagersOf(
    @Param("establishmentId") establishmentId: UUID,
  ): List<ManagerRow>
}
