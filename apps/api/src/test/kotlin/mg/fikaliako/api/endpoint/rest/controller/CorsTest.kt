package mg.fikaliako.api.endpoint.rest.controller

import mg.fikaliako.api.config.SecurityConfig
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.options

@WebMvcTest(HealthController::class)
@Import(SecurityConfig::class)
class CorsTest {
  @Autowired
  lateinit var mockMvc: MockMvc

  @Test
  fun `preflight from the web app origin is allowed`() {
    mockMvc
      .options("/v1/auth/login") {
        header(HttpHeaders.ORIGIN, "http://localhost:3000")
        header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
        header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type")
      }.andExpect {
        status { isOk() }
        header { string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000") }
        header { string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, org.hamcrest.Matchers.containsString("POST")) }
      }
  }

  @Test
  fun `preflight from an unknown origin is rejected`() {
    mockMvc
      .options("/v1/auth/login") {
        header(HttpHeaders.ORIGIN, "https://evil.example")
        header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
      }.andExpect {
        status { isForbidden() }
      }
  }

  @Test
  fun `simple cross-origin request carries the allow-origin header`() {
    mockMvc
      .get("/v1/ping") {
        header(HttpHeaders.ORIGIN, "http://localhost:3000")
      }.andExpect {
        status { isOk() }
        header { string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000") }
      }
  }
}
