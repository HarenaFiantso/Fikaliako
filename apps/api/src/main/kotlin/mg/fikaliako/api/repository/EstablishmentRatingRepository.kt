package mg.fikaliako.api.repository

import mg.fikaliako.api.entity.EstablishmentRating
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EstablishmentRatingRepository : JpaRepository<EstablishmentRating, UUID>
