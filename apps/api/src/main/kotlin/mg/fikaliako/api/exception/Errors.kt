package mg.fikaliako.api.exception

class NotFoundException(
  message: String,
) : RuntimeException(message)

class BadRequestException(
  message: String,
  cause: Throwable? = null,
) : RuntimeException(message, cause)
