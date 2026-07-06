package mg.fikaliako.api.controller

import mg.fikaliako.api.model.EstablishmentSummary
import mg.fikaliako.api.model.Page
import mg.fikaliako.api.service.EstablishmentFilterParams
import mg.fikaliako.api.service.EstablishmentService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * The core discovery query (project book ch. 6.2): active establishments within
 * a radius of a point, cheapest first, with the combinable filters applied. Uses
 * `ST_DWithin` so it stays on the GiST index; target < 300 ms p95 (ch. 9).
 */
@RestController
@RequestMapping("/v1/nearby")
class NearbyController(
    private val service: EstablishmentService,
) {
    @GetMapping
    fun nearby(
        @RequestParam lat: Double,
        @RequestParam lng: Double,
        @RequestParam(required = false) radius: Double?,
        @RequestParam(required = false) type: String?,
        @RequestParam(name = "min_price", required = false) minPrice: Int?,
        @RequestParam(name = "max_price", required = false) maxPrice: Int?,
        @RequestParam(required = false) cuisine: String?,
        @RequestParam(required = false) payment: String?,
        @RequestParam(required = false) filters: String?,
        @RequestParam(name = "open_now", defaultValue = "false") openNow: Boolean,
        @RequestParam(required = false) limit: Int?,
    ): Page<EstablishmentSummary> {
        val parsed = EstablishmentFilterParams.build(type, minPrice, maxPrice, cuisine, payment, filters, openNow)
        return service.nearby(lat, lng, radius, parsed, limit)
    }
}
