package mg.fikaliako.api.endpoint.rest.controller

import mg.fikaliako.api.endpoint.rest.model.RankingCatalog
import mg.fikaliako.api.endpoint.rest.model.RankingPage
import mg.fikaliako.api.service.RankingService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/rankings")
class RankingController(
  private val service: RankingService,
) {
  @GetMapping
  fun catalog(): RankingCatalog = service.catalog()

  @GetMapping("/{topicId}")
  fun top(
    @PathVariable topicId: String,
  ): RankingPage = service.top(topicId)
}
