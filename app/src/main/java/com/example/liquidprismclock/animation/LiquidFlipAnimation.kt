package com.example.liquidprismclock.animation

import com.example.liquidprismclock.model.Argb
import kotlin.math.abs

/**
 * Frame-ready transform data for one flipping digit.
 *
 * The class is a plain data holder (no Android/Compose imports) so the phase
 * math can be unit tested on the JVM. Drawing code maps these fields onto
 * Canvas transforms.
 */
data class FlipTransform(
    /** 0..1 overall animation progress. */
    val progress: Float,
    /** Old digit: translateY as fraction of digit height (positive = downward). */
    val oldTranslateY: Float,
    /** Old digit: horizontal fold skew. */
    val oldSkewX: Float,
    /** Old digit: vertical scale used for the liquid stretch. */
    val oldScaleY: Float,
    /** Old digit: horizontal scale (pinch as it folds). */
    val oldScaleX: Float,
    /** Old digit: 1 fully visible → 0 gone. */
    val oldAlpha: Float,
    /** New digit: translateY as fraction of digit height (negative = rising). */
    val newTranslateY: Float,
    val newSkewX: Float,
    val newScaleY: Float,
    val newScaleX: Float,
    /** New digit: 0 hidden → 1 settled. */
    val newAlpha: Float,
    /** 0..1 chromatic aberration intensity. Peaks mid-flip, zero when settled. */
    val chromaticAberration: Float,
    /** 0..1 extra glow during pre-flip and settle. */
    val glowPulse: Float,
    /** 0..1 liquid trail strength behind the old digit. */
    val trail: Float,
    /** 0..1 elastic settle offset around y (applied to the whole card). */
    val settleWobble: Float,
) {
    companion object {
        val IDLE = FlipTransform(
            progress = 0f,
            oldTranslateY = 0f, oldSkewX = 0f, oldScaleY = 1f, oldScaleX = 1f, oldAlpha = 1f,
            newTranslateY = 0f, newSkewX = 0f, newScaleY = 1f, newScaleX = 1f, newAlpha = 1f,
            chromaticAberration = 0f, glowPulse = 0f, trail = 0f, settleWobble = 0f,
        )
    }
}

/**
 * The liquid flip timeline, expressed as fixed-duration windows (ms).
 *
 *  0–100   PRE-FLIP    glow/scale build
 *  100–400 LIQUID FLIP old digit folds down, new digit rises
 *  400–500 SETTLE      damped spring
 *  500–650 RIPPLE      neighbor ripple only — this digit is already at rest
 */
object LiquidFlipAnimation {

    const val TOTAL_MS = 650
    const val PRE_FLIP_MS = 100
    const val FLIP_END_MS = 400
    const val SETTLE_END_MS = 500
    // 500..650 belongs to the ripple that propagates to neighboring digits.

    /** Peak (fraction of digit height) vertical travel for the folding digit. */
    private const val PEAK_TRAVEL = 0.92f
    /** Max vertical stretch during the fold. */
    private const val STRETCH = 0.42f
    /** Max skew (shear) at the middle of the fold. */
    private const val PEAK_SKEW = 0.16f

