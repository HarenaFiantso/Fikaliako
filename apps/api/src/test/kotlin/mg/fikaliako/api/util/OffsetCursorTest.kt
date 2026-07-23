package mg.fikaliako.api.util

import mg.fikaliako.api.model.exception.BadRequestException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OffsetCursorTest {
  @Test
  fun `round trips an offset`() {
    assertEquals(0, OffsetCursor.decode(OffsetCursor.encode(0)))
    assertEquals(120, OffsetCursor.decode(OffsetCursor.encode(120)))
  }

  @Test
  fun `decodes the project book example`() {
    // Book ch. 8.3: "next_cursor": "eyJvZmZzZXQiOjUwfQ" — base64 of {"offset":50}
    assertEquals(50, OffsetCursor.decode("eyJvZmZzZXQiOjUwfQ"))
  }

  @Test
  fun `rejects garbage`() {
    assertFailsWith<BadRequestException> { OffsetCursor.decode("not base64 !!") }
    assertFailsWith<BadRequestException> { OffsetCursor.decode(OffsetCursor.encode(1).dropLast(2)) }
  }

  @Test
  fun `rejects well-formed base64 of the wrong document`() {
    val wrong =
      java.util.Base64
        .getUrlEncoder()
        .withoutPadding()
        .encodeToString("""{"page":2}""".toByteArray())
    assertFailsWith<BadRequestException> { OffsetCursor.decode(wrong) }
  }
}
