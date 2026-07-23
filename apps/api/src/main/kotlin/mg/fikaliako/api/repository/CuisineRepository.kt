package mg.fikaliako.api.repository

import mg.fikaliako.api.model.Cuisine
import org.springframework.data.jpa.repository.JpaRepository

interface CuisineRepository : JpaRepository<Cuisine, String> {
  fun findAllByOrderBySortOrder(): List<Cuisine>
}
