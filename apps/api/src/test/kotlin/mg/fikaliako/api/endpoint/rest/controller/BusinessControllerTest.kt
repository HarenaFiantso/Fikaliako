package mg.fikaliako.api.endpoint.rest.controller

import mg.fikaliako.api.config.SecurityConfig
import mg.fikaliako.api.endpoint.rest.model.EstablishmentSummary
import mg.fikaliako.api.endpoint.rest.model.GeoPoint
import mg.fikaliako.api.endpoint.rest.model.Page
import mg.fikaliako.api.service.BusinessEstablishmentService
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.util.UUID

// RBAC around /v1/business/**: consumers are refused, BUSINESS and ADMIN pass
@WebMvcTest(BusinessController::class)
@Import(SecurityConfig::class)
class BusinessControllerTest {
  @Autowired
  lateinit var mockMvc: MockMvc

  @MockitoBean
  lateinit var service: BusinessEstablishmentService

  private val userId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002")

  private fun page() =
    Page(
      listOf(
        EstablishmentSummary(
          id = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001"),
          slug = "gargotte-chez-bao",
          name = "Gargotte Chez Bao",
          type = "gargotte",
          position = GeoPoint(-18.9092, 47.5210),
          avgPriceAr = 2500,
          verified = true,
          status = "active",
          ratingAvg = null,
          ratingCount = 0,
        ),
      ),
    )

  @Test
  fun `anonymous callers get a 401 problem`() {
    mockMvc.get("/v1/business/establishments").andExpect {
      status { isUnauthorized() }
      content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
    }
  }

  @Test
  fun `a consumer token is refused with a 403 problem`() {
    mockMvc
      .get("/v1/business/establishments") {
        with(
          jwt()
            .jwt { it.subject(userId.toString()).claim("role", "user") }
            .authorities(SimpleGrantedAuthority("ROLE_USER")),
        )
      }.andExpect {
        status { isForbidden() }
        content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
        jsonPath("$.correlation_id") { exists() }
      }
  }

  @Test
  fun `a business token lists its managed establishments`() {
    Mockito.`when`(service.listManaged(userId)).thenReturn(page())

    mockMvc
      .get("/v1/business/establishments") {
        with(
          jwt()
            .jwt { it.subject(userId.toString()).claim("role", "business") }
            .authorities(SimpleGrantedAuthority("ROLE_BUSINESS")),
        )
      }.andExpect {
        status { isOk() }
        jsonPath("$.items[0].slug") { value("gargotte-chez-bao") }
      }
  }

  @Test
  fun `an admin token passes the business route rule`() {
    Mockito.`when`(service.listManaged(userId)).thenReturn(page())

    mockMvc
      .get("/v1/business/establishments") {
        with(
          jwt()
            .jwt { it.subject(userId.toString()).claim("role", "admin") }
            .authorities(SimpleGrantedAuthority("ROLE_ADMIN")),
        )
      }.andExpect { status { isOk() } }
  }
}
