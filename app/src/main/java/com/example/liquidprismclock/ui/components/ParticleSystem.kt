package com.example.liquidprismclock.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.liquidprismclock.animation.ParticlePool
import com.example.liquidprismclock.model.Argb
import com.example.liquidprismclock.model.PrismPalette
import kotlin.math.min
import kotlin.random.Random

/**
 * Lightweight glass-dust particle layer.
 *
 * - Particles live in a fixed-size, pre-allocated [ParticlePool] (no per-frame
 *   allocation).
 * - Ambient dust is a small stable population; digit flips emit short bursts
 *   of "refracted light" fragments near the flipping card.
 * - Count scales with the measured performance tier and the user's particle
 *   intensity setting. On the lowest tier the pool is tiny.
 *
 * Drawing uses an additive PorterDuff blend so particles read as light on the
 * OLED-black background (additive blending can never muddy toward gray).
 */
class ParticleController(
    val pool: ParticlePool,
    private val ambientTarget: Int,
) {
    private val rnd = Random(System.nanoTime())

    /** Ambient particles are seeded lazily so the first frame isn't empty. */
    fun seedAmbient(intensityScale: Float) {
        pool.ensureAmbient(ambientTarget, intensityScale, rnd)
    }

    fun update(dtSeconds: Float, intensityScale: Float, timeSeconds: Float) {
        pool.update(dtSeconds, ambientTarget, intensityScale, rnd, timeSeconds)
    }

    /** Emit a flip burst at pixel coordinates. */
    fun burst(px: Float, py: Float, width: Float, height: Float, count: Int, intensityScale: Float) {
        val nx = (px / width).coerceIn(0f, 1f)
        val ny = (py / height).coerceIn(0f, 1f)
        pool.burst(nx, ny, count, intensityScale, rnd)
    }
}

@Composable
fun rememberParticleController(poolSize: Int, ambientTarget: Int): ParticleController =
    remember(poolSize, ambientTarget) {
        ParticleController(ParticlePool(poolSize), ambientTarget)
    }

/** Reusable paint — never allocated per frame. */
private val particlePaint = android.graphics.Paint().apply {
    isAntiAlias = true
    style = android.graphics.Paint.Style.FILL
    xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.ADD)
}

/** Draw all active particles. Called from within the shared background canvas. */
fun DrawScope.drawParticles(
    controller: ParticleController,
    palette: PrismPalette,
    particleScale: Float,
) {
    if (particleScale <= 0f) return
    val minDim = min(size.width, size.height)
    val pool = controller.pool
    val colors = intArrayOf(palette.primary, palette.secondary, palette.accent)
    val w = size.width
    val h = size.height

    drawContext.canvas.nativeCanvas.apply {
        for (i in 0 until pool.capacity) {
            val p = pool.particles[i]
            if (!p.active) continue
            val c = colors[p.colorIndex]
            val env = fadeAlpha(p.life) * particleScale
            if (env <= 0.01f) continue
            val cx = p.x * w
            val cy = p.y * h
            val radius = p.size * minDim * (0.6f + 0.4f * p.life)
            val r = (Argb.red(c) * 255f).toInt()
            val g = (Argb.green(c) * 255f).toInt()
            val b = (Argb.blue(c) * 255f).toInt()

            // Outer halo (faint, wide).
            particlePaint.setARGB(((0.16f * env).coerceIn(0f, 1f) * 255f).toInt(), r, g, b)
            drawCircle(cx, cy, radius * 3.2f, particlePaint)
            // Core dot.
            particlePaint.setARGB(((0.85f * env).coerceIn(0f, 1f) * 255f).toInt(), r, g, b)
            drawCircle(cx, cy, radius, particlePaint)
        }
    }
}

private fun fadeAlpha(life: Float): Float =
    // Fast fade-in, long soft fade-out.
    if (life > 0.85f) (1f - life) / 0.15f else life / 0.85f
