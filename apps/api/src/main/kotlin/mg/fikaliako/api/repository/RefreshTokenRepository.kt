package mg.fikaliako.api.repository
import mg.fikaliako.api.model.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface RefreshTokenRepository : JpaRepository<RefreshToken, UUID> {
  @Query("SELECT r FROM RefreshToken r JOIN FETCH r.user WHERE r.tokenHash = :tokenHash")
  fun findByTokenHash(
    @Param("tokenHash") tokenHash: String,
  ): RefreshToken?

  @Modifying
  @Query("UPDATE RefreshToken r SET r.revokedAt = :now WHERE r.familyId = :familyId AND r.revokedAt IS NULL")
  fun revokeFamily(
    @Param("familyId") familyId: UUID,
    @Param("now") now: Instant,
  ): Int

  @Modifying
  @Query("UPDATE RefreshToken r SET r.revokedAt = :now WHERE r.user.id = :userId AND r.revokedAt IS NULL")
  fun revokeAllForUser(
    @Param("userId") userId: UUID,
    @Param("now") now: Instant,
  ): Int
}
