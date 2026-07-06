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
        registry.addRedirectViewController("/v1/docs", "/v1/docs/index.html")
    }

    @Bean
    fun yamlMimeMapping() =
        WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> { factory ->
            val mappings = MimeMappings(MimeMappings.DEFAULT)
            mappings.add("yaml", "application/yaml")
            mappings.add("yml", "application/yaml")
            factory.setMimeMappings(mappings)
        }
}
