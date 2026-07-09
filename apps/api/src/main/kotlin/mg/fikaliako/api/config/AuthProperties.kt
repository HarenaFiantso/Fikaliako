package mg.fikaliako.api.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

// Knobs of the accounts/security module (book ch. 7.3); see application.yml
@ConfigurationProperties("fikaliako.auth")
data class AuthProperties(
  val jwtSecret: String,
  val issuer: String = "fikaliako-api",
  val accessTokenTtl: Duration = Duration.ofMinutes(15),
  val refreshTokenTtl: Duration = Duration.ofDays(30),
  val otpTtl: Duration = Duration.ofMinutes(10),
  val otpMaxPerHour: Int = 5,
  val otpMaxAttempts: Int = 5,
) {
  init {
    require(jwtSecret.toByteArray().size >= 32) { "fikaliako.auth.jwt-secret must be at least 32 bytes for HS256." }
  }
}
