package mg.fikaliako.api.controller

import mg.fikaliako.api.model.EstablishmentDetail
import mg.fikaliako.api.model.EstablishmentSummary
import mg.fikaliako.api.model.Page
import mg.fikaliako.api.service.EstablishmentFilterParams
import mg.fikaliako.api.service.EstablishmentService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/establishments")
class EstablishmentController(
    private val service: EstablishmentService,
) {
    /** Browse establishments, newest first, cursor-paginated. */
    @GetMapping
    fun list(
        @RequestParam(required = false) type: String?,
        @RequestParam(name = "min_price", required = false) minPrice: Int?,
        @RequestParam(name = "max_price", required = false) maxPrice: Int?,
        @RequestParam(required = false) cuisine: String?,
        @RequestParam(required = false) payment: String?,
        @RequestParam(required = false) filters: String?,
        @RequestParam(name = "open_now", defaultValue = "false") openNow: Boolean,
        @RequestParam(required = false) limit: Int?,
        @RequestParam(required = false) cursor: String?,
    ): Page<EstablishmentSummary> {
        val parsed = EstablishmentFilterParams.build(type, minPrice, maxPrice, cuisine, payment, filters, openNow)
        return service.list(parsed, limit, cursor)
    }

    /** Establishment detail, resolvable by UUID or SEO slug (book ch. 11.3). */
    @GetMapping("/{idOrSlug}")
    fun detail(
        @PathVariable idOrSlug: String,
    ): EstablishmentDetail = service.detail(idOrSlug)
}
