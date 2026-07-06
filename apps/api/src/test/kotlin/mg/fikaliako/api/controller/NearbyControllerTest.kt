package mg.fikaliako.api.controller

import mg.fikaliako.api.config.SecurityConfig
import mg.fikaliako.api.model.EstablishmentFilters
import mg.fikaliako.api.model.EstablishmentSummary
import mg.fikaliako.api.model.GeoPoint
import mg.fikaliako.api.model.Page
import mg.fikaliako.api.service.EstablishmentService
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.util.UUID

@WebMvcTest(NearbyController::class)
@Import(SecurityConfig::class)
class NearbyControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var service: EstablishmentService

    @Test
    fun `nearby returns items carrying the distance`() {
        val item =
            EstablishmentSummary(
                id = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001"),
                slug = "chez",
                name = "Chez Bao",
                type = "gargotte",
                position = GeoPoint(-18.9, 47.5),
                avgPriceAr = 2500,
                verified = true,
                status = "active",
                ratingAvg = null,
                ratingCount = 0,
                distanceM = 30.5,
            )
        Mockito
            .`when`(service.nearby(-18.9, 47.5, null, EstablishmentFilters(), null))
            .thenReturn(Page(listOf(item)))

        mockMvc
            .get("/v1/nearby") {
                param("lat", "-18.9")
                param("lng", "47.5")
            }.andExpect {
                status { isOk() }
                jsonPath("$.items[0].distance_m") { value(30.5) }
            }
    }

    @Test
    fun `missing lat is a problem+json 400 rather than a 403`() {
        mockMvc.get("/v1/nearby") { param("lng", "47.5") }.andExpect {
            status { isBadRequest() }
            content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
            jsonPath("$.detail") { value("Missing required parameter 'lat'.") }
        }
    }
}
