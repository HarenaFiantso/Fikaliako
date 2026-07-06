package mg.fikaliako.api.util

import mg.fikaliako.api.model.OpeningInterval
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZonedDateTime
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OpeningHoursTest {
  // 2026-07-06 is a Monday (day 0).
  private fun monday(
    hour: Int,
    minute: Int = 0,
  ): Instant = ZonedDateTime.of(2026, 7, 6, hour, minute, 0, 0, OpeningHours.ZONE).toInstant()

  private fun tuesday(
    hour: Int,
    minute: Int = 0,
  ): Instant = ZonedDateTime.of(2026, 7, 7, hour, minute, 0, 0, OpeningHours.ZONE).toInstant()

  private val mondayNineToNine = listOf(OpeningInterval(0, "07:00", "21:00"))

  @Test
  fun `open within the interval`() {
    assertTrue(OpeningHours.isOpenNow(false, mondayNineToNine, monday(12)))
  }

  @Test
  fun `closed before opening and after closing`() {
    assertFalse(OpeningHours.isOpenNow(false, mondayNineToNine, monday(6, 59)))
    assertFalse(OpeningHours.isOpenNow(false, mondayNineToNine, monday(21)))
  }

  @Test
  fun `open24h is always open regardless of intervals`() {
    assertTrue(OpeningHours.isOpenNow(true, emptyList(), monday(3)))
  }

  @Test
  fun `no intervals means closed`() {
    assertFalse(OpeningHours.isOpenNow(false, emptyList(), monday(12)))
  }

  @Test
  fun `interval wrapping past midnight is open late on its own day`() {
    val wraps = listOf(OpeningInterval(0, "20:00", "02:00"))
    assertTrue(OpeningHours.isOpenNow(false, wraps, monday(23, 30)))
  }

  @Test
  fun `interval wrapping past midnight is open early the next day`() {
    val wraps = listOf(OpeningInterval(0, "20:00", "02:00"))
    assertTrue(OpeningHours.isOpenNow(false, wraps, tuesday(1)))
    assertFalse(OpeningHours.isOpenNow(false, wraps, tuesday(3)))
  }
}
