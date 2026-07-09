package mg.fikaliako.api.endpoint.rest.controller

import jakarta.validation.Valid
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
import mg.fikaliako.api.service.AuthService
import mg.fikaliako.api.util.userId
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/auth")
class AuthController(
  private val service: AuthService,
) {
  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  fun register(
    @Valid @RequestBody request: RegisterRequest,
  ): UserProfile = service.register(request)

  @PostMapping("/verify-phone")
  fun verifyPhone(
    @Valid @RequestBody request: VerifyPhoneRequest,
  ): AuthSession = service.verifyPhone(request)

  @PostMapping("/resend-otp")
  @ResponseStatus(HttpStatus.ACCEPTED)
  fun resendOtp(
    @Valid @RequestBody request: ResendOtpRequest,
  ) = service.resendOtp(request)

  @PostMapping("/login")
  fun login(
    @Valid @RequestBody request: LoginRequest,
  ): AuthSession = service.login(request)

  @PostMapping("/refresh")
  fun refresh(
    @Valid @RequestBody request: RefreshRequest,
  ): AuthTokens = service.refresh(request)

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun logout(
    @Valid @RequestBody request: LogoutRequest,
  ) = service.logout(request)

  @PostMapping("/forgot-password")
  @ResponseStatus(HttpStatus.ACCEPTED)
  fun forgotPassword(
    @Valid @RequestBody request: ForgotPasswordRequest,
  ) = service.forgotPassword(request)

  @PostMapping("/reset-password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun resetPassword(
    @Valid @RequestBody request: ResetPasswordRequest,
  ) = service.resetPassword(request)

  @PostMapping("/change-password")
  fun changePassword(
    @AuthenticationPrincipal jwt: Jwt,
    @Valid @RequestBody request: ChangePasswordRequest,
  ): AuthTokens = service.changePassword(jwt.userId(), request)
}
