package mg.fikaliako.api.endpoint.rest.controller
import jakarta.validation.Valid
import mg.fikaliako.api.endpoint.rest.model.BusinessEstablishmentUpdate
import mg.fikaliako.api.endpoint.rest.model.EstablishmentDetail
import mg.fikaliako.api.endpoint.rest.model.EstablishmentSummary
import mg.fikaliako.api.endpoint.rest.model.OpeningHoursUpdate
import mg.fikaliako.api.endpoint.rest.model.Page
import mg.fikaliako.api.service.BusinessEstablishmentService
import mg.fikaliako.api.util.userId
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

// Premium-account surface; the /v1/business/** route rule already requires
// ROLE_BUSINESS or ROLE_ADMIN, the service then checks the manager link
@RestController
@RequestMapping("/v1/business/establishments")
class BusinessController(
  private val service: BusinessEstablishmentService,
) {
  @GetMapping
  fun listManaged(
    @AuthenticationPrincipal jwt: Jwt,
  ): Page<EstablishmentSummary> = service.listManaged(jwt.userId())

  @PatchMapping("/{establishmentId}")
  fun updateProfile(
    @AuthenticationPrincipal jwt: Jwt,
    @PathVariable establishmentId: UUID,
    @Valid @RequestBody patch: BusinessEstablishmentUpdate,
  ): EstablishmentDetail = service.updateProfile(jwt.userId(), jwt.isAdmin(), establishmentId, patch)

  @PutMapping("/{establishmentId}/opening-hours")
  fun replaceOpeningHours(
    @AuthenticationPrincipal jwt: Jwt,
    @PathVariable establishmentId: UUID,
    @Valid @RequestBody update: OpeningHoursUpdate,
  ): EstablishmentDetail = service.replaceOpeningHours(jwt.userId(), jwt.isAdmin(), establishmentId, update)

  private fun Jwt.isAdmin(): Boolean = getClaimAsString("role") == "admin"
}
