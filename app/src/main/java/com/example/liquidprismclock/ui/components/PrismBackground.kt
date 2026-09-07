package com.example.liquidprismclock.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import com.example.liquidprismclock.model.Argb
import com.example.liquidprismclock.model.PerformanceTier
import com.example.liquidprismclock.model.PrismPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.min
import kotlin.random.Random

/**
 * The ambient OLED background:
 *  - near-black base in the active palette
 *  - a few huge, very slow prismatic gradient blobs drifting on independent
 *    Lissajous orbits (each has its own frequency/phase, so the motion never
 *    visibly repeats)
 *  - a soft central radial glow so the clock floats in a light "pool"
 *  - cached film grain, skipped on low-tier devices
 *
 * The layer is deliberately dark: most of the screen stays near black.
 * All [android.graphics.Paint]/shader objects are remembered; only shader
 * radii/centers are updated per frame, so no per-frame allocation happens.
 */
@Composable
fun PrismBackground(
    palette: PrismPalette,
    frame: FrameState,
    particleController: ParticleController,
    tier: PerformanceTier,
    particleScale: Float,
    modifier: Modifier = Modifier,
) {
    // A single cached 128x128 noise tile, generated once (deterministic).
    val grain = remember(tier) {
        if (tier.drawGrain) generateGrainTile() else null
    }
    val blobPaint = remember { android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG) }
    val grainPaint = remember { android.graphics.Paint() }
    // Reused shader container — reassigned with new position/radius each frame.
    var shader: android.graphics.RadialGradient? = null

    Canvas(modifier) {
        val t = frame.elapsedSeconds
        val w = size.width
        val h = size.height
        val native = drawContext.canvas.nativeCanvas

        // Base fill.
        native.drawColor(palette.background)

        // Prismatic blobs.
        val blobColors = intArrayOf(palette.primary, palette.secondary, palette.accent)
        val blobCount = tier.blobCount
        for (i in 0 until blobCount) {
            // Slow Lissajous drift; frequencies are incommensurate on purpose.
            val fx = 0.011f + i * 0.0047f
            val fy = 0.009f + i * 0.0031f
            val px = i * 2.1f
            val py = i * 1.7f + 0.6f
            val ox = sin(t * fx * PI.toFloat() + px) * 0.30f
            val oy = cos(t * fy * PI.toFloat() + py) * 0.26f
            val cx = w * (0.5f + ox)
            val cy = h * (0.46f + oy)
            val radius = min(w, h) * (0.55f + 0.06f * sin(t * 0.05f + i))
            val col = blobColors[i % blobColors.size]
            val peakAlpha = 0.09f + 0.025f * (i % 2) // very subtle
            shader = android.graphics.RadialGradient(
                cx, cy, radius.coerceAtLeast(1f),
                intArrayOf(Argb.withAlpha(col, peakAlpha), Argb.withAlpha(col, 0f)),
                floatArrayOf(0f, 1f),
                android.graphics.Shader.TileMode.CLAMP,
            )
            blobPaint.shader = shader
            native.drawRect(0f, 0f, w, h, blobPaint)
        }
        blobPaint.shader = null

        // Central light pool behind the clock.
        val glowR = min(w, h) * 0.85f
        shader = android.graphics.RadialGradient(
            w / 2f, h / 2f, glowR,
            intArrayOf(Argb.withAlpha(palette.glow, 0.10f), Argb.withAlpha(palette.glow, 0f)),
            floatArrayOf(0f, 1f),
            android.graphics.Shader.TileMode.CLAMP,
        )
        blobPaint.shader = shader
        native.drawRect(0f, 0f, w, h, blobPaint)
        blobPaint.shader = null

        // Particles (additive light fragments).
        drawParticles(particleController, palette, particleScale)

        // Film grain, tiled from the cached bitmap.
        val tile = grain
        if (tile != null) {
            grainPaint.shader = android.graphics.BitmapShader(
                tile,
                android.graphics.Shader.TileMode.REPEAT,
                android.graphics.Shader.TileMode.REPEAT,
            )
            grainPaint.alpha = 14
            native.drawRect(0f, 0f, w, h, grainPaint)
            grainPaint.shader = null
        }
    }
}

private const val NOISE_TILE = 128

private fun generateGrainTile(): android.graphics.Bitmap {
    val bmp = android.graphics.Bitmap.createBitmap(
        NOISE_TILE, NOISE_TILE, android.graphics.Bitmap.Config.ARGB_8888
    )
    val rnd = Random(0xC10CC0L) // deterministic: same tile every launch
    val px = IntArray(NOISE_TILE * NOISE_TILE)
    for (i in px.indices) {
        val v = rnd.nextInt(255)
        px[i] = (0x0F shl 24) or (v shl 16) or (v shl 8) or v
    }
    bmp.setPixels(px, 0, NOISE_TILE, 0, 0, NOISE_TILE, NOISE_TILE)
    return bmp
}
