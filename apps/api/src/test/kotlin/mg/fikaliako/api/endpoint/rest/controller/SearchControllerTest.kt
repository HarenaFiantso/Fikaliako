package mg.fikaliako.api.endpoint.rest.controller

import mg.fikaliako.api.config.SecurityConfig
import mg.fikaliako.api.endpoint.rest.model.EstablishmentFilters
import mg.fikaliako.api.endpoint.rest.model.EstablishmentSummary
import mg.fikaliako.api.endpoint.rest.model.GeoPoint
import mg.fikaliako.api.endpoint.rest.model.Page
import mg.fikaliako.api.service.SearchService
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

@WebMvcTest(SearchController::class)
@Import(SecurityConfig::class)
class SearchControllerTest {
  @Autowired
  lateinit var mockMvc: MockMvc

  @MockitoBean
  lateinit var service: SearchService

  @Test
  fun `search is public and forwards the filters`() {
    val item =
      EstablishmentSummary(
        id = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001"),
        slug = "mama-fara",
        name = "Gargotte Chez Mama Fara",
        type = "gargotte",
        position = GeoPoint(-18.9095, 47.5263),
        avgPriceAr = 4000,
        verified = true,
        status = "active",
        ratingAvg = null,
        ratingCount = 0,
      )
    Mockito
      .`when`(
        service.search(
          "romazava",
          EstablishmentFilters(maxPrice = 5000),
          null,
          null,
          null,
          null,
          null,
        ),
      ).thenReturn(Page(listOf(item)))

    mockMvc
      .get("/v1/search") {
        param("q", "romazava")
        param("max_price", "5000")
      }.andExpect {
        status { isOk() }
        jsonPath("$.items[0].slug") { value("mama-fara") }
        jsonPath("$.items[0].avg_price_ar") { value(4000) }
      }
  }

  @Test
  fun `missing q is a problem+json 400`() {
    mockMvc.get("/v1/search").andExpect {
      status { isBadRequest() }
      content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
      jsonPath("$.detail") { value("Missing required parameter 'q'.") }
    }
  }
}
