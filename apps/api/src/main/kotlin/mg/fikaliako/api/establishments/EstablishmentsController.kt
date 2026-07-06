package mg.fikaliako.api.establishments

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1")
class EstablishmentsController {

	@GetMapping("/ping")
	fun ping(): Map<String, String> = mapOf(
		"service" to "fikaliako-api",
		"message" to "Aiza no hisakafo androany ?",
	)
}
