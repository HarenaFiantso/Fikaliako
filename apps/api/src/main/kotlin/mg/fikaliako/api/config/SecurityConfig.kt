package mg.fikaliako.api.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

/**
 * Stateless API: browsing is public, only writes (reviews, contributions,
 * favorites) require an account — project book, ch. 4.7. JWT comes with the
 * accounts module.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig {

	@Bean
	fun filterChain(http: HttpSecurity): SecurityFilterChain {
		http
			.csrf { it.disable() }
			.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
			.authorizeHttpRequests {
				it
					.requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
					.requestMatchers("/v1/openapi.yaml", "/v1/docs", "/v1/docs/**", "/webjars/**").permitAll()
					.requestMatchers(HttpMethod.GET, "/v1/**").permitAll()
					.anyRequest().authenticated()
			}
		return http.build()
	}
}
