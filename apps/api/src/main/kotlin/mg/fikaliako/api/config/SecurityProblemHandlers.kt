package mg.fikaliako.api.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import tools.jackson.databind.ObjectMapper

// Spring Security rejections happen before @RestControllerAdvice can run, so
// 401/403 get their RFC 9457 problem+json bodies (with correlation_id) here.

class ProblemAuthenticationEntryPoint(
  private val objectMapper: ObjectMapper,
) : AuthenticationEntryPoint {
  override fun commence(
    request: HttpServletRequest,
    response: HttpServletResponse,
    authException: AuthenticationException,
  ) {
    response.setHeader("WWW-Authenticate", "Bearer")
    writeProblem(
      objectMapper,
      request,
      response,
      HttpStatus.UNAUTHORIZED,
      "Unauthorized",
      "Authentication is required: provide a valid bearer token.",
    )
  }
}

class ProblemAccessDeniedHandler(
  private val objectMapper: ObjectMapper,
) : AccessDeniedHandler {
  override fun handle(
    request: HttpServletRequest,
    response: HttpServletResponse,
    accessDeniedException: AccessDeniedException,
  ) {
    writeProblem(
      objectMapper,
      request,
      response,
      HttpStatus.FORBIDDEN,
      "Forbidden",
      "Your account is not allowed to perform this action.",
    )
  }
}

private fun writeProblem(
  objectMapper: ObjectMapper,
  request: HttpServletRequest,
  response: HttpServletResponse,
  status: HttpStatus,
  title: String,
  detail: String,
) {
  response.status = status.value()
  response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
  val body =
    buildMap {
      put("type", "about:blank")
      put("title", title)
      put("status", status.value())
      put("detail", detail)
      put("instance", request.requestURI)
      request.getAttribute(CorrelationIdFilter.ATTRIBUTE)?.let { put("correlation_id", it) }
    }
  response.writer.write(objectMapper.writeValueAsString(body))
}
