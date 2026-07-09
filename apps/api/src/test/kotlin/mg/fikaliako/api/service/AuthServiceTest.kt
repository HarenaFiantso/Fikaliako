package mg.fikaliako.api.service
import mg.fikaliako.api.config.AuthProperties
import mg.fikaliako.api.endpoint.rest.model.ChangePasswordRequest
import mg.fikaliako.api.endpoint.rest.model.ForgotPasswordRequest
import mg.fikaliako.api.endpoint.rest.model.LoginRequest
import mg.fikaliako.api.endpoint.rest.model.LogoutRequest
import mg.fikaliako.api.endpoint.rest.model.RefreshRequest
import mg.fikaliako.api.endpoint.rest.model.RegisterRequest
import mg.fikaliako.api.endpoint.rest.model.ResetPasswordRequest
import mg.fikaliako.api.endpoint.rest.model.VerifyPhoneRequest
import mg.fikaliako.api.model.AccountStatus
import mg.fikaliako.api.model.OtpPurpose
import mg.fikaliako.api.model.RefreshToken
import mg.fikaliako.api.model.UserAccount
import mg.fikaliako.api.model.UserRole
import mg.fikaliako.api.model.exception.ConflictException
import mg.fikaliako.api.model.exception.ForbiddenException
import mg.fikaliako.api.model.exception.UnauthorizedException
import mg.fikaliako.api.repository.RefreshTokenRepository
import mg.fikaliako.api.repository.UserAccountRepository
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuthServiceTest {
  private val now = Instant.parse("2026-07-09T12:00:00Z")
  private val props = AuthProperties(jwtSecret = "test-only-fikaliako-jwt-secret-0123456789abcdef")
  private val users = Mockito.mock(UserAccountRepository::class.java)
  private val refreshTokens = Mockito.mock(RefreshTokenRepository::class.java)
  private val tokenService = Mockito.mock(TokenService::class.java)
  private val otpService = Mockito.mock(OtpService::class.java)
  private val passwordEncoder = Mockito.mock(PasswordEncoder::class.java)
  private val service =
    AuthService(users, refreshTokens, tokenService, otpService, passwordEncoder, props, Clock.fixed(now, ZoneOffset.UTC))

  private val phone = "+261340000001"
  private val userId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001")

  private fun activeUser(
    verified: Boolean = true,
    role: UserRole = UserRole.user,
    status: AccountStatus = AccountStatus.active,
  ) = UserAccount(
    id = userId,
    displayName = "Naina",
    phone = phone,
    phoneVerified = verified,
    passwordHash = "stored-hash",
    role = role,
    status = status,
    createdAt = now.minusSeconds(86_400),
    updatedAt = now.minusSeconds(86_400),
  )

  private fun stubTokenIssuance(user: UserAccount) {
    Mockito.`when`(tokenService.issueAccessToken(user)).thenReturn("access-jwt")
    Mockito.`when`(tokenService.newRefreshTokenValue()).thenReturn("refresh-value")
    Mockito.`when`(tokenService.hash("refresh-value")).thenReturn("refresh-hash")
  }

  @Test
  fun `register creates a consumer account and sends the verification code`() {
    Mockito.`when`(users.existsByPhone(phone)).thenReturn(false)
    Mockito.`when`(passwordEncoder.encode("password123")).thenReturn("argon2-hash")

    val profile = service.register(RegisterRequest(phone, "password123", "Naina"))

    val captor = ArgumentCaptor.forClass(UserAccount::class.java)
    Mockito.verify(users).save(captor.capture())
    assertEquals("argon2-hash", captor.value.passwordHash)
    assertEquals(UserRole.user, captor.value.role)
    assertEquals(now, captor.value.createdAt)
    Mockito.verify(otpService).issue(phone, OtpPurpose.verify_phone)
    assertEquals("user", profile.role)
    assertEquals(false, profile.phoneVerified)
  }

  @Test
  fun `register with account_type business grants the BUSINESS role`() {
    Mockito.`when`(users.existsByPhone(phone)).thenReturn(false)
    Mockito.`when`(passwordEncoder.encode(Mockito.anyString())).thenReturn("argon2-hash")

    val profile = service.register(RegisterRequest(phone, "password123", "Chez Bao", accountType = "business"))

    assertEquals("business", profile.role)
  }

  @Test
  fun `register refuses a phone number that already has an account`() {
    Mockito.`when`(users.existsByPhone(phone)).thenReturn(true)
    assertFailsWith<ConflictException> { service.register(RegisterRequest(phone, "password123", "Naina")) }
  }

  @Test
  fun `login rejects an unknown number with the same error as a bad password`() {
    Mockito.`when`(users.findByPhone(phone)).thenReturn(null)
    val ex = assertFailsWith<UnauthorizedException> { service.login(LoginRequest(phone, "password123")) }
    assertEquals(AuthService.BAD_CREDENTIALS, ex.message)
    Mockito.verify(passwordEncoder).matches(Mockito.eq("password123"), Mockito.anyString())
  }

  @Test
  fun `login rejects a wrong password`() {
    Mockito.`when`(users.findByPhone(phone)).thenReturn(activeUser())
    Mockito.`when`(passwordEncoder.matches("nope", "stored-hash")).thenReturn(false)
    assertFailsWith<UnauthorizedException> { service.login(LoginRequest(phone, "nope")) }
  }

  @Test
  fun `login refuses an unverified phone`() {
    Mockito.`when`(users.findByPhone(phone)).thenReturn(activeUser(verified = false))
    Mockito.`when`(passwordEncoder.matches("password123", "stored-hash")).thenReturn(true)
    assertFailsWith<ForbiddenException> { service.login(LoginRequest(phone, "password123")) }
  }

  @Test
  fun `login refuses a suspended account`() {
    Mockito.`when`(users.findByPhone(phone)).thenReturn(activeUser(status = AccountStatus.suspended))
    Mockito.`when`(passwordEncoder.matches("password123", "stored-hash")).thenReturn(true)
    assertFailsWith<ForbiddenException> { service.login(LoginRequest(phone, "password123")) }
  }

  @Test
  fun `login opens a session with rotated-family refresh token`() {
    val user = activeUser()
    Mockito.`when`(users.findByPhone(phone)).thenReturn(user)
    Mockito.`when`(passwordEncoder.matches("password123", "stored-hash")).thenReturn(true)
    stubTokenIssuance(user)

    val session = service.login(LoginRequest(phone, "password123"))

    val captor = ArgumentCaptor.forClass(RefreshToken::class.java)
    Mockito.verify(refreshTokens).save(captor.capture())
    val stored = captor.value
    assertEquals("refresh-hash", stored.tokenHash)
    assertNotNull(stored.familyId)
    assertEquals(now.plus(props.refreshTokenTtl), stored.expiresAt)
    assertEquals("access-jwt", session.tokens.accessToken)
    assertEquals("refresh-value", session.tokens.refreshToken)
    assertEquals(props.accessTokenTtl.seconds, session.tokens.expiresIn)
    assertEquals("user", session.user.role)
  }

  @Test
  fun `verify-phone marks the account verified and logs the user in`() {
    val user = activeUser(verified = false)
    Mockito.`when`(users.findByPhone(phone)).thenReturn(user)
    stubTokenIssuance(user)

    val session = service.verifyPhone(VerifyPhoneRequest(phone, "123456"))

    Mockito.verify(otpService).verify(phone, OtpPurpose.verify_phone, "123456")
    assertTrue(user.phoneVerified)
    assertEquals(true, session.user.phoneVerified)
  }

  @Test
  fun `refresh rotates the token within its family`() {
    val user = activeUser()
    val familyId = UUID.randomUUID()
    val current =
      RefreshToken(
        id = UUID.randomUUID(),
        user = user,
        tokenHash = "old-hash",
        familyId = familyId,
        issuedAt = now.minusSeconds(3600),
        expiresAt = now.plusSeconds(3600),
      )
    Mockito.`when`(tokenService.hash("old-value")).thenReturn("old-hash")
    Mockito.`when`(refreshTokens.findByTokenHash("old-hash")).thenReturn(current)
    stubTokenIssuance(user)

    val tokens = service.refresh(RefreshRequest("old-value"))

    assertEquals(now, current.revokedAt)
    val captor = ArgumentCaptor.forClass(RefreshToken::class.java)
    Mockito.verify(refreshTokens, Mockito.times(2)).save(captor.capture())
    val successor = captor.allValues.last { it.tokenHash == "refresh-hash" }
    assertEquals(familyId, successor.familyId)
    assertEquals("refresh-value", tokens.refreshToken)
  }

  @Test
  fun `reusing a rotated refresh token revokes the whole family`() {
    val familyId = UUID.randomUUID()
    val revoked =
      RefreshToken(
        id = UUID.randomUUID(),
        user = activeUser(),
        tokenHash = "old-hash",
        familyId = familyId,
        issuedAt = now.minusSeconds(7200),
        expiresAt = now.plusSeconds(3600),
        revokedAt = now.minusSeconds(3600),
      )
    Mockito.`when`(tokenService.hash("stolen")).thenReturn("old-hash")
    Mockito.`when`(refreshTokens.findByTokenHash("old-hash")).thenReturn(revoked)

    assertFailsWith<UnauthorizedException> { service.refresh(RefreshRequest("stolen")) }
    Mockito.verify(refreshTokens).revokeFamily(familyId, now)
  }

  @Test
  fun `an expired refresh token cannot rotate`() {
    val current =
      RefreshToken(
        id = UUID.randomUUID(),
        user = activeUser(),
        tokenHash = "old-hash",
        familyId = UUID.randomUUID(),
        issuedAt = now.minusSeconds(7200),
        expiresAt = now,
      )
    Mockito.`when`(tokenService.hash("old-value")).thenReturn("old-hash")
    Mockito.`when`(refreshTokens.findByTokenHash("old-hash")).thenReturn(current)

    assertFailsWith<UnauthorizedException> { service.refresh(RefreshRequest("old-value")) }
  }

  @Test
  fun `logout revokes the family and is idempotent for unknown tokens`() {
    val familyId = UUID.randomUUID()
    val current =
      RefreshToken(
        id = UUID.randomUUID(),
        user = activeUser(),
        tokenHash = "known-hash",
        familyId = familyId,
        issuedAt = now,
        expiresAt = now.plusSeconds(3600),
      )
    Mockito.`when`(tokenService.hash("known")).thenReturn("known-hash")
    Mockito.`when`(tokenService.hash("unknown")).thenReturn("unknown-hash")
    Mockito.`when`(refreshTokens.findByTokenHash("known-hash")).thenReturn(current)

    service.logout(LogoutRequest("known"))
    service.logout(LogoutRequest("unknown"))

    Mockito.verify(refreshTokens, Mockito.times(1)).revokeFamily(familyId, now)
  }

  @Test
  fun `forgot-password stays silent for unknown numbers`() {
    Mockito.`when`(users.findByPhone(phone)).thenReturn(null)
    service.forgotPassword(ForgotPasswordRequest(phone))
    Mockito.verifyNoInteractions(otpService)
  }

  @Test
  fun `reset-password rehashes and signs every device out`() {
    val user = activeUser()
    Mockito.`when`(users.findByPhone(phone)).thenReturn(user)
    Mockito.`when`(passwordEncoder.encode("new-password-1")).thenReturn("new-hash")

    service.resetPassword(ResetPasswordRequest(phone, "123456", "new-password-1"))

    Mockito.verify(otpService).verify(phone, OtpPurpose.reset_password, "123456")
    assertEquals("new-hash", user.passwordHash)
    Mockito.verify(refreshTokens).revokeAllForUser(userId, now)
  }

  @Test
  fun `change-password verifies the current password before rotating everything`() {
    val user = activeUser()
    Mockito.`when`(users.findById(userId)).thenReturn(Optional.of(user))
    Mockito.`when`(passwordEncoder.matches("wrong", "stored-hash")).thenReturn(false)
    assertFailsWith<UnauthorizedException> {
      service.changePassword(userId, ChangePasswordRequest("wrong", "new-password-1"))
    }

    Mockito.`when`(passwordEncoder.matches("current-1", "stored-hash")).thenReturn(true)
    Mockito.`when`(passwordEncoder.encode("new-password-1")).thenReturn("new-hash")
    stubTokenIssuance(user)

    val tokens = service.changePassword(userId, ChangePasswordRequest("current-1", "new-password-1"))

    assertEquals("new-hash", user.passwordHash)
    Mockito.verify(refreshTokens).revokeAllForUser(userId, now)
    assertEquals("refresh-value", tokens.refreshToken)
  }
}
