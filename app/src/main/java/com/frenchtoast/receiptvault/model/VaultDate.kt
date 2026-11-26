package com.frenchtoast.receiptvault.model

import java.util.Calendar
import java.util.Locale

/**
 * Pure Gregorian epoch-day date helpers compatible with minSdk 24 without core-library desugaring.
 */
object VaultDate {
    private val monthNames = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    )

    fun todayEpochDay(): Long {
        val cal = Calendar.getInstance()
        return fromYmd(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH),
        )
    }

    fun fromYmd(year: Int, month: Int, day: Int): Long {
        // Howard Hinnant civil-to-days algorithm
        val y = if (month <= 2) year - 1 else year
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val m = month
        val doy = (153 * (if (m > 2) m - 3 else m + 9) + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era.toLong() * 146097L + doe.toLong() - 719468L
    }

    fun toYmd(epochDay: Long): Triple<Int, Int, Int> {
        val z = epochDay + 719468L
        val era = (if (z >= 0) z else z - 146096L) / 146097L
        val doe = z - era * 146097L
        val yoe = (doe - doe / 1460L + doe / 36524L - doe / 146096L) / 365L
        val y = yoe + era * 400L
        val doy = doe - (365L * yoe + yoe / 4L - yoe / 100L)
        val mp = (5L * doy + 2L) / 153L
        val day = (doy - (153L * mp + 2L) / 5L + 1L).toInt()
        val month = (mp + if (mp < 10L) 3L else -9L).toInt()
        val year = (y + if (month <= 2) 1L else 0L).toInt()
        return Triple(year, month, day)
    }

    fun toUtcMidnightMillis(epochDay: Long): Long = epochDay * 86_400_000L

    fun fromUtcMillis(utcMillis: Long): Long = Math.floorDiv(utcMillis, 86_400_000L)

    fun plusDays(epochDay: Long, days: Long): Long = epochDay + days

    fun plusYears(epochDay: Long, years: Int): Long {
        val (y, m, d) = toYmd(epochDay)
        val targetYear = y + years
        val maxDay = daysInMonth(targetYear, m)
        return fromYmd(targetYear, m, minOf(d, maxDay))
    }

    private fun daysInMonth(year: Int, month: Int): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
        else -> 30
    }

    fun formatDisplay(epochDay: Long): String {
        val (y, m, d) = toYmd(epochDay)
        val mon = monthNames.getOrElse(m - 1) { "Jan" }
        return String.format(Locale.US, "%s %d, %d", mon, d, y)
    }

    fun formatIso(epochDay: Long): String {
        val (y, m, d) = toYmd(epochDay)
        return String.format(Locale.US, "%04d-%02d-%02d", y, m, d)
    }

    fun parseIsoOrNull(iso: String): Long? {
        val trimmed = iso.trim()
        val parts = trimmed.split("-")
        if (parts.size != 3) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val d = parts[2].toIntOrNull() ?: return null
        if (m !in 1..12 || d !in 1..31 || y !in 1970..2100) return null
        return fromYmd(y, m, minOf(d, daysInMonth(y, m)))
    }
}
