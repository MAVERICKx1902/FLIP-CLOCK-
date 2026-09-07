package com.example.liquidprismclock.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Immutable snapshot of everything the UI renders at one wall-clock instant.
 *
 * The app NEVER fakes the clock: this is built from [Calendar.getInstance]
 * (device local time) on every tick and tick timing is aligned to the actual
 * second boundary (see ClockTicker).
 */
data class TimeData(
    val hour24: Int,        // 0..23
    val minute: Int,        // 0..59
    val second: Int,        // 0..59
    val millis: Int,        // 0..999
    val dayOfWeek: Int,     // Calendar.DAY_OF_WEEK
    val date: Date,
) {
    val hourOfAmPm: Int
        get() = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }

    val isPm: Boolean get() = hour24 >= 12

    val timeOfDay: TimeOfDay get() = TimeOfDay.fromHour(hour24)

    /** Fractional hour 0..24 used for smooth palette transitions. */
    val hourFloat: Float
        get() = hour24 + minute / 60f + (second + millis / 1000f) / 3600f

    /**
     * The six always-present clock slots:
     * [0]=hour tens, [1]=hour ones, [2]=minute tens, [3]=minute ones,
     * [4]=second tens, [5]=second ones.
     *
     * In 12-hour mode hour tens is a blank (shown as space / hidden placeholder)
     * so the layout stays stable and centered across 9:42 / 12:42.
     */
    fun slotDigits(use24Hour: Boolean): IntArray {
        val hour = if (use24Hour) hour24 else hourOfAmPm
        return intArrayOf(
            hour / 10,
            hour % 10,
            minute / 10,
            minute % 10,
            second / 10,
            second % 10,
        )
    }

    fun spokenTime(use24Hour: Boolean): String {
        val h = if (use24Hour) hour24 else hourOfAmPm
        val hh = h.toString().padStart(2, '0')
        val mm = minute.toString().padStart(2, '0')
        val ss = second.toString().padStart(2, '0')
        return if (use24Hour) {
            "$hh:$mm:$ss"
        } else {
            "$hh:$mm:$ss ${if (isPm) "PM" else "AM"}"
        }
    }

    fun dateString(locale: Locale): String =
        SimpleDateFormat("EEEE, MMMM d, yyyy", locale).format(date)

    companion object {
        /** Build from the real device clock. */
        fun now(): TimeData {
            val cal = Calendar.getInstance()
            return TimeData(
                hour24 = cal.get(Calendar.HOUR_OF_DAY),
                minute = cal.get(Calendar.MINUTE),
                second = cal.get(Calendar.SECOND),
                millis = cal.get(Calendar.MILLISECOND),
                dayOfWeek = cal.get(Calendar.DAY_OF_WEEK),
                date = cal.time,
            )
        }
    }
}
