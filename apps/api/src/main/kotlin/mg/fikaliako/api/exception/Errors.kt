package mg.fikaliako.api.exception

/** 404 — the requested resource does not exist. */
class NotFoundException(
    message: String,
) : RuntimeException(message)

/** 400 — the request is syntactically valid but semantically wrong. */
class BadRequestException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
