package mg.fikaliako.api.util

import mg.fikaliako.api.model.OpeningInterval
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

object OpeningHours {
  val ZONE: ZoneId = ZoneId.of("Indian/Antananarivo")

  fun isOpenNow(
    open24h: Boolean,
    intervals: List<OpeningInterval>,
    now: Instant,
  ): Boolean {
    if (open24h) return true
    val local = now.atZone(ZONE)
    val today = local.dayOfWeek.value - 1
    val yesterday = (today + 6) % 7
    val time = local.toLocalTime()
    return intervals.any { covers(it, today, yesterday, time) }
  }

  private fun covers(
    interval: OpeningInterval,
    today: Int,
    yesterday: Int,
    time: LocalTime,
  ): Boolean {
    val opens = LocalTime.parse(interval.opensAt)
    val closes = LocalTime.parse(interval.closesAt)
    val wraps = !closes.isAfter(opens)
    return when {
      !wraps && interval.dayOfWeek == today -> !time.isBefore(opens) && time.isBefore(closes)
      wraps && interval.dayOfWeek == today -> !time.isBefore(opens)
      wraps && interval.dayOfWeek == yesterday -> time.isBefore(closes)
      else -> false
    }
  }
}
