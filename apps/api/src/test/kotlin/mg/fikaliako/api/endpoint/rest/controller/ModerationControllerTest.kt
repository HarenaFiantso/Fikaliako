package mg.fikaliako.api.endpoint.rest.controller

import mg.fikaliako.api.config.SecurityConfig
import mg.fikaliako.api.endpoint.rest.model.ContributionItem
import mg.fikaliako.api.endpoint.rest.model.Page
import mg.fikaliako.api.service.ModerationService
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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.request.RequestPostProcessor
import java.time.Instant
import java.util.UUID

@WebMvcTest(ModerationController::class)
@Import(SecurityConfig::class)
class ModerationControllerTest {
  @Autowired
  lateinit var mockMvc: MockMvc

  @MockitoBean
  lateinit var service: ModerationService

  private val moderatorId = UUID.fromString("cccccccc-0000-0000-0000-000000000001")
  private val contributionId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001")

  private fun moderator(): RequestPostProcessor =
    jwt()
      .jwt { it.subject(moderatorId.toString()).claim("role", "moderator") }
      .authorities(SimpleGrantedAuthority("ROLE_MODERATOR"))

  private fun item(status: String = "pending") =
    ContributionItem(
      id = contributionId,
      type = "create",
      status = status,
      payload = mapOf("name" to "Chez Bao"),
      establishmentId = null,
      authorName = "Naina",
      confirmationsCount = 0,
      resolutionNote = null,
      createdAt = Instant.parse("2026-07-20T09:00:00Z"),
      resolvedAt = null,
    )

  @Test
  fun `the queue requires authentication`() {
    mockMvc.get("/v1/moderation/contributions").andExpect {
      status { isUnauthorized() }
      content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
    }
  }

  @Test
  fun `a consumer token cannot read the queue`() {
    mockMvc
      .get("/v1/moderation/contributions") {
        with(
          jwt()
            .jwt { it.subject(UUID.randomUUID().toString()).claim("role", "user") }
            .authorities(SimpleGrantedAuthority("ROLE_USER")),
        )
      }.andExpect {
        status { isForbidden() }
        content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
      }
  }

  @Test
  fun `a moderator reads the queue`() {
    Mockito.`when`(service.queue("pending", null, null)).thenReturn(Page(listOf(item())))

    mockMvc
      .get("/v1/moderation/contributions?status=pending") { with(moderator()) }
      .andExpect {
        status { isOk() }
        jsonPath("$.items[0].id") { value(contributionId.toString()) }
        jsonPath("$.items[0].status") { value("pending") }
        jsonPath("$.items[0].author_name") { value("Naina") }
        jsonPath("$.items[0].payload.name") { value("Chez Bao") }
      }
  }

  @Test
  fun `an admin approves a contribution`() {
    Mockito
      .`when`(service.approve(moderatorId, contributionId))
      .thenReturn(item(status = "applied"))

    mockMvc
      .post("/v1/moderation/contributions/$contributionId/approve") {
        with(
          jwt()
            .jwt { it.subject(moderatorId.toString()).claim("role", "admin") }
            .authorities(SimpleGrantedAuthority("ROLE_ADMIN")),
        )
      }.andExpect {
        status { isOk() }
        jsonPath("$.status") { value("applied") }
      }
  }

  @Test
  fun `a moderator rejects with a reason`() {
    Mockito
      .`when`(service.reject(moderatorId, contributionId, "Duplicate"))
      .thenReturn(item(status = "rejected"))

    mockMvc
      .post("/v1/moderation/contributions/$contributionId/reject") {
        with(moderator())
        contentType = MediaType.APPLICATION_JSON
        content = """{"reason":"Duplicate"}"""
      }.andExpect {
        status { isOk() }
        jsonPath("$.status") { value("rejected") }
      }

    Mockito.verify(service).reject(moderatorId, contributionId, "Duplicate")
  }

  @Test
  fun `rejecting without a body is allowed`() {
    Mockito
      .`when`(service.reject(moderatorId, contributionId, null))
      .thenReturn(item(status = "rejected"))

    mockMvc
      .post("/v1/moderation/contributions/$contributionId/reject") { with(moderator()) }
      .andExpect { status { isOk() } }
  }
}
