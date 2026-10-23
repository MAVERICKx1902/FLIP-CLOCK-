package com.example.liquidprismclock.model

import kotlin.math.max
import kotlin.math.min

/**
 * Minimal packed-ARGB color helpers.
 *
 * Colors are kept as plain 32-bit ARGB ints throughout the pure logic layer so
 * that [PrismPalette], the particle system and the animation math have no
 * Compose or android.graphics dependency. The Compose UI converts with
 * `Color(argb)` (Long conversion happens inside Compose).
 */
object Argb {

    const val BLACK: Int = 0xFF000000.toInt()
    const val WHITE: Int = 0xFFFFFFFF.toInt()

    fun alpha(argb: Int): Float = ((argb ushr 24) and 0xFF) / 255f
    fun red(argb: Int): Float = ((argb ushr 16) and 0xFF) / 255f
    fun green(argb: Int): Float = ((argb ushr 8) and 0xFF) / 255f
    fun blue(argb: Int): Float = (argb and 0xFF) / 255f

    fun rgb(r: Float, g: Float, b: Float, a: Float = 1f): Int =
        ((clamp01(a) * 255f).toInt() shl 24) or
            ((clamp01(r) * 255f).toInt() shl 16) or
            ((clamp01(g) * 255f).toInt() shl 8) or
            (clamp01(b) * 255f).toInt()

    /** Copy an ARGB color replacing its alpha channel. */
    fun withAlpha(argb: Int, alpha: Float): Int {
        val a = (clamp01(alpha) * 255f).toInt() shl 24
        return (argb and 0x00FFFFFF) or a
    }

    /** Scale a color toward white by [t] (0 = unchanged, 1 = white). */
    fun lighten(argb: Int, t: Float): Int = mix(argb, WHITE, t)

    /** Linear interpolation between two packed ARGB colors. */
    fun mix(from: Int, to: Int, t: Float): Int {
        val x = clamp01(t)
        val aa = ((alpha(from) * (1f - x) + alpha(to) * x) * 255f).toInt()
        val rr = ((red(from) * (1f - x) + red(to) * x) * 255f).toInt()
        val gg = ((green(from) * (1f - x) + green(to) * x) * 255f).toInt()
        val bb = ((blue(from) * (1f - x) + blue(to) * x) * 255f).toInt()
        return (aa shl 24) or (rr shl 16) or (gg shl 8) or bb
    }

    /** 0..1 clamp used everywhere in the animation math. */
    fun clamp01(v: Float): Float = max(0f, min(1f, v))
}
