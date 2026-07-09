package mg.fikaliako.api.service
import mg.fikaliako.api.config.AuthProperties
import mg.fikaliako.api.endpoint.rest.model.AuthSession
import mg.fikaliako.api.endpoint.rest.model.AuthTokens
import mg.fikaliako.api.endpoint.rest.model.ChangePasswordRequest
import mg.fikaliako.api.endpoint.rest.model.ForgotPasswordRequest
import mg.fikaliako.api.endpoint.rest.model.LoginRequest
import mg.fikaliako.api.endpoint.rest.model.LogoutRequest
import mg.fikaliako.api.endpoint.rest.model.RefreshRequest
import mg.fikaliako.api.endpoint.rest.model.RegisterRequest
import mg.fikaliako.api.endpoint.rest.model.ResendOtpRequest
import mg.fikaliako.api.endpoint.rest.model.ResetPasswordRequest
import mg.fikaliako.api.endpoint.rest.model.UserProfile
import mg.fikaliako.api.endpoint.rest.model.VerifyPhoneRequest
import mg.fikaliako.api.model.AccountStatus
import mg.fikaliako.api.model.OtpPurpose
import mg.fikaliako.api.model.RefreshToken
import mg.fikaliako.api.model.UserAccount
import mg.fikaliako.api.model.UserRole
import mg.fikaliako.api.model.exception.BadRequestException
import mg.fikaliako.api.model.exception.ConflictException
import mg.fikaliako.api.model.exception.ForbiddenException
import mg.fikaliako.api.model.exception.UnauthorizedException
import mg.fikaliako.api.repository.RefreshTokenRepository
import mg.fikaliako.api.repository.UserAccountRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.util.UUID

