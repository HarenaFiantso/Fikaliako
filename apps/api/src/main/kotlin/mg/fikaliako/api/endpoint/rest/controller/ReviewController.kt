package mg.fikaliako.api.endpoint.rest.controller
import jakarta.validation.Valid
import mg.fikaliako.api.endpoint.rest.model.Page
import mg.fikaliako.api.endpoint.rest.model.ReviewInput
import mg.fikaliako.api.endpoint.rest.model.ReviewItem
import mg.fikaliako.api.service.ReviewService
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
import java.util.UUID

@RestController
@RequestMapping("/v1/establishments/{establishmentId}/reviews")
class ReviewController(
  private val service: ReviewService,
) {
  @GetMapping
  fun list(
    @PathVariable establishmentId: UUID,
    @RequestParam(required = false) limit: Int?,
    @RequestParam(required = false) cursor: String?,
  ): Page<ReviewItem> = service.listForEstablishment(establishmentId, limit, cursor)

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  fun create(
    @PathVariable establishmentId: UUID,
    @AuthenticationPrincipal jwt: Jwt,
    @Valid @RequestBody input: ReviewInput,
  ): ReviewItem = service.create(establishmentId, jwt.userId(), input)
}
