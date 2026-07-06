package mg.fikaliako.api.config

import org.springframework.boot.web.server.MimeMappings
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {

	override fun addViewControllers(registry: ViewControllerRegistry) {
		// Swagger UI is a static page (static/v1/docs/index.html); directory
		// paths don't auto-serve index.html outside the web root
		registry.addRedirectViewController("/v1/docs", "/v1/docs/index.html")
	}

	@Bean
	fun yamlMimeMapping() = WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> { factory ->
		// Tomcat has no default mapping for YAML; without this the OpenAPI
		// contract is served as application/octet-stream
		val mappings = MimeMappings(MimeMappings.DEFAULT)
		mappings.add("yaml", "application/yaml")
		mappings.add("yml", "application/yaml")
		factory.setMimeMappings(mappings)
	}
}
