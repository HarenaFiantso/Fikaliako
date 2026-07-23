package mg.fikaliako.api.endpoint.rest.controller

import mg.fikaliako.api.config.SecurityConfig
import mg.fikaliako.api.endpoint.rest.model.Amenities
import mg.fikaliako.api.endpoint.rest.model.ContributionReceipt
import mg.fikaliako.api.endpoint.rest.model.EstablishmentDetail
import mg.fikaliako.api.endpoint.rest.model.EstablishmentFilters
import mg.fikaliako.api.endpoint.rest.model.EstablishmentProposal
import mg.fikaliako.api.endpoint.rest.model.EstablishmentSummary
import mg.fikaliako.api.endpoint.rest.model.GeoPoint
import mg.fikaliako.api.endpoint.rest.model.OpeningInterval
import mg.fikaliako.api.endpoint.rest.model.Page
import mg.fikaliako.api.endpoint.rest.model.RatingSummary
import mg.fikaliako.api.endpoint.rest.model.ReferentialItem
import mg.fikaliako.api.model.exception.NotFoundException
import mg.fikaliako.api.service.ContributionService
import mg.fikaliako.api.service.EstablishmentService
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

@WebMvcTest(EstablishmentController::class)
@Import(SecurityConfig::class)
class EstablishmentControllerTest {
  @Autowired
  lateinit var mockMvc: MockMvc

  @MockitoBean
  lateinit var service: EstablishmentService

  @MockitoBean
  lateinit var contributionService: ContributionService

  @Test
  fun `list serializes items in snake_case with the next cursor`() {
    Mockito
      .`when`(service.list(EstablishmentFilters(), null, null))
      .thenReturn(Page(listOf(summary()), "next-cur"))

    mockMvc.get("/v1/establishments").andExpect {
      status { isOk() }
      content { contentType(MediaType.APPLICATION_JSON) }
      jsonPath("$.items[0].avg_price_ar") { value(2500) }
      jsonPath("$.items[0].rating_avg") { value(4.5) }
      jsonPath("$.next_cursor") { value("next-cur") }
    }
  }

  @Test
  fun `detail renders nested amenities with the contract field name`() {
    Mockito.`when`(service.detail("chez")).thenReturn(detail())

    mockMvc.get("/v1/establishments/chez").andExpect {
      status { isOk() }
      jsonPath("$.open_now") { value(true) }
      jsonPath("$.amenities.open_24h") { value(false) }
      jsonPath("$.payment_methods[0].code") { value("cash") }
    }
  }

  @Test
  fun `missing establishment is a problem+json 404`() {
    Mockito.`when`(service.detail("nope")).thenThrow(NotFoundException("not found"))

    mockMvc.get("/v1/establishments/nope").andExpect {
      status { isNotFound() }
      content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
      jsonPath("$.status") { value(404) }
      jsonPath("$.correlation_id") { exists() }
    }
  }

  @Test
  fun `unknown boolean filter is rejected as a 400`() {
    mockMvc.get("/v1/establishments") { param("filters", "teleport") }.andExpect {
      status { isBadRequest() }
      content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
      jsonPath("$.detail") { value("Unknown filter: teleport") }
    }
  }

