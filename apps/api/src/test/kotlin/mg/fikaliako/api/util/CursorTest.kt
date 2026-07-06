package mg.fikaliako.api.util

import mg.fikaliako.api.exception.BadRequestException
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CursorTest {
  @Test
  fun `round-trips through encode and decode`() {
    val cursor =
      Cursor(Instant.parse("2026-07-06T10:15:30Z"), UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001"))
    val decoded = Cursor.decode(cursor.encode())
    assertEquals(cursor, decoded)
  }

  @Test
  fun `rejects a malformed cursor with a bad request`() {
    assertFailsWith<BadRequestException> { Cursor.decode("@@not-base64@@") }
    assertFailsWith<BadRequestException> {
      Cursor.decode(
        java.util.Base64
          .getUrlEncoder()
          .encodeToString("nope".toByteArray()),
      )
    }
  }
}
