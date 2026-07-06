package mg.fikaliako.api.util

import mg.fikaliako.api.model.OpeningInterval
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * Open/closed status is computed server-side in Indian/Antananarivo (project
 * book ch. 4.9). Days are 0 = Monday … 6 = Sunday. An interval whose `closesAt`
 * is not after `opensAt` wraps past midnight and is evaluated against both the
 * day it starts and the following calendar day.
 */
object OpeningHours {
    val ZONE: ZoneId = ZoneId.of("Indian/Antananarivo")

    fun isOpenNow(
        open24h: Boolean,
        intervals: List<OpeningInterval>,
        now: Instant,
    ): Boolean {
        if (open24h) return true
        val local = now.atZone(ZONE)
        val today = local.dayOfWeek.value - 1 // Monday(1)..Sunday(7) -> 0..6
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
            // Same-day interval: [opens, closes)
            !wraps && interval.dayOfWeek == today -> !time.isBefore(opens) && time.isBefore(closes)

            // Wrapping interval starting today: [opens, 24:00)
            wraps && interval.dayOfWeek == today -> !time.isBefore(opens)

            // Wrapping interval started yesterday: [00:00, closes)
            wraps && interval.dayOfWeek == yesterday -> time.isBefore(closes)

            else -> false
        }
    }
}
