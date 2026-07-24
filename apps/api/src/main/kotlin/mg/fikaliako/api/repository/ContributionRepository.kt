package mg.fikaliako.api.repository

import mg.fikaliako.api.model.Contribution
import mg.fikaliako.api.model.ContributionStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

interface ContributionRepository : JpaRepository<Contribution, UUID> {
  // The moderation queue is FIFO (book ch. 4.8): oldest submissions first.
  fun findByStatusOrderByCreatedAtAscIdAsc(
    status: ContributionStatus,
    pageable: Pageable,
  ): List<Contribution>

  @Query(
    """
    SELECT c FROM Contribution c
    WHERE c.status = :status
      AND (c.createdAt > :afterCreatedAt OR (c.createdAt = :afterCreatedAt AND c.id > :afterId))
    ORDER BY c.createdAt ASC, c.id ASC
    """,
  )
  fun findByStatusAfter(
    status: ContributionStatus,
    afterCreatedAt: Instant,
    afterId: UUID,
    pageable: Pageable,
  ): List<Contribution>
}
