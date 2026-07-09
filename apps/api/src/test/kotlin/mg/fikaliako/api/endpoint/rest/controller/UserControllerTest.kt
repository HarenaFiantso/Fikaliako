package mg.fikaliako.api.endpoint.rest.controller

import mg.fikaliako.api.config.SecurityConfig
import mg.fikaliako.api.endpoint.rest.model.UserProfile
import mg.fikaliako.api.service.UserService
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.put
import java.time.Instant
import java.util.UUID

@WebMvcTest(UserController::class)
@Import(SecurityConfig::class)
class UserControllerTest {
  @Autowired
  lateinit var mockMvc: MockMvc

  @MockitoBean
  lateinit var service: UserService

  private val userId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001")
  private val establishmentId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001")

  private val profile =
    UserProfile(
      id = userId,
      phone = "+261340000001",
      displayName = "Naina",
      role = "user",
      phoneVerified = true,
      locale = "fr",
      createdAt = Instant.parse("2026-07-09T12:00:00Z"),
    )

  @Test
  fun `the profile endpoint requires authentication`() {
    mockMvc.get("/v1/users/me").andExpect {
      status { isUnauthorized() }
      content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
      jsonPath("$.correlation_id") { exists() }
    }
  }

  @Test
  fun `returns the authenticated profile`() {
    Mockito.`when`(service.me(userId)).thenReturn(profile)

    mockMvc
      .get("/v1/users/me") { with(jwt().jwt { it.subject(userId.toString()) }) }
      .andExpect {
        status { isOk() }
        jsonPath("$.display_name") { value("Naina") }
        jsonPath("$.phone_verified") { value(true) }
      }
  }

  @Test
  fun `profile updates are validated`() {
    mockMvc
      .patch("/v1/users/me") {
        with(jwt().jwt { it.subject(userId.toString()) })
        contentType = MediaType.APPLICATION_JSON
        content = """{"locale": "en"}"""
      }.andExpect {
        status { isBadRequest() }
        content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
      }
  }

  @Test
  fun `favorites toggle with 204s`() {
    mockMvc
      .put("/v1/users/me/favorites/$establishmentId") {
        with(jwt().jwt { it.subject(userId.toString()) })
      }.andExpect { status { isNoContent() } }
    Mockito.verify(service).addFavorite(userId, establishmentId)

    mockMvc
      .delete("/v1/users/me/favorites/$establishmentId") {
        with(jwt().jwt { it.subject(userId.toString()) })
      }.andExpect { status { isNoContent() } }
    Mockito.verify(service).removeFavorite(userId, establishmentId)
  }
}