// Authentication flows (book ch. 4.7, 7.3). Identity is the phone number;
// signup and password reset are confirmed by OTP SMS. Sessions are one
// refresh-token family each: rotation on every refresh, reuse detection kills
// the family, logout/reset/change-password revoke server-side.
@Service
class AuthService(
  private val users: UserAccountRepository,
  private val refreshTokens: RefreshTokenRepository,
  private val tokenService: TokenService,
  private val otpService: OtpService,
  private val passwordEncoder: PasswordEncoder,
  private val props: AuthProperties,
  private val clock: Clock,
) {
  @Transactional
  fun register(request: RegisterRequest): UserProfile {
    if (users.existsByPhone(request.phone)) {
      throw ConflictException("An account already exists for this phone number.")
    }
    val now = clock.instant()
    val user =
      UserAccount(
        id = UUID.randomUUID(),
        displayName = request.displayName.trim(),
        phone = request.phone,
        passwordHash = requireNotNull(passwordEncoder.encode(request.password)),
        role = if (request.accountType == "business") UserRole.BUSINESS else UserRole.USER,
        locale = request.locale,
        createdAt = now,
        updatedAt = now,
      )
    users.save(user)
    otpService.issue(user.phone, OtpPurpose.VERIFY_PHONE)
    return user.toProfile()
  }

  @Transactional
  fun verifyPhone(request: VerifyPhoneRequest): AuthSession {
    // deliberately the same error as a bad code: don't reveal which numbers exist
    val user = users.findByPhone(request.phone) ?: throw BadRequestException(OtpService.INVALID_CODE)
    otpService.verify(request.phone, OtpPurpose.VERIFY_PHONE, request.code)
    if (!user.phoneVerified) {
      user.phoneVerified = true
      user.updatedAt = clock.instant()
      users.save(user)
    }
    requireActive(user)
    return AuthSession(user = user.toProfile(), tokens = openSession(user))
  }

  // Always answers 202: whether the number has an unverified account is not
  // revealed. The OTP issuance cap still applies.
  @Transactional
  fun resendOtp(request: ResendOtpRequest) {
    val user = users.findByPhone(request.phone) ?: return
    if (!user.phoneVerified) otpService.issue(user.phone, OtpPurpose.VERIFY_PHONE)
  }

  @Transactional
  fun login(request: LoginRequest): AuthSession {
    val user = users.findByPhone(request.phone)
    if (user == null) {
      // constant-time-ish: hash anyway so absent accounts cost the same
      passwordEncoder.matches(request.password, DUMMY_HASH)
      throw UnauthorizedException(BAD_CREDENTIALS)
    }
    if (!passwordEncoder.matches(request.password, user.passwordHash)) {
      throw UnauthorizedException(BAD_CREDENTIALS)
    }
    requireActive(user)
    if (!user.phoneVerified) {
      throw ForbiddenException("Phone number not verified. Verify it with the code sent by SMS (/v1/auth/verify-phone).")
    }
    return AuthSession(user = user.toProfile(), tokens = openSession(user))
  }

  @Transactional
  fun refresh(request: RefreshRequest): AuthTokens {
    val now = clock.instant()
    val current =
      refreshTokens.findByTokenHash(tokenService.hash(request.refreshToken))
        ?: throw UnauthorizedException("Invalid refresh token.")
    if (current.revokedAt != null) {
      // rotation reuse — the token leaked or the client replayed; kill the session
      refreshTokens.revokeFamily(requireNotNull(current.familyId), now)
      throw UnauthorizedException("Refresh token reuse detected; the session has been revoked.")
    }
    if (requireNotNull(current.expiresAt) <= now) {
      throw UnauthorizedException("Refresh token expired.")
    }
    val user = requireNotNull(current.user)
    requireActive(user)
    current.revokedAt = now
    refreshTokens.save(current)
    return issueTokens(user, requireNotNull(current.familyId))
  }

  // Idempotent: an unknown token is already as logged-out as it gets
  @Transactional
  fun logout(request: LogoutRequest) {
    val current = refreshTokens.findByTokenHash(tokenService.hash(request.refreshToken)) ?: return
    refreshTokens.revokeFamily(requireNotNull(current.familyId), clock.instant())
  }

  // Always answers 202 (no account enumeration); the code only goes to the
  // registered number
  @Transactional
  fun forgotPassword(request: ForgotPasswordRequest) {
    users.findByPhone(request.phone) ?: return
    otpService.issue(request.phone, OtpPurpose.RESET_PASSWORD)
  }

  @Transactional
  fun resetPassword(request: ResetPasswordRequest) {
    val user = users.findByPhone(request.phone) ?: throw BadRequestException(OtpService.INVALID_CODE)
    otpService.verify(request.phone, OtpPurpose.RESET_PASSWORD, request.code)
    val now = clock.instant()
    user.passwordHash = requireNotNull(passwordEncoder.encode(request.newPassword))
    user.updatedAt = now
    users.save(user)
    // a reset proves control of the number, not of every device: sign everyone out
    refreshTokens.revokeAllForUser(requireNotNull(user.id), now)
  }

  @Transactional
  fun changePassword(
    userId: UUID,
    request: ChangePasswordRequest,
  ): AuthTokens {
    val user = users.findById(userId).orElseThrow { UnauthorizedException("Account no longer exists.") }
    if (!passwordEncoder.matches(request.currentPassword, user.passwordHash)) {
      throw UnauthorizedException("Current password is incorrect.")
    }
    val now = clock.instant()
    user.passwordHash = requireNotNull(passwordEncoder.encode(request.newPassword))
    user.updatedAt = now
    users.save(user)
    refreshTokens.revokeAllForUser(userId, now)
    // fresh session for the caller so the client is not logged out mid-flow
    return openSession(user)
  }

  private fun openSession(user: UserAccount): AuthTokens = issueTokens(user, familyId = UUID.randomUUID())

  private fun issueTokens(
    user: UserAccount,
    familyId: UUID,
  ): AuthTokens {
    val now = clock.instant()
    val refreshValue = tokenService.newRefreshTokenValue()
    refreshTokens.save(
      RefreshToken(
        id = UUID.randomUUID(),
        user = user,
        tokenHash = tokenService.hash(refreshValue),
        familyId = familyId,
        issuedAt = now,
        expiresAt = now.plus(props.refreshTokenTtl),
      ),
    )
    return AuthTokens(
      accessToken = tokenService.issueAccessToken(user),
      expiresIn = props.accessTokenTtl.seconds,
      refreshToken = refreshValue,
      refreshExpiresIn = props.refreshTokenTtl.seconds,
    )
  }

  private fun requireActive(user: UserAccount) {
    if (user.status != AccountStatus.ACTIVE) {
      throw ForbiddenException("This account is ${user.status.name.lowercase()}.")
    }
  }

  companion object {
    const val BAD_CREDENTIALS = "Invalid phone number or password."

    // a real Argon2id hash of a random throwaway password, for timing equalisation
    private const val DUMMY_HASH =
      "\$argon2id\$v=19\$m=19456,t=2,p=1\$QUFBQUFBQUFBQUFBQUFBQQ\$K5d9UGB6RJqyq3tdKDGvCYZDXDTmM+SX3jEqUPXhqkI"
  }
}