  @Test
  fun `proposing an establishment requires authentication`() {
    mockMvc
      .post("/v1/establishments") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"name": "Chez Bao", "type": "gargotte", "position": {"lat": -18.91, "lng": 47.52}}"""
      }.andExpect {
        status { isUnauthorized() }
        content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
      }
  }

  @Test
  fun `an authenticated proposal is accepted for moderation`() {
    val authorId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001")
    val proposal =
      EstablishmentProposal(
        name = "Chez Bao",
        type = "gargotte",
        position = GeoPoint(-18.91, 47.52),
        avgPriceAr = 3000,
      )
    Mockito
      .`when`(contributionService.proposeEstablishment(authorId, proposal))
      .thenReturn(
        ContributionReceipt(
          id = UUID.fromString("cccccccc-0000-0000-0000-000000000001"),
          status = "pending",
          createdAt = Instant.parse("2026-07-06T09:00:00Z"),
        ),
      )

    mockMvc
      .post("/v1/establishments") {
        with(jwt().jwt { it.subject(authorId.toString()) })
        contentType = MediaType.APPLICATION_JSON
        content =
          """
          {"name": "Chez Bao", "type": "gargotte",
           "position": {"lat": -18.91, "lng": 47.52}, "avg_price_ar": 3000}
          """.trimIndent()
      }.andExpect {
        status { isAccepted() }
        jsonPath("$.id") { value("cccccccc-0000-0000-0000-000000000001") }
        jsonPath("$.status") { value("pending") }
        jsonPath("$.created_at") { exists() }
      }
  }

  @Test
  fun `a nameless proposal is a 400`() {
    mockMvc
      .post("/v1/establishments") {
        with(jwt().jwt { it.subject("bbbbbbbb-0000-0000-0000-000000000001") })
        contentType = MediaType.APPLICATION_JSON
        content = """{"type": "gargotte", "position": {"lat": -18.91, "lng": 47.52}}"""
      }.andExpect {
        status { isBadRequest() }
        content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
      }
  }

  @Test
  fun `an out-of-range pin is a 400`() {
    mockMvc
      .post("/v1/establishments") {
        with(jwt().jwt { it.subject("bbbbbbbb-0000-0000-0000-000000000001") })
        contentType = MediaType.APPLICATION_JSON
        content = """{"name": "Chez Bao", "type": "gargotte", "position": {"lat": -95.0, "lng": 47.52}}"""
      }.andExpect {
        status { isBadRequest() }
        content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
      }
  }

  @Test
  fun `an unsupported method is a problem+json 405, not a 500`() {
    mockMvc
      .post("/v1/establishments/some-slug") {
        with(jwt().jwt { it.subject("bbbbbbbb-0000-0000-0000-000000000001") })
        contentType = MediaType.APPLICATION_JSON
        content = "{}"
      }.andExpect {
        status { isMethodNotAllowed() }
        content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
        jsonPath("$.title") { value("Method not allowed") }
      }
  }

  private fun summary() =
    EstablishmentSummary(
      id = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001"),
      slug = "chez",
      name = "Chez Bao",
      type = "gargotte",
      position = GeoPoint(-18.9, 47.5),
      avgPriceAr = 2500,
      verified = true,
      status = "active",
      ratingAvg = BigDecimal("4.5"),
      ratingCount = 3,
    )

  private fun detail() =
    EstablishmentDetail(
      id = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001"),
      slug = "chez",
      name = "Chez Bao",
      type = "gargotte",
      position = GeoPoint(-18.9, 47.5),
      address = null,
      district = "Analakely",
      city = "Antananarivo",
      phone = null,
      whatsapp = null,
      facebookUrl = null,
      website = null,
      avgPriceAr = 2500,
      verified = true,
      status = "active",
      openNow = true,
      amenities =
        Amenities(
          delivery = true,
          parking = false,
          wifi = false,
          wheelchairAccess = false,
          airConditioning = false,
          terrace = false,
          familyFriendly = false,
          romantic = false,
          studentFriendly = false,
          scenicView = false,
          open24h = false,
        ),
      openingHours = listOf(OpeningInterval(0, "07:00", "21:00")),
      paymentMethods = listOf(ReferentialItem("cash", "Espèces", "Vola madinika")),
      cuisines = listOf(ReferentialItem("malagasy", "Malgache", "Sakafo gasy")),
      rating = RatingSummary(3, BigDecimal("4.5"), null, null, null, null, null, BigDecimal("4.2")),
      createdAt = Instant.parse("2026-07-06T09:00:00Z"),
      updatedAt = Instant.parse("2026-07-06T09:00:00Z"),
    )
}
