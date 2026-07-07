package mg.fikaliako.api.repository
import mg.fikaliako.api.model.Review
import mg.fikaliako.api.model.ReviewStatus
import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface ReviewRepository : JpaRepository<Review, UUID> {
  @Query(
    """
        SELECT r FROM Review r JOIN FETCH r.author
        WHERE r.establishment.id = :establishmentId AND r.status = :status
        ORDER BY r.createdAt DESC, r.id DESC
        """,
  )
  fun findPublished(
    @Param("establishmentId") establishmentId: UUID,
    @Param("status") status: ReviewStatus,
    limit: Limit,
  ): List<Review>

  @Query(
    """
        SELECT r FROM Review r JOIN FETCH r.author
        WHERE r.establishment.id = :establishmentId AND r.status = :status
          AND (r.createdAt < :cursorCreatedAt OR (r.createdAt = :cursorCreatedAt AND r.id < :cursorId))
        ORDER BY r.createdAt DESC, r.id DESC
        """,
  )
  fun findPublishedAfter(
    @Param("establishmentId") establishmentId: UUID,
    @Param("status") status: ReviewStatus,
    @Param("cursorCreatedAt") cursorCreatedAt: Instant,
    @Param("cursorId") cursorId: UUID,
    limit: Limit,
  ): List<Review>
}
