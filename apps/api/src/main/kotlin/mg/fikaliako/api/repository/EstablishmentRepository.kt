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
  EstablishmentSearchRepository,
  EstablishmentTextSearchRepository {
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

  /**
   * Filter-only smart search (project book ch. 4.2): establishments ordered by
   * the deterministic discovery score mixing Bayesian note, recent review
   * volume and budget adequacy.
   */
  fun searchDiscovery(
    geo: GeoSearchContext?,
    filters: EstablishmentFilters,
    limit: Int,
    offset: Int,
    now: Instant,
  ): List<EstablishmentSummary>
}
