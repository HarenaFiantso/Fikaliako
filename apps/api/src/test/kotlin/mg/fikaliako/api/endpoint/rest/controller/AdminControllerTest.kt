package mg.fikaliako.api.endpoint.rest.controller

import mg.fikaliako.api.config.SecurityConfig
import mg.fikaliako.api.service.AdminService
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.put
import java.util.UUID

// RBAC around /v1/admin/**: only ROLE_ADMIN passes
@WebMvcTest(AdminController::class)
@Import(SecurityConfig::class)
class AdminControllerTest {
  @Autowired
  lateinit var mockMvc: MockMvc

  @MockitoBean
  lateinit var service: AdminService

  private val adminId = UUID.fromString("dddddddd-0000-0000-0000-000000000001")
  private val establishmentId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001")
  private val businessUserId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002")

  @Test
  fun `a business token cannot grant managers`() {
    mockMvc
      .put("/v1/admin/establishments/$establishmentId/managers/$businessUserId") {
        with(
          jwt()
            .jwt { it.subject(businessUserId.toString()).claim("role", "business") }
            .authorities(SimpleGrantedAuthority("ROLE_BUSINESS")),
        )
      }.andExpect {
        status { isForbidden() }
        content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
      }
  }

  @Test
  fun `an admin grants a manager with a 204`() {
    mockMvc
      .put("/v1/admin/establishments/$establishmentId/managers/$businessUserId") {
        with(
          jwt()
            .jwt { it.subject(adminId.toString()).claim("role", "admin") }
            .authorities(SimpleGrantedAuthority("ROLE_ADMIN")),
        )
      }.andExpect { status { isNoContent() } }

    Mockito.verify(service).grantManager(adminId, establishmentId, businessUserId)
  }
}
