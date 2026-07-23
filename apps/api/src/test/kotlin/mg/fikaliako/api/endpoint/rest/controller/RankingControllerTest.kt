package mg.fikaliako.api.endpoint.rest.controller

import mg.fikaliako.api.config.SecurityConfig
import mg.fikaliako.api.endpoint.rest.model.EstablishmentSummary
import mg.fikaliako.api.endpoint.rest.model.GeoPoint
import mg.fikaliako.api.endpoint.rest.model.RankingCatalog
import mg.fikaliako.api.endpoint.rest.model.RankingPage
import mg.fikaliako.api.endpoint.rest.model.RankingTopicItem
import mg.fikaliako.api.model.exception.NotFoundException
import mg.fikaliako.api.service.RankingService
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.util.UUID

@WebMvcTest(RankingController::class)
@Import(SecurityConfig::class)
class RankingControllerTest {
  @Autowired
  lateinit var mockMvc: MockMvc

  @MockitoBean
  lateinit var service: RankingService

  @Test
  fun `the catalog is public`() {
    Mockito
      .`when`(service.catalog())
      .thenReturn(RankingCatalog(listOf(RankingTopicItem("top-rated", "Les mieux notés", "Ny tsara naoty indrindra"))))

    mockMvc.get("/v1/rankings").andExpect {
      status { isOk() }
      jsonPath("$.items[0].id") { value("top-rated") }
      jsonPath("$.items[0].title_fr") { value("Les mieux notés") }
    }
  }

  @Test
  fun `a top serializes its establishments`() {
    val item =
      EstablishmentSummary(
        id = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001"),
        slug = "mama-fara",
        name = "Chez Mama Fara",
        type = "gargotte",
        position = GeoPoint(-18.9, 47.5),
        avgPriceAr = 4000,
        verified = true,
        status = "active",
        ratingAvg = null,
        ratingCount = 12,
      )
    Mockito
      .`when`(service.top("best-gargottes"))
      .thenReturn(
        RankingPage(
          RankingTopicItem("best-gargottes", "Meilleures gargottes", "Hotely gasy tsara indrindra"),
          listOf(item),
        ),
      )

    mockMvc.get("/v1/rankings/best-gargottes").andExpect {
      status { isOk() }
      jsonPath("$.topic.id") { value("best-gargottes") }
      jsonPath("$.items[0].slug") { value("mama-fara") }
    }
  }

  @Test
  fun `an unknown topic is a problem+json 404`() {
    Mockito.`when`(service.top("nope")).thenThrow(NotFoundException("Ranking 'nope' not found."))

    mockMvc.get("/v1/rankings/nope").andExpect {
      status { isNotFound() }
      content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
    }
  }
}
