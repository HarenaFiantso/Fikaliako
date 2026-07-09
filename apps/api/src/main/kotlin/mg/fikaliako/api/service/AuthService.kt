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
        role = if (request.accountType == "business") UserRole.business else UserRole.user,
        locale = request.locale,
        createdAt = now,
        updatedAt = now,
      )
    users.save(user)
    otpService.issue(user.phone, OtpPurpose.verify_phone)
    return user.toProfile()
  }

  @Transactional(noRollbackFor = [BadRequestException::class])
  fun verifyPhone(request: VerifyPhoneRequest): AuthSession {
    val user = users.findByPhone(request.phone) ?: throw BadRequestException(OtpService.INVALID_CODE)
    otpService.verify(request.phone, OtpPurpose.verify_phone, request.code)
    if (!user.phoneVerified) {
      user.phoneVerified = true
      user.updatedAt = clock.instant()
      users.save(user)
    }
    requireActive(user)
    return AuthSession(user = user.toProfile(), tokens = openSession(user))
  }

  @Transactional
  fun resendOtp(request: ResendOtpRequest) {
    val user = users.findByPhone(request.phone) ?: return
    if (!user.phoneVerified) otpService.issue(user.phone, OtpPurpose.verify_phone)
  }

  @Transactional
  fun login(request: LoginRequest): AuthSession {
    val user = users.findByPhone(request.phone)
    if (user == null) {
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

  @Transactional(noRollbackFor = [UnauthorizedException::class])
  fun refresh(request: RefreshRequest): AuthTokens {
    val now = clock.instant()
    val current =
      refreshTokens.findByTokenHash(tokenService.hash(request.refreshToken))
        ?: throw UnauthorizedException("Invalid refresh token.")
    if (current.revokedAt != null) {
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

  @Transactional
  fun logout(request: LogoutRequest) {
    val current = refreshTokens.findByTokenHash(tokenService.hash(request.refreshToken)) ?: return
    refreshTokens.revokeFamily(requireNotNull(current.familyId), clock.instant())
  }

  @Transactional
  fun forgotPassword(request: ForgotPasswordRequest) {
    users.findByPhone(request.phone) ?: return
    otpService.issue(request.phone, OtpPurpose.reset_password)
  }

  @Transactional(noRollbackFor = [BadRequestException::class])
  fun resetPassword(request: ResetPasswordRequest) {
    val user = users.findByPhone(request.phone) ?: throw BadRequestException(OtpService.INVALID_CODE)
    otpService.verify(request.phone, OtpPurpose.reset_password, request.code)
    val now = clock.instant()
    user.passwordHash = requireNotNull(passwordEncoder.encode(request.newPassword))
    user.updatedAt = now
    users.save(user)
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
    if (user.status != AccountStatus.active) {
      throw ForbiddenException("This account is ${user.status.name.lowercase()}.")
    }
  }

  companion object {
    const val BAD_CREDENTIALS = "Invalid phone number or password."

    private const val DUMMY_HASH =
      "\$argon2id\$v=19\$m=19456,t=2,p=1\$QUFBQUFBQUFBQUFBQUFBQQ\$K5d9UGB6RJqyq3tdKDGvCYZDXDTmM+SX3jEqUPXhqkI"
  }
}
