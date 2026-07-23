package mg.fikaliako.api.repository

import mg.fikaliako.api.endpoint.rest.model.EstablishmentFilters
import mg.fikaliako.api.endpoint.rest.model.EstablishmentSummary
import java.time.Instant

/**
 * Port for typo-tolerant text search (project book ch. 4.2). The current
 * adapter runs on PostgreSQL pg_trgm; the target Meilisearch engine (ch. 7.1)
 * replaces the adapter behind this interface without touching the service layer.
 */
interface EstablishmentTextSearchRepository {
  /**
   * Relevance-ordered search over establishment names, districts, cities and
   * cuisine labels. [synonymCuisines]/[synonymTypes] come from FR/MG synonym
   * expansion (e.g. "romazava" → malagasy cuisine) and count as matches even
   * when the raw text does not appear anywhere.
   */
  fun searchText(
    query: String,
    synonymCuisines: Set<String>,
    synonymTypes: Set<String>,
    filters: EstablishmentFilters,
    geo: GeoSearchContext?,
    limit: Int,
    offset: Int,
    now: Instant,
  ): List<EstablishmentSummary>
}

/** Optional geographic context: annotates hits with distance and narrows by radius. */
data class GeoSearchContext(
  val lat: Double,
  val lng: Double,
  val radiusM: Double?,
)
