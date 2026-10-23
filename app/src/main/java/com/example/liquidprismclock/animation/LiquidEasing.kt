package com.example.liquidprismclock.animation

import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Easing curves used by the liquid flip.
 *
 * These are deliberately not the generic Material/standard set: the flip needs
 * an elastic, fluid settle (damped spring) and a gooey accelerate/decelerate
 * pair for the fold itself.
 */
object LiquidEasing {

    /** Cubic ease-in: slow start, fast end (old digit falling away). */
    fun easeInCubic(t: Float): Float = t * t * t

    /** Cubic ease-out: fast start, soft landing (new digit rising into view). */
    fun easeOutCubic(t: Float): Float {
        val u = 1f - t
        return 1f - u * u * u
    }

    /** Smoothstep 3t²−2t³. */
    fun smoothStep(t: Float): Float = t * t * (3f - 2f * t)

    /**
     * Under-damped spring settling to 1.0 with a small overshoot — used for the
     * PHASE 3 settle. [t] in 0..1 across the settle window.
     */
    fun elasticSettle(t: Float): Float {
        // Spring parameters chosen for roughly 1.3 overshoots of ~6%.
        val omega = 22f
        val zeta = 0.38f
        val wd = omega * sqrt(1f - zeta * zeta)
        val decay = exp(-zeta * omega * t)
        return 1f - decay * (cos(wd * t) + zeta / sqrt(1f - zeta * zeta) * sin(wd * t))
    }

    /**
     * Damped wobble used for the PHASE 4 ripple: amplitude decays exponentially;
     * the wave itself is a sine over about 1.25 cycles.
     */
    fun rippleWave(t: Float): Float {
        if (t <= 0f || t >= 1f) return 0f
        val decay = exp(-3.2f * t)
        return (decay * sin((t * 1.25f * 2f * Math.PI).toFloat()))
    }

    /**
     * Exponential blob drift parameter — keeps background motion perceptually
     * uniform over slow multi-minute loops.
     */
    fun slowLoop(phase: Float): Float {
        val x = phase - phase.toInt()
        return smoothStep(if (x < 0.5f) x * 2f else 2f - x * 2f)
    }

    /** Natural exponential fade for particles. */
    fun expFade(life: Float): Float {
        if (life >= 1f) return 0f
        return (1f - life).pow(0.75f)
    }
}
