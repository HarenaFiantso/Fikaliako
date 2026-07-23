package mg.fikaliako.api.endpoint.rest.controller

import mg.fikaliako.api.endpoint.rest.model.SearchPage
import mg.fikaliako.api.service.EstablishmentFilterParams
import mg.fikaliako.api.service.SearchService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/search")
class SearchController(
  private val service: SearchService,
) {
  @GetMapping
  fun search(
    @RequestParam q: String,
    @RequestParam(required = false) lat: Double?,
    @RequestParam(required = false) lng: Double?,
    @RequestParam(required = false) radius: Double?,
    @RequestParam(required = false) type: String?,
    @RequestParam(name = "min_price", required = false) minPrice: Int?,
    @RequestParam(name = "max_price", required = false) maxPrice: Int?,
    @RequestParam(required = false) cuisine: String?,
    @RequestParam(required = false) payment: String?,
    @RequestParam(required = false) filters: String?,
    @RequestParam(name = "open_now", defaultValue = "false") openNow: Boolean,
    @RequestParam(required = false) limit: Int?,
    @RequestParam(required = false) cursor: String?,
  ): SearchPage {
    val parsed = EstablishmentFilterParams.build(type, minPrice, maxPrice, cuisine, payment, filters, openNow)
    return service.search(q, parsed, lat, lng, radius, limit, cursor)
  }
}
