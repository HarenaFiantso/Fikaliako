package mg.fikaliako.api.endpoint.rest.controller
import jakarta.validation.Valid
import mg.fikaliako.api.endpoint.rest.model.EstablishmentSummary
import mg.fikaliako.api.endpoint.rest.model.Page
import mg.fikaliako.api.endpoint.rest.model.UpdateProfileRequest
import mg.fikaliako.api.endpoint.rest.model.UserProfile
import mg.fikaliako.api.service.UserService
import mg.fikaliako.api.util.userId
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/v1/users/me")
class UserController(
  private val service: UserService,
) {
  @GetMapping
  fun me(
    @AuthenticationPrincipal jwt: Jwt,
  ): UserProfile = service.me(jwt.userId())

  @PatchMapping
  fun updateMe(
    @AuthenticationPrincipal jwt: Jwt,
    @Valid @RequestBody request: UpdateProfileRequest,
  ): UserProfile = service.updateMe(jwt.userId(), request)

  @GetMapping("/favorites")
  fun favorites(
    @AuthenticationPrincipal jwt: Jwt,
    @RequestParam(required = false) limit: Int?,
    @RequestParam(required = false) cursor: String?,
  ): Page<EstablishmentSummary> = service.listFavorites(jwt.userId(), limit, cursor)

  @PutMapping("/favorites/{establishmentId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun addFavorite(
    @AuthenticationPrincipal jwt: Jwt,
    @PathVariable establishmentId: UUID,
  ) = service.addFavorite(jwt.userId(), establishmentId)

  @DeleteMapping("/favorites/{establishmentId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun removeFavorite(
    @AuthenticationPrincipal jwt: Jwt,
    @PathVariable establishmentId: UUID,
  ) = service.removeFavorite(jwt.userId(), establishmentId)
}
