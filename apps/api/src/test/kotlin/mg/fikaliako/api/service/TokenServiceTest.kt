package mg.fikaliako.api.service
import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.proc.SecurityContext
import mg.fikaliako.api.config.AuthProperties
import mg.fikaliako.api.model.UserAccount
import mg.fikaliako.api.model.UserRole
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import java.time.Clock
import java.time.Duration
import java.util.UUID
import javax.crypto.spec.SecretKeySpec
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TokenServiceTest {
  private val props = AuthProperties(jwtSecret = "test-only-fikaliako-jwt-secret-0123456789abcdef")
  private val key = SecretKeySpec(props.jwtSecret.toByteArray(), "HmacSHA256")
  private val service = TokenService(NimbusJwtEncoder(ImmutableSecret<SecurityContext>(key)), props, Clock.systemUTC())

  private val user =
    UserAccount(
      id = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001"),
      displayName = "Chez Bao",
      phone = "+261340000001",
      role = UserRole.BUSINESS,
    )

  @Test
  fun `access token carries subject, role and a 15-minute lifetime`() {
    val decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build()
    val jwt = decoder.decode(service.issueAccessToken(user))
    assertEquals(user.id.toString(), jwt.subject)
    assertEquals("business", jwt.getClaimAsString("role"))
    assertEquals(props.issuer, jwt.getClaimAsString("iss"))
    assertEquals(Duration.ofMinutes(15), Duration.between(jwt.issuedAt, jwt.expiresAt))
  }

  @Test
  fun `refresh token values are unique, opaque and URL-safe`() {
    val first = service.newRefreshTokenValue()
    val second = service.newRefreshTokenValue()
    assertNotEquals(first, second)
    assertTrue(first.length >= 43, "256 bits of entropy expected")
    assertTrue(first.matches(Regex("^[A-Za-z0-9_-]+$")))
  }

  @Test
  fun `hashing is deterministic and never echoes the token`() {
    val token = service.newRefreshTokenValue()
    val hash = service.hash(token)
    assertEquals(hash, service.hash(token))
    assertNotEquals(token, hash)
    assertTrue(hash.matches(Regex("^[0-9a-f]{64}$")))
  }

  @Test
  fun `a short signing secret is rejected at startup`() {
    assertFailsWith<IllegalArgumentException> { AuthProperties(jwtSecret = "too-short") }
  }
}
