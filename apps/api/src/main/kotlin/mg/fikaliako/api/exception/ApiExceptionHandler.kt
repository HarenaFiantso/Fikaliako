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

/**
 * Renders every error as RFC 9457 `application/problem+json` (project book
 * ch. 8), stamped with the request's correlation id. Spring emits this content
 * type automatically for [ProblemDetail] return values.
 */
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

    /** Bean Validation failures on `@RequestParam`/`@PathVariable` (single values). */
    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleConstraint(
        ex: HandlerMethodValidationException,
        request: HttpServletRequest,
    ): ProblemDetail = problem(HttpStatus.BAD_REQUEST, "Invalid request parameters", ex.message, request)

    /** Bean Validation failures on a `@Valid @RequestBody` payload. */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleBodyValidation(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ProblemDetail {
        val detail =
            ex.bindingResult.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", detail, request)
    }

    /** Wrong type for a query/path param, e.g. `?limit=abc` or a non-UUID id. */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(
        ex: MethodArgumentTypeMismatchException,
        request: HttpServletRequest,
    ): ProblemDetail = problem(HttpStatus.BAD_REQUEST, "Bad request", "Invalid value for '${ex.name}'.", request)

    /** A required query parameter was absent, e.g. `/v1/nearby` without `lat`. */
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParam(
        ex: MissingServletRequestParameterException,
        request: HttpServletRequest,
    ): ProblemDetail =
        problem(HttpStatus.BAD_REQUEST, "Bad request", "Missing required parameter '${ex.parameterName}'.", request)

    /** Catch-all so unexpected failures still return problem+json with a correlation id. */
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
