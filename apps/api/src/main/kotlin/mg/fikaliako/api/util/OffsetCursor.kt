package mg.fikaliako.api.util

import mg.fikaliako.api.model.exception.BadRequestException
import java.util.Base64

/**
 * Offset-carrying cursor for relevance-ordered search results, where keyset
 * pagination has no stable key. Encodes `{"offset":N}` in base64url — the
 * exact shape of the project book's `next_cursor` example (ch. 8.3).
 */
object OffsetCursor {
  private val PATTERN = Regex("""\{"offset":(\d{1,9})\}""")

  fun encode(offset: Int): String =
    Base64
      .getUrlEncoder()
      .withoutPadding()
      .encodeToString("""{"offset":$offset}""".toByteArray())

  fun decode(value: String): Int {
    val raw =
      try {
        String(Base64.getUrlDecoder().decode(value))
      } catch (e: IllegalArgumentException) {
        throw BadRequestException("Malformed cursor.", e)
      }
    val match = PATTERN.matchEntire(raw) ?: throw BadRequestException("Malformed cursor.")
    return match.groupValues[1].toInt()
  }
}
