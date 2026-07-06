package mg.fikaliako.api.exception

import jakarta.servlet.http.HttpServletRequest
import mg.fikaliako.api.filter.CorrelationIdFilter
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class ApiExceptionHandler {
  private val log = LoggerFactory.getLogger(javaClass)

  @ExceptionHandler(NotFoundException::class)
  fun handleNotFound(
    ex: NotFoundException,
    request: HttpServletRequest,
  ): ProblemDetail = problem(HttpStatus.NOT_FOUND, "Not found", ex.message, request)

  @ExceptionHandler(BadRequestException::class)
  fun handleBadRequest(
    ex: BadRequestException,
    request: HttpServletRequest,
  ): ProblemDetail = problem(HttpStatus.BAD_REQUEST, "Bad request", ex.message, request)

  @ExceptionHandler(HandlerMethodValidationException::class)
  fun handleConstraint(
    ex: HandlerMethodValidationException,
    request: HttpServletRequest,
  ): ProblemDetail = problem(HttpStatus.BAD_REQUEST, "Invalid request parameters", ex.message, request)

  @ExceptionHandler(MethodArgumentNotValidException::class)
  fun handleBodyValidation(
    ex: MethodArgumentNotValidException,
    request: HttpServletRequest,
  ): ProblemDetail {
    val detail =
      ex.bindingResult.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
    return problem(HttpStatus.BAD_REQUEST, "Validation failed", detail, request)
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException::class)
  fun handleTypeMismatch(
    ex: MethodArgumentTypeMismatchException,
    request: HttpServletRequest,
  ): ProblemDetail = problem(HttpStatus.BAD_REQUEST, "Bad request", "Invalid value for '${ex.name}'.", request)

  @ExceptionHandler(MissingServletRequestParameterException::class)
  fun handleMissingParam(
    ex: MissingServletRequestParameterException,
    request: HttpServletRequest,
  ): ProblemDetail = problem(HttpStatus.BAD_REQUEST, "Bad request", "Missing required parameter '${ex.parameterName}'.", request)

  @ExceptionHandler(Exception::class)
  fun handleUnexpected(
    ex: Exception,
    request: HttpServletRequest,
  ): ProblemDetail {
    log.error("Unhandled exception on {}", request.requestURI, ex)
    return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", "Unexpected server error.", request)
  }

  private fun problem(
    status: HttpStatus,
    title: String,
    detail: String?,
    request: HttpServletRequest,
  ): ProblemDetail =
    ProblemDetail.forStatusAndDetail(status, detail ?: status.reasonPhrase).apply {
      this.title = title
      setProperty("correlation_id", request.getAttribute(CorrelationIdFilter.ATTRIBUTE))
    }
}
