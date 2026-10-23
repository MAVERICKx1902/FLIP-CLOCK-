package com.example.liquidprismclock.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos

/**
 * Frame-clock-driven animation state.
 *
 * Rather than assuming `delay(16)` means 60fps (it does not — frame pacing
 * depends on the display refresh and Choreographer), every continuous effect
 * is driven by `withFrameNanos`, which is synchronized to the actual display
 * vsync.
 *
 * The elapsed-time fields are snapshot state read inside `DrawScope` lambdas:
 * reading them invalidates only the draw phase, so continuous animation never
 * forces recomposition of the composable tree.
 */
class FrameState internal constructor() {
    var lastFrameNanos: Long = 0L
        internal set
    var elapsedSeconds: Float by mutableFloatStateOf(0f)
        internal set
    /** Wall clock of the latest frame; flips timestamp themselves against it. */
    var frameNanos: Long by mutableLongStateOf(0L)
        internal set
    /** Last frame delta in milliseconds (for the debug overlay). */
    var frameMillis: Long by mutableLongStateOf(16L)
        internal set

    internal fun advance(nanos: Long): Float {
        if (lastFrameNanos == 0L) lastFrameNanos = nanos
        val deltaNanos = nanos - lastFrameNanos
        lastFrameNanos = nanos
        // Clamp jumps (first frame, resume from background) to avoid particle
        // explosions or flipped digits teleporting.
        val dtSeconds = (deltaNanos.coerceIn(1_000_000L, 100_000_000L)) / 1_000_000_000f
        elapsedSeconds += dtSeconds
        frameNanos = nanos
        frameMillis = (deltaNanos / 1_000_000L).coerceIn(1L, 100L)
        return dtSeconds
    }
}

/**
 * Run [onFrame] on every vsync while this composition is active. One loop is
 * shared by the background, particle system and digit flips to avoid multiple
 * Choreographer registrations.
 */
@Composable
fun rememberFrameLoop(onFrame: (frameNanos: Long, dtSeconds: Float) -> Unit): FrameState {
    val state = remember { FrameState() }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { nanos ->
                val dt = state.advance(nanos)
                onFrame(nanos, dt)
            }
        }
    }
    return state
}
