package com.example.liquidprismclock.ui.components

import androidx.compose.ui.graphics.nativeCanvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.example.liquidprismclock.model.Argb
import com.example.liquidprismclock.model.PrismPalette
import com.example.liquidprismclock.model.TimeData

/**
 * The hero clock: six individual [LiquidDigit] cards + colon separators.
 *
 * Digit card size is computed from the available constraints (never hard
 * coded), so the clock scales across phones, tablets, portrait and landscape.
 * Only the slot whose glyph changed starts a flip; everything else stays at
 * rest.
 *
 * Seconds can be hidden (HH:MM) and 12-hour mode omits the hour-tens card so
 * the layout stays centered.
 */
@Composable
fun FlipClock(
    time: TimeData,
    use24Hour: Boolean,
    showSeconds: Boolean,
    palette: PrismPalette,
    frame: FrameState,
    glowScale: Float,
    animationScale: Float,
    states: Array<DigitState>,
    onBurst: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hourChars = (if (use24Hour) time.hour24 else time.hourOfAmPm)
        .toString().padStart(2, '0')
    val minuteChars = time.minute.toString().padStart(2, '0')
    val secondChars = time.second.toString().padStart(2, '0')
    val seq = hourChars + minuteChars + secondChars

    // Wire glyph changes → flip start + particle burst. Timestamped against
    // the shared frame clock so the flip begins on the vsync the change shows.
    LaunchedEffect(time, use24Hour) {
        for (slot in 0 until 6) {
            val ch = seq[slot]
            val st = states[slot]
            if (st.current != ch) {
                st.from = st.current
                st.current = ch
                st.startNanos = frame.frameNanos
                if (st.centerX > 0f) onBurst(st.centerX, st.centerY)
            }
        }
    }

    val showHourTens = use24Hour || hourChars[0] != '0'

    BoxWithConstraints(
        modifier.semantics(mergeDescendants = true) {
            contentDescription = time.spokenTime(use24Hour)
        },
        contentAlignment = Alignment.Center,
    ) {
        val density = LocalDensity.current
        val maxWpx = with(density) { maxWidth.toPx() }
        val maxHpx = with(density) { maxHeight.toPx() }

        // Adaptive sizing from available space (no hard-coded 60×90dp).
        val visibleDigits = (if (showSeconds) 6 else 4) - if (showHourTens) 0 else 1
        val visibleColons = if (showSeconds) 2 else 1
        val unit = maxWpx / (visibleDigits * 0.74f + visibleColons * 0.26f + (if (use24Hour) 0f else 0.7f))
        val cardW = unit * 0.68f
        val cardH = (cardW * 1.36f).coerceAtMost(maxHpx * 0.94f)
        val cardWDp = with(density) { cardW.toDp() }
        val cardHDp = with(density) { cardH.toDp() }
        val cardPad = cardWDp * 0.045f
        val colonW = cardWDp * 0.34f

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (showHourTens) {
                DigitCard(states, 0, frame, palette, glowScale, animationScale, cardWDp, cardHDp, cardPad)
            }
            DigitCard(states, 1, frame, palette, glowScale, animationScale, cardWDp, cardHDp, cardPad)

            Colon(palette, frame, colonW, cardHDp)

            DigitCard(states, 2, frame, palette, glowScale, animationScale, cardWDp, cardHDp, cardPad)
            DigitCard(states, 3, frame, palette, glowScale, animationScale, cardWDp, cardHDp, cardPad)

            if (showSeconds) {
                Colon(palette, frame, colonW, cardHDp)
                DigitCard(states, 4, frame, palette, glowScale, animationScale, cardWDp, cardHDp, cardPad)
                DigitCard(states, 5, frame, palette, glowScale, animationScale, cardWDp, cardHDp, cardPad)
            }

            if (!use24Hour) {
                AmPmLabel(time.isPm, palette, cardHDp, Modifier.padding(start = cardWDp * 0.28f))
            }
        }
    }
}

@Composable
private fun DigitCard(
    states: Array<DigitState>,
    slot: Int,
    frame: FrameState,
    palette: PrismPalette,
    glowScale: Float,
    animationScale: Float,
    cardW: Dp,
    cardH: Dp,
    cardPad: Dp,
) {
    LiquidDigit(
        state = states[slot],
        slot = slot,
        allStates = states.asList(),
        frame = frame,
        palette = palette,
        glowScale = glowScale,
        animationScale = animationScale,
        placeholder = false,
        contentDescription = states[slot].current.toString(),
        modifier = Modifier
            .padding(horizontal = cardPad)
            .width(cardW)
            .height(cardH),
    )
}

@Composable
private fun Colon(palette: PrismPalette, frame: FrameState, colonW: Dp, cardH: Dp) {
    Box(Modifier.width(colonW).height(cardH), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val h = size.height
            val cy = h / 2f
            val dotR = h * 0.030f
            val gap = h * 0.16f
            // Gentle breathing pulse tied to the shared clock.
            val pulse = 0.55f + 0.45f * kotlin.math.sin(
                frame.elapsedSeconds * 2f * Math.PI.toFloat()
            )
            val alpha = 0.50f + 0.30f * pulse
            colonPaint.setARGB(
                (alpha * 255f).toInt().coerceIn(0, 255),
                (Argb.red(palette.primary) * 235f + 20f).toInt().coerceIn(0, 255),
                (Argb.green(palette.secondary) * 235f + 20f).toInt().coerceIn(0, 255),
                (Argb.blue(palette.accent) * 235f + 20f).toInt().coerceIn(0, 255),
            )
            drawContext.canvas.nativeCanvas.apply {
                drawCircle(size.width / 2f, cy - gap / 2f, dotR, colonPaint)
                drawCircle(size.width / 2f, cy + gap / 2f, dotR, colonPaint)
            }
        }
    }
}

@Composable
private fun AmPmLabel(isPm: Boolean, palette: PrismPalette, cardH: Dp, modifier: Modifier = Modifier) {
    Box(modifier.height(cardH), contentAlignment = Alignment.Center) {
        Text(
            text = if (isPm) "PM" else "AM",
            color = Color(Argb.withAlpha(palette.secondary, 0.75f)),
            fontSize = (cardH.value * 0.15f).coerceAtMost(26f).sp,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .alpha(0.85f)
                .padding(top = cardH * 0.28f),
        )
    }
}

private val colonPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
