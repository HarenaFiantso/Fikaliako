package mg.fikaliako.api.model.exception

class NotFoundException(
  message: String,
) : RuntimeException(message)

class BadRequestException(
  message: String,
  cause: Throwable? = null,
) : RuntimeException(message, cause)

class UnauthorizedException(
  message: String,
) : RuntimeException(message)

class ForbiddenException(
  message: String,
) : RuntimeException(message)

class ConflictException(
  message: String,
) : RuntimeException(message)

class TooManyRequestsException(
  message: String,
) : RuntimeException(message)
