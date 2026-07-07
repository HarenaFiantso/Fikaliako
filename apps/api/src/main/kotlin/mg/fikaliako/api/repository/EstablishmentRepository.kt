package mg.fikaliako.api.repository
import mg.fikaliako.api.endpoint.rest.model.EstablishmentFilters
import mg.fikaliako.api.endpoint.rest.model.EstablishmentSummary
import mg.fikaliako.api.model.Establishment
import mg.fikaliako.api.util.Cursor
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

interface EstablishmentRepository :
  JpaRepository<Establishment, UUID>,
  EstablishmentSearchRepository {
  fun findBySlug(slug: String): Establishment?
}

data class EstablishmentListRow(
  val summary: EstablishmentSummary,
  val createdAt: Instant,
)

interface EstablishmentSearchRepository {
  fun searchNearby(
    lat: Double,
    lng: Double,
    radiusM: Double,
    filters: EstablishmentFilters,
    limit: Int,
    now: Instant,
  ): List<EstablishmentSummary>

  fun searchList(
    filters: EstablishmentFilters,
    limit: Int,
    cursor: Cursor?,
    now: Instant,
  ): List<EstablishmentListRow>
}
