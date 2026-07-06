package mg.fikaliako.api.controller

import mg.fikaliako.api.config.SecurityConfig
import mg.fikaliako.api.model.Page
import mg.fikaliako.api.model.ReviewItem
import mg.fikaliako.api.service.ReviewService
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@WebMvcTest(ReviewController::class)
@Import(SecurityConfig::class)
class ReviewControllerTest {
  @Autowired
  lateinit var mockMvc: MockMvc

  @MockitoBean
  lateinit var service: ReviewService

  private val establishmentId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001")

  @Test
  fun `lists reviews with the weighted global note`() {
    val review =
      ReviewItem(
        id = UUID.fromString("663c4707-280a-4ac1-a2fb-d0bafeea3af3"),
        authorName = "Naina",
        ratingQuality = 5,
        ratingPrice = 5,
        ratingCleanliness = 4,
        ratingSpeed = 4,
        ratingWelcome = 5,
        globalNote = BigDecimal("4.67"),
        comment = "Tsara be ny hena!",
        createdAt = Instant.parse("2026-07-06T09:00:00Z"),
      )
    Mockito
      .`when`(service.listForEstablishment(establishmentId, null, null))
      .thenReturn(Page(listOf(review)))

    mockMvc.get("/v1/establishments/$establishmentId/reviews").andExpect {
      status { isOk() }
      jsonPath("$.items[0].author_name") { value("Naina") }
      jsonPath("$.items[0].global_note") { value(4.67) }
    }
  }

  @Test
  fun `a non-UUID establishment id is a 400`() {
    mockMvc.get("/v1/establishments/not-a-uuid/reviews").andExpect {
      status { isBadRequest() }
      content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
    }
  }
}
