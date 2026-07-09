package mg.fikaliako.api.endpoint.rest.model

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

const val E164_PATTERN = "^\\+[1-9][0-9]{7,14}$"
const val OTP_PATTERN = "^[0-9]{6}$"

data class RegisterRequest(
  @field:NotBlank
  @field:Pattern(regexp = E164_PATTERN, message = "must be an E.164 phone number, e.g. +261340000001")
  val phone: String = "",

  @field:NotBlank
  @field:Size(min = 8, max = 128)
  val password: String = "",

  @field:NotBlank
  @field:Size(min = 2, max = 60)
  val displayName: String = "",

  // consumer → USER role; business → BUSINESS (premium establishment account)
  @field:Pattern(regexp = "^(consumer|business)$", message = "must be 'consumer' or 'business'")
  val accountType: String = "consumer",

  @field:Pattern(regexp = "^(fr|mg)$", message = "must be 'fr' or 'mg'")
  val locale: String = "fr",
)

data class LoginRequest(
  @field:NotBlank
  val phone: String = "",

  @field:NotBlank
  val password: String = "",
)

data class VerifyPhoneRequest(
  @field:NotBlank
  @field:Pattern(regexp = E164_PATTERN, message = "must be an E.164 phone number")
  val phone: String = "",

  @field:Pattern(regexp = OTP_PATTERN, message = "must be the 6-digit code")
  val code: String = "",
)

data class ResendOtpRequest(
  @field:NotBlank
  @field:Pattern(regexp = E164_PATTERN, message = "must be an E.164 phone number")
  val phone: String = "",
)

data class ForgotPasswordRequest(
  @field:NotBlank
  @field:Pattern(regexp = E164_PATTERN, message = "must be an E.164 phone number")
  val phone: String = "",
)

data class ResetPasswordRequest(
  @field:NotBlank
  @field:Pattern(regexp = E164_PATTERN, message = "must be an E.164 phone number")
  val phone: String = "",

  @field:Pattern(regexp = OTP_PATTERN, message = "must be the 6-digit code")
  val code: String = "",

  @field:NotBlank
  @field:Size(min = 8, max = 128)
  val newPassword: String = "",
)

data class ChangePasswordRequest(
  @field:NotBlank
  val currentPassword: String = "",

  @field:NotBlank
  @field:Size(min = 8, max = 128)
  val newPassword: String = "",
)

data class RefreshRequest(
  @field:NotBlank
  val refreshToken: String = "",
)

data class LogoutRequest(
  @field:NotBlank
  val refreshToken: String = "",
)

data class UserProfile(
  val id: UUID,
  val phone: String,
  val displayName: String,
  val role: String,
  val phoneVerified: Boolean,
  val locale: String,
  val createdAt: Instant,
)

data class AuthTokens(
  val tokenType: String = "Bearer",
  val accessToken: String,
  val expiresIn: Long,
  val refreshToken: String,
  val refreshExpiresIn: Long,
)

data class AuthSession(
  val user: UserProfile,
  val tokens: AuthTokens,
)
