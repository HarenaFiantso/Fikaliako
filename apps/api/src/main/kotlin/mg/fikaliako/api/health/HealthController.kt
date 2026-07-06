package mg.fikaliako.api.health

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1")
class HealthController {
    @GetMapping("/ping")
    fun ping(): Map<String, String> =
        mapOf(
            "service" to "fikaliako-api",
            "message" to "Aiza no hisakafo androany ?",
        )
}
