package mg.fikaliako.api.model.exception

class NotFoundException(
  message: String,
) : RuntimeException(message)

class BadRequestException(
  message: String,
  cause: Throwable? = null,
) : RuntimeException(message, cause)
