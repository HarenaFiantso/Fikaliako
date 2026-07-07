package mg.fikaliako.api.util

import mg.fikaliako.api.model.exception.BadRequestException
import java.time.Instant
import java.util.Base64
import java.util.UUID

data class Cursor(
  val createdAt: Instant,
  val id: UUID,
) {
  fun encode(): String {
    val raw = "$createdAt|$id"
    return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.toByteArray())
  }

  companion object {
    fun decode(value: String): Cursor {
      try {
        val raw = String(Base64.getUrlDecoder().decode(value))
        val (ts, id) = raw.split("|", limit = 2)
        return Cursor(Instant.parse(ts), UUID.fromString(id))
      } catch (e: IllegalArgumentException) {
        throw BadRequestException("Malformed cursor.", e)
      } catch (e: IndexOutOfBoundsException) {
        throw BadRequestException("Malformed cursor.", e)
      }
    }
  }
}
