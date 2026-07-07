package mg.fikaliako.api.endpoint.rest.controller

import mg.fikaliako.api.config.SecurityConfig
import mg.fikaliako.api.endpoint.rest.model.Amenities
import mg.fikaliako.api.endpoint.rest.model.EstablishmentDetail
import mg.fikaliako.api.endpoint.rest.model.EstablishmentFilters
import mg.fikaliako.api.endpoint.rest.model.EstablishmentSummary
import mg.fikaliako.api.endpoint.rest.model.GeoPoint
import mg.fikaliako.api.endpoint.rest.model.OpeningInterval
import mg.fikaliako.api.endpoint.rest.model.Page
import mg.fikaliako.api.endpoint.rest.model.RatingSummary
import mg.fikaliako.api.endpoint.rest.model.ReferentialItem
import mg.fikaliako.api.model.exception.NotFoundException
import mg.fikaliako.api.service.EstablishmentService
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

@WebMvcTest(EstablishmentController::class)
@Import(SecurityConfig::class)
class EstablishmentControllerTest {
  @Autowired
  lateinit var mockMvc: MockMvc

  @MockitoBean
  lateinit var service: EstablishmentService

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
