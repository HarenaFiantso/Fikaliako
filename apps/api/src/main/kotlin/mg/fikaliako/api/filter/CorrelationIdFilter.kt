package mg.fikaliako.api.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CorrelationIdFilter : OncePerRequestFilter() {
  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain,
  ) {
    val correlationId = request.getHeader(HEADER)?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
    request.setAttribute(ATTRIBUTE, correlationId)
    response.setHeader(HEADER, correlationId)
    MDC.put(MDC_KEY, correlationId)
    try {
      filterChain.doFilter(request, response)
    } finally {
      MDC.remove(MDC_KEY)
    }
  }

  companion object {
    const val HEADER = "X-Correlation-Id"
    const val ATTRIBUTE = "correlationId"
    const val MDC_KEY = "correlation_id"
  }
}
