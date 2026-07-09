package mg.fikaliako.api.endpoint.rest.controller

import mg.fikaliako.api.config.SecurityConfig
import mg.fikaliako.api.endpoint.rest.model.AuthSession
import mg.fikaliako.api.endpoint.rest.model.AuthTokens
import mg.fikaliako.api.endpoint.rest.model.ChangePasswordRequest
import mg.fikaliako.api.endpoint.rest.model.LoginRequest
import mg.fikaliako.api.endpoint.rest.model.RegisterRequest
import mg.fikaliako.api.endpoint.rest.model.ResendOtpRequest
import mg.fikaliako.api.endpoint.rest.model.UserProfile
import mg.fikaliako.api.model.exception.TooManyRequestsException
import mg.fikaliako.api.model.exception.UnauthorizedException
import mg.fikaliako.api.service.AuthService
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.Instant
import java.util.UUID

@WebMvcTest(AuthController::class)
@Import(SecurityConfig::class)
class AuthControllerTest {
  @Autowired
  lateinit var mockMvc: MockMvc

  @MockitoBean
  lateinit var service: AuthService

  private val userId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001")

  private val profile =
    UserProfile(
      id = userId,
      phone = "+261340000001",
      displayName = "Naina",
      role = "user",
      phoneVerified = false,
      locale = "fr",
      createdAt = Instant.parse("2026-07-09T12:00:00Z"),
    )

  private val tokens =
    AuthTokens(
      accessToken = "access-jwt",
      expiresIn = 900,
      refreshToken = "refresh-value",
      refreshExpiresIn = 2_592_000,
    )

  @Test
  fun `register returns 201 with the snake_case profile`() {
    Mockito
      .`when`(service.register(RegisterRequest("+261340000001", "password123", "Naina")))
      .thenReturn(profile)

    mockMvc
      .post("/v1/auth/register") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"phone": "+261340000001", "password": "password123", "display_name": "Naina"}"""
      }.andExpect {
        status { isCreated() }
        jsonPath("$.display_name") { value("Naina") }
        jsonPath("$.phone_verified") { value(false) }
        jsonPath("$.role") { value("user") }
      }
  }

  @Test
  fun `register validates the phone format`() {
    mockMvc
      .post("/v1/auth/register") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"phone": "034 00 000 01", "password": "password123", "display_name": "Naina"}"""
      }.andExpect {
        status { isBadRequest() }
        content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
        jsonPath("$.correlation_id") { exists() }
      }
  }

  @Test
  fun `register rejects a missing body`() {
    mockMvc
      .post("/v1/auth/register") { contentType = MediaType.APPLICATION_JSON }
      .andExpect {
        status { isBadRequest() }
        content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
      }
  }

  @Test
  fun `login returns the session`() {
    Mockito
      .`when`(service.login(LoginRequest("+261340000001", "password123")))
      .thenReturn(AuthSession(profile.copy(phoneVerified = true), tokens))

    mockMvc
      .post("/v1/auth/login") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"phone": "+261340000001", "password": "password123"}"""
      }.andExpect {
        status { isOk() }
        jsonPath("$.tokens.access_token") { value("access-jwt") }
        jsonPath("$.tokens.token_type") { value("Bearer") }
        jsonPath("$.tokens.expires_in") { value(900) }
        jsonPath("$.user.phone_verified") { value(true) }
      }
  }

  @Test
  fun `bad credentials surface as a 401 problem`() {
    Mockito
      .`when`(service.login(LoginRequest("+261340000001", "nope-nope")))
      .thenThrow(UnauthorizedException("Invalid phone number or password."))

    mockMvc
      .post("/v1/auth/login") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"phone": "+261340000001", "password": "nope-nope"}"""
      }.andExpect {
        status { isUnauthorized() }
        content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
        jsonPath("$.detail") { value("Invalid phone number or password.") }
      }
  }

  @Test
  fun `OTP flooding surfaces as a 429 problem`() {
    Mockito
      .doThrow(TooManyRequestsException("Too many codes requested for this number. Try again later."))
      .`when`(service)
      .resendOtp(ResendOtpRequest("+261340000001"))

    mockMvc
      .post("/v1/auth/resend-otp") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"phone": "+261340000001"}"""
      }.andExpect {
        status { isTooManyRequests() }
        content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
      }
  }

  @Test
  fun `logout answers 204`() {
    mockMvc
      .post("/v1/auth/logout") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"refresh_token": "refresh-value"}"""
      }.andExpect { status { isNoContent() } }
  }

  @Test
  fun `forgot-password always answers 202`() {
    mockMvc
      .post("/v1/auth/forgot-password") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"phone": "+261340000001"}"""
      }.andExpect { status { isAccepted() } }
  }

  @Test
  fun `change-password requires authentication`() {
    mockMvc
      .post("/v1/auth/change-password") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"current_password": "current-1", "new_password": "new-password-1"}"""
      }.andExpect {
        status { isUnauthorized() }
        content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
      }
  }

  @Test
  fun `change-password rotates the session for the authenticated user`() {
    Mockito
      .`when`(service.changePassword(userId, ChangePasswordRequest("current-1", "new-password-1")))
      .thenReturn(tokens)

    mockMvc
      .post("/v1/auth/change-password") {
        with(jwt().jwt { it.subject(userId.toString()) })
        contentType = MediaType.APPLICATION_JSON
        content = """{"current_password": "current-1", "new_password": "new-password-1"}"""
      }.andExpect {
        status { isOk() }
        jsonPath("$.refresh_token") { value("refresh-value") }
      }
  }
}
