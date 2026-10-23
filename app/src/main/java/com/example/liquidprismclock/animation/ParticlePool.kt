package com.example.liquidprismclock.animation

import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * One particle's live state. Positions are normalized (0..1 of the canvas)
 * so the pool never reallocates on resize; the drawer maps to pixels.
 */
class Particle {
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f
    var size = 0.004f        // fraction of min(width,height)
    var life = 1f            // 1 born → 0 dead
    var decay = 0.004f       // per frame at 60fps (scaled by dt)
    var colorIndex = 0       // 0 primary, 1 secondary, 2 accent
    var driftSeed = 0f
    var active = false

    fun reset(
        x: Float, y: Float, vx: Float, vy: Float, size: Float,
        life: Float, decay: Float, colorIndex: Int, seed: Float,
    ) {
        this.x = x; this.y = y; this.vx = vx; this.vy = vy
        this.size = size; this.life = life; this.decay = decay
        this.colorIndex = colorIndex; this.driftSeed = seed; this.active = true
    }
}

/**
 * Lightweight, allocation-free particle pool.
 *
 * Particles are pre-allocated; [burst] reuses dead slots. Ambient particles
 * are a small stable population; flipping emits a short "glass fragment"
 * burst near the flipping digit.
 */
class ParticlePool(val capacity: Int) {

    val particles = Array(capacity) { Particle() }
    private var cursor = 0

    /** Number of currently active particles (for the debug overlay). */
    var activeCount = 0
        private set

    private var ambientSeeded = false

    fun ensureAmbient(ambientTarget: Int, intensity: Float, rnd: Random) {
        if (!ambientSeeded) {
            var placed = 0
            for (i in particles.indices) {
                if (placed >= ambientTarget) break
                val p = particles[i]
                if (!p.active) {
                    spawnAmbient(p, rnd, intensity, initial = true)
                    placed++
                }
            }
            ambientSeeded = true
            recount()
        }
    }

    /** Emit [count] fragments around normalized position (x,y). */
    fun burst(x: Float, y: Float, count: Int, intensity: Float, rnd: Random) {
        var emitted = 0
        for (n in 0 until capacity) {
            if (emitted >= count) break
            val idx = (cursor + n) % capacity
            val p = particles[idx]
            if (!p.active) {
                cursor = (idx + 1) % capacity
                val angle = rnd.nextFloat() * Math.PI.toFloat() * 2f
                val speed = (0.02f + rnd.nextFloat() * 0.09f) * (0.4f + intensity)
                p.reset(
                    x = x + (rnd.nextFloat() - 0.5f) * 0.04f,
                    y = y + (rnd.nextFloat() - 0.5f) * 0.08f,
                    vx = cos(angle) * speed * 0.6f,
                    vy = sin(angle) * speed - 0.05f * intensity, // slight upward shimmer
                    size = 0.002f + rnd.nextFloat() * 0.007f,
                    life = 1f,
                    decay = 0.006f + rnd.nextFloat() * 0.012f,
                    colorIndex = rnd.nextInt(3),
                    seed = rnd.nextFloat() * 100f,
                )
                emitted++
            }
        }
        recount()
    }

    private fun spawnAmbient(p: Particle, rnd: Random, intensity: Float, initial: Boolean) {
        p.reset(
            x = rnd.nextFloat(),
            y = if (initial) rnd.nextFloat() else 1.05f,
            vx = (rnd.nextFloat() - 0.5f) * 0.004f,
            vy = -(0.004f + rnd.nextFloat() * 0.010f) * (0.5f + intensity),
            size = 0.0015f + rnd.nextFloat() * 0.004f,
            life = if (initial) rnd.nextFloat() else 1f,
            decay = 0.0008f + rnd.nextFloat() * 0.0015f,
            colorIndex = rnd.nextInt(3),
            seed = rnd.nextFloat() * 100f,
        )
    }

    /**
     * Advance the simulation. [dtSeconds] is real elapsed time; motion is
     * normalized to 60fps so particle speed is frame-rate independent.
     */
    fun update(dtSeconds: Float, ambientTarget: Int, intensity: Float, rnd: Random, time: Float) {
        val frameScale = (dtSeconds * 60f).coerceIn(0.25f, 3f)
        var active = 0
        var ambient = 0
        for (p in particles) {
            if (!p.active) continue
            // Gentle sinusoidal drift gives glass-dust floatiness.
            val drift = sin(time * 0.6f + p.driftSeed) * 0.0009f
            p.x += (p.vx + drift) * frameScale
            p.y += p.vy * frameScale
            p.vy += 0.00015f * frameScale            // very slight gravity
            p.vx *= (1f - 0.01f * frameScale)        // drag
            p.life -= p.decay * frameScale
            if (p.life <= 0f || p.y < -0.05f || p.y > 1.08f) {
                p.active = false
            } else {
                active++
                // Ambient particles drift upward (negative vy); bursts can fall.
                if (p.vy < 0f) ambient++
            }
        }
        // Replenish ambient dust to keep a calm steady state.
        if (ambient < ambientTarget && rnd.nextInt(3) == 0) {
            for (p in particles) {
                if (!p.active) {
                    spawnAmbient(p, rnd, intensity, initial = false)
                    active++
                    break
                }
            }
        }
        activeCount = active
    }

    private fun recount() {
        var c = 0
        for (p in particles) if (p.active) c++
        activeCount = c
    }
}
