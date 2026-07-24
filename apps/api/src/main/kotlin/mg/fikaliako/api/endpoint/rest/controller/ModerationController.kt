package mg.fikaliako.api.endpoint.rest.controller

import jakarta.validation.Valid
import mg.fikaliako.api.endpoint.rest.model.ContributionItem
import mg.fikaliako.api.endpoint.rest.model.Page
import mg.fikaliako.api.endpoint.rest.model.RejectContributionRequest
import mg.fikaliako.api.service.ModerationService
import mg.fikaliako.api.util.userId
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/v1/moderation/contributions")
class ModerationController(
  private val service: ModerationService,
) {
  @GetMapping
  fun queue(
    @RequestParam(required = false) status: String?,
    @RequestParam(required = false) limit: Int?,
    @RequestParam(required = false) cursor: String?,
  ): Page<ContributionItem> = service.queue(status, limit, cursor)

  @GetMapping("/{contributionId}")
  fun get(
    @PathVariable contributionId: UUID,
  ): ContributionItem = service.get(contributionId)

  @PostMapping("/{contributionId}/approve")
  fun approve(
    @AuthenticationPrincipal jwt: Jwt,
    @PathVariable contributionId: UUID,
  ): ContributionItem = service.approve(jwt.userId(), contributionId)

  @PostMapping("/{contributionId}/reject")
  fun reject(
    @AuthenticationPrincipal jwt: Jwt,
    @PathVariable contributionId: UUID,
    @Valid @RequestBody(required = false) request: RejectContributionRequest?,
  ): ContributionItem = service.reject(jwt.userId(), contributionId, request?.reason)
}
