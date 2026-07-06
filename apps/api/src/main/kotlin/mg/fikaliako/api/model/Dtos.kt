package mg.fikaliako.api.model

/** WGS 84 coordinate (SRID 4326). `lat` ∈ [-90, 90], `lng` ∈ [-180, 180]. */
data class GeoPoint(
    val lat: Double,
    val lng: Double,
)

/**
 * Cursor-paginated slice (project book ch. 8). [nextCursor] is null on the last
 * page; pass it back as `?cursor=` to fetch the following page.
 */
data class Page<T>(
    val items: List<T>,
    val nextCursor: String? = null,
)
