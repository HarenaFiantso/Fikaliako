package mg.fikaliako.api.repository
import mg.fikaliako.api.model.Favorite
import mg.fikaliako.api.model.FavoriteId
import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface FavoriteRepository : JpaRepository<Favorite, FavoriteId> {
  @Query(
    """
        SELECT f FROM Favorite f JOIN FETCH f.establishment
        WHERE f.id.userId = :userId
        ORDER BY f.createdAt DESC, f.id.establishmentId DESC
        """,
  )
  fun findForUser(
    @Param("userId") userId: UUID,
    limit: Limit,
  ): List<Favorite>

  @Query(
    """
        SELECT f FROM Favorite f JOIN FETCH f.establishment
        WHERE f.id.userId = :userId
          AND (f.createdAt < :cursorCreatedAt
            OR (f.createdAt = :cursorCreatedAt AND f.id.establishmentId < :cursorId))
        ORDER BY f.createdAt DESC, f.id.establishmentId DESC
        """,
  )
  fun findForUserAfter(
    @Param("userId") userId: UUID,
    @Param("cursorCreatedAt") cursorCreatedAt: Instant,
    @Param("cursorId") cursorId: UUID,
    limit: Limit,
  ): List<Favorite>
}
