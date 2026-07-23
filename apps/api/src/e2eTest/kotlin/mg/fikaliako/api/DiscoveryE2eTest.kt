package mg.fikaliako.api

import mg.fikaliako.api.service.RatingAggregationService
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * End-to-end discovery journey (project book ch. 4.1–4.6, 6.2, 8.2) on a real
 * PostGIS: Flyway migrations run for real, the native geospatial/trigram SQL
 * executes for real, and every scenario goes through the full HTTP stack.
 *
 * The stage is Analakely (the reference point of the book's ch. 8.3 example):
 * four establishments within walking distance, one gargotte across town, one
 * closed snack. Opening status uses open_24h (deterministic regardless of when
 * the suite runs); the wrap-around interval logic is covered by unit tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DiscoveryE2eTest {
  companion object {
    // Started eagerly (singleton-container pattern); Testcontainers' Ryuk
    // reaper removes it when the JVM exits.
    @JvmStatic
    val postgis: PostgreSQLContainer<*> =
      PostgreSQLContainer(
        DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"),
      ).also { it.start() }

    @DynamicPropertySource
    @JvmStatic
    fun datasource(registry: DynamicPropertyRegistry) {
      registry.add("spring.datasource.url", postgis::getJdbcUrl)
      registry.add("spring.datasource.username", postgis::getUsername)
      registry.add("spring.datasource.password", postgis::getPassword)
    }

    const val ANALAKELY_LAT = -18.9101
    const val ANALAKELY_LNG = 47.5257

    const val E_MAMA_FARA = "11111111-0000-0000-0000-000000000001"
    const val E_ROMAZAVA_HOUSE = "11111111-0000-0000-0000-000000000002"
    const val E_PIZZA_BELLA = "11111111-0000-0000-0000-000000000003"
    const val E_CLOSED_CORNER = "11111111-0000-0000-0000-000000000004"
    const val E_HOTELY_KELY = "11111111-0000-0000-0000-000000000005"

    const val U1 = "22222222-0000-0000-0000-000000000001"
    const val U2 = "22222222-0000-0000-0000-000000000002"
    const val U3 = "22222222-0000-0000-0000-000000000003"
  }

  @Autowired
  lateinit var mockMvc: MockMvc

  @Autowired
  lateinit var jdbc: JdbcTemplate

  @Autowired
  lateinit var ratingAggregationService: RatingAggregationService

  @BeforeAll
  fun seed() {
    jdbc.execute("TRUNCATE establishments, users CASCADE")
    users()
    // Around the Analakely reference point (-18.9101, 47.5257)
    establishment(
      id = E_MAMA_FARA,
      slug = "mama-fara",
      name = "Gargotte Chez Mama Fara",
      type = "gargotte",
      lat = -18.9095,
      lng = 47.5263,
      price = 4000,
      extras = "student_friendly = TRUE, terrace = TRUE",
      ageDays = 200,
    )
    establishment(
      id = E_ROMAZAVA_HOUSE,
      slug = "romazava-house",
      name = "Romazava House",
      type = "restaurant",
      lat = -18.9110,
      lng = 47.5280,
      price = 8000,
      extras = "romantic = TRUE",
      ageDays = 300,
    )
    establishment(
      id = E_PIZZA_BELLA,
      slug = "pizza-bella",
      name = "Pizza Bella",
      type = "restaurant",
      lat = -18.9130,
      lng = 47.5300,
      price = 15000,
      extras = "delivery = TRUE",
      ageDays = 400,
    )
    establishment(
      id = E_CLOSED_CORNER,
      slug = "closed-corner",
      name = "Closed Corner",
      type = "snack",
      lat = -18.9105,
      lng = 47.5260,
      price = 3000,
      open24h = false,
      ageDays = 100,
    )
    // Across town (~4 km) and freshly added — the revelation candidate
    establishment(
      id = E_HOTELY_KELY,
      slug = "hotely-kely",
      name = "Hotely Kely",
      type = "gargotte",
      lat = -18.9400,
      lng = 47.5500,
      price = 2500,
      ageDays = 10,
    )

    cuisine(E_MAMA_FARA, "malagasy")
    cuisine(E_ROMAZAVA_HOUSE, "malagasy")
    cuisine(E_PIZZA_BELLA, "fast_food")
    cuisine(E_HOTELY_KELY, "malagasy")
    payment(E_MAMA_FARA, "cash")
    payment(E_MAMA_FARA, "mvola")
    payment(E_ROMAZAVA_HOUSE, "cash")
    payment(E_ROMAZAVA_HOUSE, "carte")

    review(E_MAMA_FARA, U1, quality = 5, others = 5, ageDays = 5)
    review(E_MAMA_FARA, U2, quality = 5, others = 4, ageDays = 15)
    review(E_MAMA_FARA, U3, quality = 4, others = 4, ageDays = 40)
    review(E_ROMAZAVA_HOUSE, U1, quality = 4, others = 3, ageDays = 60)
    review(E_ROMAZAVA_HOUSE, U2, quality = 3, others = 3, ageDays = 90)
    review(E_ROMAZAVA_HOUSE, U3, quality = 4, others = 4, ageDays = 120)
    review(E_HOTELY_KELY, U1, quality = 5, others = 5, ageDays = 2)

    // The nightly job (ch. 4.6), invoked directly: fills averages + Bayesian notes
    ratingAggregationService.recomputeAll()
  }

  @Test
  fun `nearby returns walking-distance establishments cheapest first`() {
    mockMvc
      .get("/v1/nearby") {
        param("lat", "$ANALAKELY_LAT")
        param("lng", "$ANALAKELY_LNG")
        param("radius", "1000")
      }.andExpect {
        status { isOk() }
        jsonPath("$.items.length()") { value(4) }
        jsonPath("$.items[0].slug") { value("closed-corner") }
        jsonPath("$.items[1].slug") { value("mama-fara") }
        jsonPath("$.items[2].slug") { value("romazava-house") }
        jsonPath("$.items[3].slug") { value("pizza-bella") }
        jsonPath("$.items[1].distance_m") { isNumber() }
        jsonPath("$.items[1].rating_count") { value(3) }
        jsonPath("$.items[1].rating_avg") { isNumber() }
      }
  }

  @Test
  fun `nearby open_now drops the closed snack`() {
    mockMvc
      .get("/v1/nearby") {
        param("lat", "$ANALAKELY_LAT")
        param("lng", "$ANALAKELY_LNG")
        param("radius", "1000")
        param("open_now", "true")
      }.andExpect {
        status { isOk() }
        jsonPath("$.items.length()") { value(3) }
        jsonPath("$.items[0].slug") { value("mama-fara") }
      }
  }

  @Test
  fun `nearby respects the budget king filter`() {
    mockMvc
      .get("/v1/nearby") {
        param("lat", "$ANALAKELY_LAT")
        param("lng", "$ANALAKELY_LNG")
        param("radius", "1000")
        param("max_price", "5000")
      }.andExpect {
        status { isOk() }
        jsonPath("$.items.length()") { value(2) }
        jsonPath("$.items[0].slug") { value("closed-corner") }
        jsonPath("$.items[1].slug") { value("mama-fara") }
      }
  }

  @Test
  fun `search survives a typo`() {
    mockMvc
      .get("/v1/search") { param("q", "romzava") }
      .andExpect {
        status { isOk() }
        jsonPath("$.items[0].slug") { value("romazava-house") }
      }
  }

  @Test
  fun `searching a dish also surfaces the cuisine via FR-MG synonyms`() {
    mockMvc
      .get("/v1/search") { param("q", "romazava") }
      .andExpect {
        status { isOk() }
        // Direct name match first, then malagasy-cuisine establishments by synonym
        jsonPath("$.items[0].slug") { value("romazava-house") }
        jsonPath("$.items[*].slug") { value(org.hamcrest.Matchers.hasItem("mama-fara")) }
      }
  }

  @Test
  fun `search combines text with the budget filter`() {
    mockMvc
      .get("/v1/search") {
        param("q", "romazava")
        param("max_price", "5000")
      }.andExpect {
        status { isOk() }
        jsonPath("$.items[*].slug") {
          value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("romazava-house")))
        }
        jsonPath("$.items[*].slug") { value(org.hamcrest.Matchers.hasItem("mama-fara")) }
      }
  }

  @Test
  fun `j'ai faim near Analakely means open, within 1 km, best first`() {
    mockMvc
      .get("/v1/search") {
        param("q", "j'ai faim")
        param("lat", "$ANALAKELY_LAT")
        param("lng", "$ANALAKELY_LNG")
      }.andExpect {
        status { isOk() }
        jsonPath("$.interpretation.intents[0]") { value("hungry") }
        jsonPath("$.interpretation.open_now") { value(true) }
        jsonPath("$.interpretation.radius_m") { value(1000.0) }
        jsonPath("$.interpretation.ordering") { value("discovery_score") }
        // Closed Corner is closed, Hotely Kely is 4 km away
        jsonPath("$.items.length()") { value(3) }
        // Mama Fara carries the best Bayesian note and the freshest reviews
        jsonPath("$.items[0].slug") { value("mama-fara") }
      }
  }

  @Test
  fun `pizza pas cher keeps the dish but caps the budget`() {
    mockMvc
      .get("/v1/search") { param("q", "pizza pas cher") }
      .andExpect {
        status { isOk() }
        jsonPath("$.interpretation.max_price_ar") { value(5000) }
        jsonPath("$.interpretation.ordering") { value("relevance") }
        // Pizza Bella costs 15 000 Ar — correctly excluded
        jsonPath("$.items.length()") { value(0) }
      }
  }

  @Test
  fun `the establishment page carries everything needed to decide`() {
    mockMvc
      .get("/v1/establishments/mama-fara")
      .andExpect {
        status { isOk() }
        jsonPath("$.name") { value("Gargotte Chez Mama Fara") }
        jsonPath("$.open_now") { value(true) }
        jsonPath("$.avg_price_ar") { value(4000) }
        jsonPath("$.amenities.terrace") { value(true) }
        jsonPath("$.rating.count") { value(3) }
        jsonPath("$.rating.avg_global") { isNumber() }
        jsonPath("$.rating.bayesian_note") { isNumber() }
        jsonPath("$.cuisines[0].code") { value("malagasy") }
        jsonPath("$.payment_methods[*].code") { value(org.hamcrest.Matchers.hasItem("mvola")) }
      }
  }

  @Test
  fun `rankings expose the nightly tops`() {
    mockMvc.get("/v1/rankings").andExpect {
      status { isOk() }
      jsonPath("$.items[*].id") { value(org.hamcrest.Matchers.hasItem("best-gargottes")) }
      jsonPath("$.items[*].id") { value(org.hamcrest.Matchers.hasItem("cuisine-malagasy")) }
    }

    mockMvc.get("/v1/rankings/best-gargottes").andExpect {
      status { isOk() }
      // Mama Fara is the only gargotte with >= 3 reviews (Hotely Kely has 1)
      jsonPath("$.items.length()") { value(1) }
      jsonPath("$.items[0].slug") { value("mama-fara") }
    }

    mockMvc.get("/v1/rankings/revelations").andExpect {
      status { isOk() }
      // Added 10 days ago with a review — the month's revelation
      jsonPath("$.items.length()") { value(1) }
      jsonPath("$.items[0].slug") { value("hotely-kely") }
    }
  }

  @Test
  fun `an unknown ranking is a problem+json 404 with a correlation id`() {
    mockMvc.get("/v1/rankings/best-sushi").andExpect {
      status { isNotFound() }
      jsonPath("$.correlation_id") { isString() }
    }
  }

  private fun users() {
    listOf(U1 to "Naina", U2 to "Hery", U3 to "Voahangy").forEachIndexed { i, (id, name) ->
      jdbc.update(
        "INSERT INTO users (id, phone, phone_verified, display_name, password_hash) VALUES (?::uuid, ?, TRUE, ?, 'x')",
        id,
        "+26134000000$i",
        name,
      )
    }
  }

  private fun establishment(
    id: String,
    slug: String,
    name: String,
    type: String,
    lat: Double,
    lng: Double,
    price: Int,
    open24h: Boolean = true,
    extras: String? = null,
    ageDays: Int,
  ) {
    jdbc.update(
      """
      INSERT INTO establishments (id, name, slug, type, position, city, avg_price_ar, status, open_24h, created_at)
      VALUES (?::uuid, ?, ?, ?::establishment_type,
              CAST(ST_SetSRID(ST_MakePoint(?, ?), 4326) AS geography),
              'Antananarivo', ?, 'active', ?, now() - make_interval(days => ?))
      """.trimIndent(),
      id,
      name,
      slug,
      type,
      lng,
      lat,
      price,
      open24h,
      ageDays,
    )
    extras?.let { jdbc.execute("UPDATE establishments SET $it WHERE id = '$id'") }
  }

  private fun cuisine(
    establishmentId: String,
    code: String,
  ) {
    jdbc.update(
      "INSERT INTO establishment_cuisines (establishment_id, cuisine_code) VALUES (?::uuid, ?)",
      establishmentId,
      code,
    )
  }

  private fun payment(
    establishmentId: String,
    code: String,
  ) {
    jdbc.update(
      "INSERT INTO establishment_payment_methods (establishment_id, payment_method_code) VALUES (?::uuid, ?)",
      establishmentId,
      code,
    )
  }

  private fun review(
    establishmentId: String,
    userId: String,
    quality: Int,
    others: Int,
    ageDays: Int,
  ) {
    jdbc.update(
      """
      INSERT INTO reviews (establishment_id, user_id, rating_quality, rating_price,
                           rating_cleanliness, rating_speed, rating_welcome, created_at)
      VALUES (?::uuid, ?::uuid, ?, ?, ?, ?, ?, now() - make_interval(days => ?))
      """.trimIndent(),
      establishmentId,
      userId,
      quality,
      others,
      others,
      others,
      others,
      ageDays,
    )
  }
}
