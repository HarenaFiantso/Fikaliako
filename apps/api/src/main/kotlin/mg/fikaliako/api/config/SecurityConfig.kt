package mg.fikaliako.api.config

import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import tools.jackson.databind.ObjectMapper
import javax.crypto.spec.SecretKeySpec

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(AuthProperties::class, CorsProperties::class)
class SecurityConfig(
  private val props: AuthProperties,
  private val corsProps: CorsProperties,
) {
  @Bean
  fun filterChain(
    http: HttpSecurity,
    jwtDecoder: JwtDecoder,
    jwtAuthenticationConverter: JwtAuthenticationConverter,
    objectMapper: ObjectMapper,
  ): SecurityFilterChain {
    val entryPoint = ProblemAuthenticationEntryPoint(objectMapper)
    val deniedHandler = ProblemAccessDeniedHandler(objectMapper)
    http
      .cors {}
      .csrf { it.disable() }
      .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
      .oauth2ResourceServer { rs ->
        rs.jwt {
          it.decoder(jwtDecoder)
          it.jwtAuthenticationConverter(jwtAuthenticationConverter)
        }
        rs.authenticationEntryPoint(entryPoint)
        rs.accessDeniedHandler(deniedHandler)
      }.exceptionHandling {
        it.authenticationEntryPoint(entryPoint)
        it.accessDeniedHandler(deniedHandler)
      }.authorizeHttpRequests {
        it
          .requestMatchers("/actuator/health/**", "/actuator/info")
          .permitAll()
          .requestMatchers("/v1/openapi.yaml", "/v1/docs", "/v1/docs/**", "/webjars/**")
          .permitAll()
          .requestMatchers("/error")
          .permitAll()
          // change-password is the one /v1/auth endpoint that needs a session
          .requestMatchers("/v1/auth/change-password")
          .authenticated()
          .requestMatchers("/v1/auth/**")
          .permitAll()
          .requestMatchers("/v1/users/me", "/v1/users/me/**")
          .authenticated()
          .requestMatchers("/v1/business/**")
          .hasAnyRole("BUSINESS", "ADMIN")
          .requestMatchers("/v1/admin/**")
          .hasRole("ADMIN")
          .requestMatchers(HttpMethod.GET, "/v1/**")
          .permitAll()
          .anyRequest()
          .authenticated()
      }
    return http.build()
  }

  @Bean
  fun corsConfigurationSource(): CorsConfigurationSource {
    val config =
      CorsConfiguration().apply {
        allowedOrigins = corsProps.allowedOrigins
        allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        allowedHeaders = listOf("Authorization", "Content-Type", "Accept")
        exposedHeaders = listOf("X-Correlation-Id")
        maxAge = 3600
      }
    return UrlBasedCorsConfigurationSource().apply { registerCorsConfiguration("/**", config) }
  }

  // Argon2id with OWASP parameters (book ch. 7.3): m=19 MiB, t=2, p=1
  @Bean
  fun passwordEncoder(): PasswordEncoder = Argon2PasswordEncoder(16, 32, 1, 19_456, 2)

  @Bean
  fun jwtDecoder(): JwtDecoder {
    val decoder =
      NimbusJwtDecoder
        .withSecretKey(secretKey())
        .macAlgorithm(MacAlgorithm.HS256)
        .build()
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(props.issuer))
    return decoder
  }

  @Bean
  fun jwtEncoder(): JwtEncoder = NimbusJwtEncoder(ImmutableSecret<SecurityContext>(secretKey()))

  // The `role` claim (user/business/moderator/admin) becomes the sole authority
  @Bean
  fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
    val converter = JwtAuthenticationConverter()
    converter.setJwtGrantedAuthoritiesConverter { jwt: Jwt ->
      val role = jwt.getClaimAsString("role") ?: "user"
      mutableListOf<GrantedAuthority>(SimpleGrantedAuthority("ROLE_" + role.uppercase()))
    }
    return converter
  }

  private fun secretKey() = SecretKeySpec(props.jwtSecret.toByteArray(), "HmacSHA256")
}