    /**
     * Evaluate the flip transform at [elapsedMs] since the digit changed.
     *
     * [intensity] 0..1 scales motion range so users with reduced-motion or low
     * performance settings get a calmer (but still present) animation.
     */
    fun evaluate(elapsedMs: Long, intensity: Float = 1f): FlipTransform {
        val tMs = elapsedMs.coerceIn(0L, TOTAL_MS.toLong())
        val t = tMs / TOTAL_MS.toFloat()
        val strength = Argb.clamp01(intensity)
        if (tMs >= TOTAL_MS) return FlipTransform.IDLE

        // ---- Phase windows as local 0..1 parameters ----
        val pre = tMs.toFloat() / PRE_FLIP_MS
        val flip = ((tMs - PRE_FLIP_MS).coerceAtLeast(0L)).toFloat() /
            (FLIP_END_MS - PRE_FLIP_MS)
        val settle = ((tMs - FLIP_END_MS).coerceAtLeast(0L)).toFloat() /
            (SETTLE_END_MS - FLIP_END_MS)

        // PHASE 1 — pre-flip: swell + glow + hint of aberration.
        val preIn = LiquidEasing.smoothStep(pre.coerceIn(0f, 1f))
        val swell = preIn * 0.045f * strength
        val preGlow = preIn * 0.5f * strength
        val preAberration = preIn * 0.25f * strength

        // PHASE 2 — liquid flip.
        val f = flip.coerceIn(0f, 1f)
        val fall = LiquidEasing.easeInCubic(f)          // old: accelerating drop
        val rise = LiquidEasing.easeOutCubic(f)         // new: decelerating rise
        val midBell = sinBell(f)                        // peaks at f=0.5
        val foldOut = fall
        val foldIn = rise

        val oldAlpha = (1f - fall).coerceIn(0f, 1f)
        val oldY = (PEAK_TRAVEL * foldOut) * strength
        val oldStretch = (1f + STRETCH * midBell) * (0.55f + 0.45f * strength)
        val oldPinch = 1f - 0.12f * midBell * strength
        val oldSkew = PEAK_SKEW * midBell * strength
        val trail = midBell * 0.55f * strength

        // New digit starts above (hidden behind card top), rises in.
        val newAlpha = rise.coerceIn(0f, 1f)
        val newY = (-PEAK_TRAVEL * (1f - rise)) * strength
        val newStretch = 1f + STRETCH * 0.7f * midBell
        val newPinch = 1f - 0.10f * midBell * strength
        val newSkew = -PEAK_SKEW * 0.8f * midBell * strength

        // Chromatic aberration: ramps up through pre-flip, peaks at f≈0.45,
        // released during settle.
        val aberration = when {
            tMs < PRE_FLIP_MS -> preAberration
            tMs < FLIP_END_MS -> (preAberration + (1f - abs(0.5f - f) * 2f) * 0.75f) * strength
            else -> (1f - settle) * 0.3f * strength
        }.coerceIn(0f, 1f)

        // PHASE 3 — elastic settle (whole card). Damped spring settles to 0.
        val settleWobble = if (tMs >= FLIP_END_MS && tMs < SETTLE_END_MS) {
            // Spring around 1 → map into a small vertical oscillation.
            (LiquidEasing.elasticSettle(settle) - settle) * 0.05f * strength
        } else if (tMs < FLIP_END_MS) {
            // tiny breathing swell during flip
            midBell * 0.02f * strength
        } else 0f

        val glow = when {
            tMs < PRE_FLIP_MS -> preGlow
            tMs < FLIP_END_MS -> preGlow * (1f - f * 0.4f) + midBell * 0.35f * strength
            tMs < SETTLE_END_MS -> 0.35f * (1f - settle) * strength
            else -> 0f
        }.coerceIn(0f, 1f)

        return FlipTransform(
            progress = t,
            oldTranslateY = oldY,
            oldSkewX = oldSkew,
            oldScaleY = if (tMs < PRE_FLIP_MS) 1f + swell else oldStretch,
            oldScaleX = if (tMs < PRE_FLIP_MS) 1f + swell else oldPinch,
            oldAlpha = if (tMs < PRE_FLIP_MS) 1f else oldAlpha,
            newTranslateY = if (tMs < PRE_FLIP_MS) 0f else newY,
            newSkewX = if (tMs < PRE_FLIP_MS) 0f else newSkew,
            newScaleY = if (tMs < PRE_FLIP_MS) 1f + swell else newStretch,
            newScaleX = if (tMs < PRE_FLIP_MS) 1f + swell else newPinch,
            newAlpha = if (tMs < PRE_FLIP_MS) 0f else newAlpha,
            chromaticAberration = aberration,
            glowPulse = glow,
            trail = if (tMs >= PRE_FLIP_MS && tMs <= FLIP_END_MS) trail else 0f,
            settleWobble = settleWobble,
        )
    }

    /**
     * Ripple contribution for a digit adjacent to a flipping one.
     * [distance] is slot distance (1 or 2). Returns scale/alpha bump in -1..1.
     */
    fun rippleAt(elapsedMs: Long, distance: Int): Float {
        if (distance <= 0 || distance > 2) return 0f
        val start = SETTLE_END_MS + 30L * distance
        val end = TOTAL_MS.toLong()
        if (elapsedMs < start || elapsedMs > end) return 0f
        val local = (elapsedMs - start).toFloat() / (end - start)
        val attenuation = if (distance == 1) 1f else 0.45f
        return LiquidEasing.rippleWave(local) * 0.045f * attenuation
    }

    private fun sinBell(f: Float): Float {
        // 0 at f=0 and f=1, 1 at f=0.5
        val x = (f.coerceIn(0f, 1f))
        return kotlin.math.sin(x * Math.PI.toFloat())
    }
}
