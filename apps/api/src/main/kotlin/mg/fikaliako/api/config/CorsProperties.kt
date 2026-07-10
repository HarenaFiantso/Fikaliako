package mg.fikaliako.api.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Browser clients (the Next.js web app) call the API from another origin,
 * so the allowed origins must be explicit. Tokens travel in the
 * Authorization header, never in cookies, so credentials stay disabled.
 */
@ConfigurationProperties("fikaliako.cors")
data class CorsProperties(
  val allowedOrigins: List<String> = listOf("http://localhost:3000"),
)
