package com.example.liquidprismclock.model

/**
 * A prismatic color theme. All fields are packed 32-bit ARGB ints so the pure
 * animation/state layers never depend on Compose.
 */
data class PrismPalette(
    val primary: Int,
    val secondary: Int,
    val accent: Int,
    val background: Int,
    val glow: Int,
) {
    /** Linearly blend every channel of two palettes (0 = [from], 1 = [to]). */
    fun lerp(to: PrismPalette, t: Float): PrismPalette = PrismPalette(
        primary = Argb.mix(primary, to.primary, t),
        secondary = Argb.mix(secondary, to.secondary, t),
        accent = Argb.mix(accent, to.accent, t),
        background = Argb.mix(background, to.background, t),
        glow = Argb.mix(glow, to.glow, t),
    )
}

/**
 * The four automatic time-of-day palettes. Colors are intentionally soft and
 * OLED-friendly: backgrounds stay near-black, accents stay muted (no neon).
 */
object PrismPalettes {
    val MORNING = PrismPalette(
        primary = 0xFFFFA3B8.toInt(),   // warm pink
        secondary = 0xFFFFC88F.toInt(), // peach
        accent = 0xFFFDE4A1.toInt(),    // soft yellow
        background = 0xFF140A24.toInt(),// dark purple
        glow = 0xFFFF7EB6.toInt(),
    )

    val AFTERNOON = PrismPalette(
        primary = 0xFF6FE7FF.toInt(),   // cyan
        secondary = 0xFF5B8CFF.toInt(), // blue
        accent = 0xFF8DF0A8.toInt(),    // green
        background = 0xFF050B1E.toInt(),// deep blue
        glow = 0xFF4FC3F7.toInt(),
    )

    val EVENING = PrismPalette(
        primary = 0xFFFF9E6B.toInt(),   // orange
        secondary = 0xFFFF7AC8.toInt(), // pink
        accent = 0xFFB58CFF.toInt(),    // purple
        background = 0xFF160826.toInt(),// dark violet
        glow = 0xFFFF6FA8.toInt(),
    )

    val NIGHT = PrismPalette(
        primary = 0xFF9D8CFF.toInt(),   // violet
        secondary = 0xFF6B8CFF.toInt(), // blue
        accent = 0xFFFF8FA8.toInt(),    // subtle red
        background = 0xFF04030A.toInt(),// almost black
        glow = 0xFF7A6CFF.toInt(),
    )

    /** Order used for both display and interpolation. */
    val ALL: List<Pair<ThemeMode, PrismPalette>> = listOf(
        ThemeMode.MORNING to MORNING,
        ThemeMode.AFTERNOON to AFTERNOON,
        ThemeMode.EVENING to EVENING,
        ThemeMode.NIGHT to NIGHT,
    )

    fun of(mode: ThemeMode): PrismPalette = when (mode) {
        ThemeMode.MORNING -> MORNING
        ThemeMode.AFTERNOON -> AFTERNOON
        ThemeMode.EVENING -> EVENING
        ThemeMode.NIGHT -> NIGHT
        ThemeMode.AUTO -> NIGHT // resolved dynamically, never used directly
    }

    /**
     * Anchor hours (fractional, within 0..24) for each theme.
     *
     * Themes are anchored at the *middle* of their time-of-day window so the
     * broad 2h transition bands centered on boundaries stay smooth and never
     * hard-snap at 06:00 / 12:00 / etc.
     */
    private const val H_MORNING = 9.0f     // midpoint of 06:00–12:00
    private const val H_AFTERNOON = 14.5f  // midpoint of 12:00–17:00
    private const val H_EVENING = 19.0f    // midpoint of 17:00–21:00
    private const val H_NIGHT = 2.5f       // midpoint of 21:00–06:00 (wraps midnight)

    /** Half-width of the cross-fade between adjacent themes, in hours. */
    private const val TRANSITION_HOURS = 1.0f

    /**
     * Smoothly interpolated palette for a time-of-day fraction.
     *
     * [hourFloat] is `hour + minute/60 + second/3600`, wrapped to 0..24.
     * Between anchors the palette cross-fades linearly; each anchor holds its
     * theme at full strength near its center.
     */
    fun automatic(hourFloat: Float): PrismPalette {
        // Build a cyclic list of (anchorHourWrapped, palette), with night
        // duplicated on both sides of midnight so interpolation wraps cleanly.
        val anchors = listOf(
            H_NIGHT to NIGHT,
            H_MORNING to MORNING,
            H_AFTERNOON to AFTERNOON,
            H_EVENING to EVENING,
            H_NIGHT + 24f to NIGHT,
        )
        val t = if (hourFloat < H_NIGHT) hourFloat + 24f else hourFloat
        for (i in 0 until anchors.size - 1) {
            val (h0, p0) = anchors[i]
            val (h1, p1) = anchors[i + 1]
            if (t >= h0 && t <= h1) {
                val span = h1 - h0
                val x = (t - h0) / span
                // Steepened smoothstep-ish curve: stays near each anchor longer,
                // fades through the midpoint.
                val eased = smoothTransition(x)
                return p0.lerp(p1, eased)
            }
        }
        return NIGHT
    }

    /**
     * Map segment position [x] (0 at anchor A, 1 at anchor B) to a blend factor
     * that holds anchor A near x=0, anchor B near x=1, and cross-fades through a
     * [TRANSITION_HOURS]-wide window centered on the boundary (x = 0.5).
     */
    private fun smoothTransition(x: Float): Float {
        // Convert the hourly transition half-width to segment fraction using the
        // smallest anchor spacing (4.5h afternoon→evening) so bands never overlap.
        val minSpacing = 4.5f
        val halfBandFraction = (TRANSITION_HOURS / minSpacing).coerceIn(0.05f, 0.45f)
        return when {
            x <= 0.5f - halfBandFraction -> 0f
            x >= 0.5f + halfBandFraction -> 1f
            else -> {
                val y = (x - (0.5f - halfBandFraction)) / (2f * halfBandFraction)
                y * y * (3f - 2f * y) // smoothstep
            }
        }
    }
}
