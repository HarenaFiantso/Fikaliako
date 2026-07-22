package mg.fikaliako.api.endpoint.rest.controller

import mg.fikaliako.api.endpoint.rest.model.ManagerItem
import mg.fikaliako.api.endpoint.rest.model.Page
import mg.fikaliako.api.service.AdminService
import mg.fikaliako.api.util.userId
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/v1/admin/establishments/{establishmentId}/managers")
class AdminController(
  private val service: AdminService,
) {
  @GetMapping
  fun list(
    @PathVariable establishmentId: UUID,
  ): Page<ManagerItem> = service.listManagers(establishmentId)

  @PutMapping("/{userId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun grant(
    @AuthenticationPrincipal jwt: Jwt,
    @PathVariable establishmentId: UUID,
    @PathVariable userId: UUID,
  ) = service.grantManager(jwt.userId(), establishmentId, userId)

  @DeleteMapping("/{userId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun revoke(
    @AuthenticationPrincipal jwt: Jwt,
    @PathVariable establishmentId: UUID,
    @PathVariable userId: UUID,
  ) = service.revokeManager(jwt.userId(), establishmentId, userId)
}
