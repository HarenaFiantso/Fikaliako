package mg.fikaliako.api.service
import mg.fikaliako.api.config.AuthProperties
import mg.fikaliako.api.model.UserAccount
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.util.Base64
import java.util.UUID

// Token primitives (book ch. 7.3): 15-minute HS256 access JWTs and opaque
// 256-bit refresh tokens. The refresh value is handed out once and only its
// SHA-256 hash is persisted; AuthService owns rotation/revocation semantics.
@Service
class TokenService(
  private val jwtEncoder: JwtEncoder,
  private val props: AuthProperties,
  private val clock: Clock,
) {
  fun issueAccessToken(user: UserAccount): String {
    val now = clock.instant()
    val claims =
      JwtClaimsSet
        .builder()
        .issuer(props.issuer)
        .subject(requireNotNull(user.id).toString())
        .issuedAt(now)
        .expiresAt(now.plus(props.accessTokenTtl))
        .id(UUID.randomUUID().toString())
        .claim("role", user.role.name.lowercase())
        .build()
    val header = JwsHeader.with(MacAlgorithm.HS256).build()
    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
  }

  fun newRefreshTokenValue(): String {
    val bytes = ByteArray(REFRESH_TOKEN_BYTES)
    secureRandom.nextBytes(bytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
  }

  fun hash(tokenValue: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(tokenValue.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
  }

  companion object {
    private const val REFRESH_TOKEN_BYTES = 32
    private val secureRandom = SecureRandom()
  }
}
