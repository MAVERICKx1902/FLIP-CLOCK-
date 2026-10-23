package com.example.liquidprismclock.model

/**
 * Time-of-day quadrant, following the app's specification:
 *  Morning   06:00–11:59
 *  Afternoon 12:00–16:59
 *  Evening   17:00–20:59
 *  Night     21:00–05:59
 */
enum class TimeOfDay {
    MORNING, AFTERNOON, EVENING, NIGHT;

    companion object {
        fun fromHour(hour: Int): TimeOfDay = when (hour) {
            in 6..11 -> MORNING
            in 12..16 -> AFTERNOON
            in 17..20 -> EVENING
            else -> NIGHT // 21..23 and 0..5
        }
    }
}

/** Manual/auto palette selection. */
enum class ThemeMode {
    AUTO, MORNING, AFTERNOON, EVENING, NIGHT;

    val timeOfDay: TimeOfDay
        get() = when (this) {
            ThemeMode.MORNING -> TimeOfDay.MORNING
            ThemeMode.AFTERNOON -> TimeOfDay.AFTERNOON
            ThemeMode.EVENING -> TimeOfDay.EVENING
            ThemeMode.NIGHT -> TimeOfDay.NIGHT
            ThemeMode.AUTO -> TimeOfDay.NIGHT
        }
}

/** Generic 0..3 intensity used for animation, particles and glow. */
enum class Intensity {
    OFF, LOW, MEDIUM, HIGH;

    /** 0f for OFF … 1f for HIGH, used to scale effects. */
    val scale: Float
        get() = when (this) {
            OFF -> 0f
            LOW -> 0.35f
            MEDIUM -> 0.7f
            HIGH -> 1f
        }

    companion object {
        val DEFAULT = Intensity.MEDIUM
    }
}
