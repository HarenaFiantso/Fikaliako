package mg.fikaliako.api.endpoint.rest.controller
import mg.fikaliako.api.endpoint.rest.model.Page
import mg.fikaliako.api.endpoint.rest.model.ReviewItem
import mg.fikaliako.api.service.ReviewService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
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
}
