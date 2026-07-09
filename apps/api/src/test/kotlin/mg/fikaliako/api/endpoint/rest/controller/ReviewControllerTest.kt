package mg.fikaliako.api.endpoint.rest.controller

import mg.fikaliako.api.config.SecurityConfig
import mg.fikaliako.api.endpoint.rest.model.Page
import mg.fikaliako.api.endpoint.rest.model.ReviewInput
import mg.fikaliako.api.endpoint.rest.model.ReviewItem
import mg.fikaliako.api.service.ReviewService
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
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
  fun `posting a review requires authentication`() {
    mockMvc
      .post("/v1/establishments/$establishmentId/reviews") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"rating_quality": 5, "rating_price": 4, "rating_cleanliness": 4, "rating_speed": 3, "rating_welcome": 5}"""
      }.andExpect {
        status { isUnauthorized() }
        content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
      }
  }

  @Test
  fun `an authenticated user can post a review`() {
    val authorId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001")
    val input = ReviewInput(5, 4, 4, 3, 5, comment = "Tsara be ny hena!")
    Mockito
      .`when`(service.create(establishmentId, authorId, input))
      .thenReturn(
        ReviewItem(
          id = UUID.fromString("663c4707-280a-4ac1-a2fb-d0bafeea3af3"),
          authorName = "Naina",
          ratingQuality = 5,
          ratingPrice = 4,
          ratingCleanliness = 4,
          ratingSpeed = 3,
          ratingWelcome = 5,
          globalNote = BigDecimal("4.33"),
          comment = "Tsara be ny hena!",
          createdAt = Instant.parse("2026-07-09T12:00:00Z"),
        ),
      )

    mockMvc
      .post("/v1/establishments/$establishmentId/reviews") {
        with(jwt().jwt { it.subject(authorId.toString()) })
        contentType = MediaType.APPLICATION_JSON
        content =
          """
          {"rating_quality": 5, "rating_price": 4, "rating_cleanliness": 4,
           "rating_speed": 3, "rating_welcome": 5, "comment": "Tsara be ny hena!"}
          """.trimIndent()
      }.andExpect {
        status { isCreated() }
        jsonPath("$.global_note") { value(4.33) }
        jsonPath("$.author_name") { value("Naina") }
      }
  }

  @Test
  fun `review ratings outside 1-5 are rejected`() {
    mockMvc
      .post("/v1/establishments/$establishmentId/reviews") {
        with(jwt().jwt { it.subject("bbbbbbbb-0000-0000-0000-000000000001") })
        contentType = MediaType.APPLICATION_JSON
        content = """{"rating_quality": 6, "rating_price": 4, "rating_cleanliness": 4, "rating_speed": 3, "rating_welcome": 5}"""
      }.andExpect {
        status { isBadRequest() }
        content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
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
