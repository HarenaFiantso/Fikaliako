package mg.fikaliako.api.endpoint.rest.controller

import mg.fikaliako.api.endpoint.rest.model.EstablishmentSummary
import mg.fikaliako.api.endpoint.rest.model.Page
import mg.fikaliako.api.service.EstablishmentFilterParams
import mg.fikaliako.api.service.EstablishmentService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

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
