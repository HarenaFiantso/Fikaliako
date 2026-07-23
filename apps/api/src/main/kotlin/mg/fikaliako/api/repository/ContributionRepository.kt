package mg.fikaliako.api.repository

import mg.fikaliako.api.model.Contribution
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ContributionRepository : JpaRepository<Contribution, UUID>
