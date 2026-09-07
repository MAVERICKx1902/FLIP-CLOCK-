package com.example.liquidprismclock.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas

/**
 * Developer-only performance overlay.
 *
 * The FPS number is computed from REAL frame deltas (the time between
 * successive vsync callbacks over a rolling window) — it is never a constant
 * "60". The overlay itself only paints a few glyphs and one bar, so it has
 * negligible cost when enabled.
 */
class FpsMeter {
    private val window = FloatArray(60)
    private var idx = 0
    private var filled = 0
    private var fpsSmoothed = 60f

    /** Feed one measured frame delta (seconds). Returns smoothed FPS. */
    fun onFrame(dtSeconds: Float): Float {
        window[idx] = dtSeconds
        idx = (idx + 1) % window.size
        if (filled < window.size) filled++
        var sum = 0f
        var max = 0f
        for (i in 0 until filled) {
            sum += window[i]
            if (window[i] > max) max = window[i]
        }
        // Drop the single worst frame (jank spike) from the average so the
        // readout reflects sustained frame rate.
        val avg = if (filled > 2) (sum - max) / (filled - 1)
        else sum / filled.coerceAtLeast(1)
        val instant = 1f / avg.coerceAtLeast(1f / 240f)
        fpsSmoothed += (instant - fpsSmoothed) * 0.15f
        return fpsSmoothed
    }
}

@Composable
fun rememberFpsMeter(): FpsMeter = remember { FpsMeter() }

/**
 * Draws the FPS readout and optional animation debug info into the top-start
 * corner using cached Paint objects. Pure draw-phase work; all live values are
 * read INSIDE the draw lambda so they update every frame without recomposition.
 */
@Composable
fun PerfOverlay(
    frame: FrameState,
    live: FpsLiveStats,
    showDebug: Boolean,
    particleCountProvider: () -> Int,
    modifier: Modifier = Modifier,
) {
    val textPaint = remember {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 30f
            typeface = android.graphics.Typeface.MONOSPACE
        }
    }
    val barPaint = remember { android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG) }

    Canvas(modifier) {
        // Touching frame state here subscribes this draw to frame updates.
        val frameMillis = frame.frameMillis
        val native = drawContext.canvas.nativeCanvas
        val x = 28f
        var y = 52f

        val fps = live.fps
        val fpsColor = when {
            fps >= 55f -> 0xFF8DF0A8.toInt()
            fps >= 40f -> 0xFFFDE4A1.toInt()
            else -> 0xFFFF8FA8.toInt()
        }
        textPaint.color = fpsColor
        native.drawText("FPS: %d".format(fps.toInt().coerceIn(0, 240)), x, y, textPaint)

        // Frame-time bar vs the 16.7ms (60fps) target.
        val baseY = y + 16f
        val barWidth = 150f
        val targetH = 10f
        barPaint.color = 0x44FFFFFF
        native.drawRect(x, baseY - targetH, x + barWidth, baseY - targetH + 1.5f, barPaint)
        barPaint.color = fpsColor
        val fillW = (frameMillis.toFloat() / 16.7f).coerceIn(0.08f, 1f) * barWidth
        native.drawRect(x, baseY - 14f, x + fillW, baseY, barPaint)
        y = baseY + 30f

        if (showDebug) {
            textPaint.color = 0xCCB8C4E0.toInt()
            textPaint.textSize = 24f
            native.drawText("frame: $frameMillis ms", x, y, textPaint); y += 30f
            native.drawText("particles: ${particleCountProvider()}", x, y, textPaint); y += 30f
            native.drawText("active flips: ${live.activeFlips}", x, y, textPaint)
            textPaint.textSize = 30f
        }
    }
}

/** Live stats object shared between the frame loop and the overlay (no state). */
open class FpsLiveStats {
    @Volatile var fps: Float = 60f
    @Volatile var activeFlips: Int = 0
}
