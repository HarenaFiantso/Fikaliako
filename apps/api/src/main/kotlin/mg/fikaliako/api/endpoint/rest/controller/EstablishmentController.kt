package mg.fikaliako.api.endpoint.rest.controller

import jakarta.validation.Valid
import mg.fikaliako.api.endpoint.rest.model.ContributionReceipt
import mg.fikaliako.api.endpoint.rest.model.EstablishmentDetail
import mg.fikaliako.api.endpoint.rest.model.EstablishmentProposal
import mg.fikaliako.api.endpoint.rest.model.EstablishmentSummary
import mg.fikaliako.api.endpoint.rest.model.Page
import mg.fikaliako.api.service.ContributionService
import mg.fikaliako.api.service.EstablishmentFilterParams
import mg.fikaliako.api.service.EstablishmentService
import mg.fikaliako.api.util.userId
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/establishments")
class EstablishmentController(
  private val service: EstablishmentService,
  private val contributionService: ContributionService,
) {
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

  @GetMapping("/{idOrSlug}")
  fun detail(
    @PathVariable idOrSlug: String,
  ): EstablishmentDetail = service.detail(idOrSlug)

  @PostMapping
  @ResponseStatus(HttpStatus.ACCEPTED)
  fun propose(
    @AuthenticationPrincipal jwt: Jwt,
    @Valid @RequestBody proposal: EstablishmentProposal,
  ): ContributionReceipt = contributionService.proposeEstablishment(jwt.userId(), proposal)
}
