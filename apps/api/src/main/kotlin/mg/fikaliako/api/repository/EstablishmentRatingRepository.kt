package mg.fikaliako.api.repository

import mg.fikaliako.api.model.EstablishmentRating
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface EstablishmentRatingRepository : JpaRepository<EstablishmentRating, UUID> {
  @Query("SELECT r.establishmentId FROM EstablishmentRating r")
  fun establishmentIds(): List<UUID>
}
