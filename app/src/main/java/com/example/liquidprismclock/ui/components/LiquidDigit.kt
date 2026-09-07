package com.example.liquidprismclock.ui.components

import androidx.compose.ui.graphics.nativeCanvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import com.example.liquidprismclock.animation.FlipTransform
import com.example.liquidprismclock.animation.LiquidFlipAnimation
import com.example.liquidprismclock.model.Argb
import com.example.liquidprismclock.model.PrismPalette
import kotlin.math.abs
import kotlin.math.min

/**
 * Mutable per-slot flip state, held by [FlipClock] (one entry per digit).
 *
 * When the slot's glyph changes, [from] snapshots the old glyph and [startNanos]
 * records the frame time. The drawing code then evaluates [LiquidFlipAnimation]
 * against the shared [FrameState] clock each vsync.
 */
class DigitState(
    var current: Char = '0',
    var from: Char = '0',
    var startNanos: Long = 0L,
    var centerX: Float = 0f,
    var centerY: Float = 0f,
)

/**
 * A single liquid-glass digit card.
 *
 * One [LiquidDigit] exists per clock slot (hour tens … second ones), so when a
 * digit changes only that card animates — the rest of the clock stays at rest.
 *
 * The flip is a stylized *liquid fold* rather than a mechanical airport flap:
 *  - PHASE 1 (0–100ms): glow swell + chromatic hint
 *  - PHASE 2 (100–400ms): old glyph folds down, stretches, skews and fades,
 *    leaving fading glass-trail ghosts; new glyph rises from the card top
 *  - PHASE 3 (400–500ms): elastic settle of the whole card
 *  - PHASE 4 (500–650ms): a damped ripple passes through neighboring cards
 *
 * Everything is drawn in a single Canvas with a handful of reused Paint
 * objects; the only per-frame input is the shared [FrameState] clock, which
 * invalidates the draw phase only (no recomposition while animating).
 */
@Composable
fun LiquidDigit(
    state: DigitState,
    slot: Int,
    allStates: List<DigitState>,
    frame: FrameState,
    palette: PrismPalette,
    glowScale: Float,
    animationScale: Float,
    placeholder: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onPositioned: (IntOffset) -> Unit = {},
) {
    val typeface = remember {
        android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL)
    }

    Box(
        modifier
            .semantics { this.contentDescription = contentDescription }
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                state.centerX = pos.x + coords.size.width / 2f
                state.centerY = pos.y + coords.size.height / 2f
                onPositioned(pos.round())
            }
    ) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val native = drawContext.canvas.nativeCanvas
            val minDim = min(w, h)
            val corner = minDim * 0.14f

            // ---- Timing -----------------------------------------------------
            val nowNanos = frame.frameNanos
            val ownStart = state.startNanos
            val elapsedMs = if (ownStart > 0L) {
                ((nowNanos - ownStart) / 1_000_000L).coerceAtLeast(0L)
            } else Long.MAX_VALUE
            val flipping = elapsedMs in 0L until LiquidFlipAnimation.TOTAL_MS.toLong()

            // Ripple from the nearest flipping neighbor.
            var ripple = 0f
            allStates.forEachIndexed { otherSlot, other ->
                if (otherSlot == slot) return@forEachIndexed
                val neighborElapsed = ((nowNanos - other.startNanos) / 1_000_000L).coerceAtLeast(0L)
                val r = LiquidFlipAnimation.rippleAt(neighborElapsed, abs(otherSlot - slot))
                if (abs(r) > abs(ripple)) ripple = r
            }

            val tx = if (flipping) LiquidFlipAnimation.evaluate(elapsedMs, animationScale)
            else FlipTransform.IDLE

            val cardPaint = DigitPaints.cardPaint
            val fillPaint = DigitPaints.fillPaint
            val strokePaint = DigitPaints.strokePaint
            val glyphPaint = DigitPaints.glyphPaint.apply {
                this.typeface = typeface
                textSize = h * 0.62f
                textAlign = android.graphics.Paint.Align.CENTER
            }

            // ---- Card geometry ----------------------------------------------
            val cardLeft = w * 0.06f
            val cardTop = h * 0.05f
            val cardRight = w * 0.94f
            val cardBottom = h * 0.95f
            val cardW = cardRight - cardLeft
            val cardH = cardBottom - cardTop

            DigitPaints.cardPath.reset()
            DigitPaints.cardPath.addRoundRect(
                cardLeft, cardTop, cardRight, cardBottom,
                corner, corner,
                android.graphics.Path.Direction.CW,
            )

            // Whole-card wobble: elastic settle (own) + neighbor ripple.
            val wobbleY = (if (flipping) tx.settleWobble else 0f) + ripple
            native.save()
            if (abs(wobbleY) > 0.0001f) native.translate(0f, wobbleY * h)
            val cardScale = 1f + abs(ripple) * 0.5f
            if (cardScale != 1f) native.scale(cardScale, cardScale, w / 2f, h / 2f)

            // Soft outer prism glow, pulsing through the flip. Tinted via a
            // cached SRC_IN color filter so the reusable unit-space white
            // radial shader never has to be reallocated per frame.
            val glowStrength = 0.12f * glowScale + tx.glowPulse * 0.22f
            if (!placeholder && glowStrength > 0.01f) {
                DigitPaints.placeRadial(w / 2f, h / 2f, cardW * 0.95f)
                cardPaint.shader = DigitPaints.glowShader
                cardPaint.colorFilter = DigitPaints.tintFilter(palette.glow)
                cardPaint.alpha = (glowStrength.coerceIn(0f, 1f) * 255f).toInt()
                native.drawRect(0f, 0f, w, h, cardPaint)
                cardPaint.shader = null
                cardPaint.colorFilter = null
                cardPaint.alpha = 255
            }

            // Glass body: flat dark translucent fill with a faint prism tint
            // (no shader — avoids per-frame allocation).
            val bodyTint = Argb.mix(0xFF0B0E18.toInt(), palette.primary, 0.12f)
            fillPaint.shader = null
            fillPaint.color = Argb.withAlpha(bodyTint, 0.66f)
            native.drawPath(DigitPaints.cardPath, fillPaint)

            // Top inner highlight pooling light on the glass rim. White unit
            // shader; only alpha is modulated, so no tinting is needed.
            DigitPaints.placeVertical(cardTop, cardTop + cardH * 0.55f)
            fillPaint.shader = DigitPaints.highlightShader
            fillPaint.color = Argb.WHITE
            fillPaint.alpha = ((0.10f + tx.glowPulse * 0.08f) * 255f).toInt().coerceIn(0, 255)
            native.save()
            native.clipPath(DigitPaints.cardPath)
            native.drawRect(cardLeft, cardTop, cardRight, cardTop + cardH * 0.55f, fillPaint)
            native.restore()
            fillPaint.shader = null
            fillPaint.alpha = 255

            // Prismatic edge: a thin horizontal light line along the card
            // bottom, drawn as three short segments tinted with prism colors.
            strokePaint.style = android.graphics.Paint.Style.STROKE
            strokePaint.strokeWidth = minDim * 0.008f
            strokePaint.alpha = ((0.35f + tx.glowPulse * 0.4f) * 255f).toInt().coerceIn(0, 255)
            native.save()
            native.clipPath(DigitPaints.cardPath)
            val edgeY = cardBottom - strokePaint.strokeWidth / 2f
            val edgeStart = cardLeft + corner * 0.4f
            val edgeEnd = cardRight - corner * 0.4f
            val edgeMid = (edgeStart + edgeEnd) / 2f
            strokePaint.color = Argb.withAlpha(palette.primary, 0.55f)
            native.drawLine(edgeStart, edgeY, edgeMid, edgeY, strokePaint)
            strokePaint.color = Argb.withAlpha(palette.secondary, 0.55f)
            native.drawLine(edgeMid, edgeY, edgeEnd, edgeY, strokePaint)
            // Accent dot at the very center for a prismatic sparkle.
            strokePaint.color = Argb.withAlpha(palette.accent, 0.7f)
            native.drawLine(edgeMid - cardW * 0.08f, edgeY, edgeMid + cardW * 0.08f, edgeY, strokePaint)
            native.restore()

            // Hairline glass border.
            strokePaint.color = Argb.withAlpha(Argb.WHITE, 0.16f + tx.glowPulse * 0.10f)
            strokePaint.strokeWidth = minDim * 0.004f
            native.drawPath(DigitPaints.cardPath, strokePaint)

            // ---- Glyphs ------------------------------------------------------
            if (!placeholder) {
                native.save()
                native.clipPath(DigitPaints.cardPath)
                val baseline = h / 2f - (glyphPaint.ascent() + glyphPaint.descent()) / 2f
                val centerX = w / 2f

                if (!flipping) {
                    drawGlyph(native, glyphPaint, state.current, centerX, baseline, 1f, palette, 0f)
                } else {
                    // Fading glass-trail ghosts following the falling old glyph.
                    if (tx.trail > 0.01f) {
                        for (k in 1..3) {
                            val trailFrac = k / 4f
                            val ty = tx.oldTranslateY * h * (1f - trailFrac)
                            val a = tx.trail * (1f - trailFrac) * 0.16f
                            glyphPaint.color = Argb.withAlpha(palette.secondary, a)
                            glyphPaint.textSize = h * 0.62f
                            glyphPaint.textAlign = android.graphics.Paint.Align.CENTER
                            native.save()
                            native.translate(0f, ty)
                            native.drawText(state.from.toString(), centerX, baseline, glyphPaint)
                            native.restore()
                        }
                    }
                    // Old digit folding downward.
                    drawTransformedGlyph(
                        native, glyphPaint, state.from, centerX, baseline, cardH,
                        topFold = false,
                        ty = tx.oldTranslateY, skew = tx.oldSkewX,
                        scaleX = tx.oldScaleX, scaleY = tx.oldScaleY,
                        alpha = tx.oldAlpha, aberration = tx.chromaticAberration,
                        palette = palette,
                    )
                    // New digit rising upward.
                    drawTransformedGlyph(
                        native, glyphPaint, state.current, centerX, baseline, cardH,
                        topFold = true,
                        ty = tx.newTranslateY, skew = tx.newSkewX,
                        scaleX = tx.newScaleX, scaleY = tx.newScaleY,
                        alpha = tx.newAlpha, aberration = tx.chromaticAberration,
                        palette = palette,
                    )
                }
                native.restore()
            }

            native.restore()
        }
    }
}

/** Draw a glyph with the liquid fold transform and a subtle RGB split. */
private fun drawTransformedGlyph(
    canvas: android.graphics.Canvas,
    paint: android.graphics.Paint,
    glyph: Char,
    cx: Float,
    baseline: Float,
    cardH: Float,
    topFold: Boolean,
    ty: Float,
    skew: Float,
    scaleX: Float,
    scaleY: Float,
    alpha: Float,
    aberration: Float,
    palette: PrismPalette,
) {
    if (alpha <= 0.01f) return
    canvas.save()
    canvas.translate(0f, ty * cardH)
    canvas.skew(skew, 0f)
    // Old digit stretches away from its bottom edge as it falls; the new digit
    // stretches from the top edge as it flows in.
    val pivotY = if (topFold) baseline - cardH * 0.30f else baseline + cardH * 0.30f
    canvas.scale(scaleX, scaleY, cx, pivotY)

    val caOffset = aberration * cardH * 0.025f
    val a = (alpha * 255f).toInt().coerceIn(0, 255)
    val text = glyph.toString()
    paint.textAlign = android.graphics.Paint.Align.CENTER
    if (caOffset > 0.01f) {
        // Red channel pulled left, blue channel pulled right, white core —
        // a very subtle chromatic split that never harms readability.
        paint.setARGB((a * 0.42f).toInt().coerceIn(0, 255), 255, 96, 110)
        canvas.drawText(text, cx - caOffset, baseline, paint)
        paint.setARGB((a * 0.42f).toInt().coerceIn(0, 255), 105, 185, 255)
        canvas.drawText(text, cx + caOffset, baseline, paint)
        paint.setARGB(a, 238, 246, 255)
        canvas.drawText(text, cx, baseline, paint)
    } else {
        // Resting glyph carries a faint prism tint so the glass feels lit.
        paint.setARGB(
            a,
            (Argb.red(palette.primary) * 55f + 238f * 0.78f).toInt().coerceIn(0, 255),
            (Argb.green(palette.secondary) * 40f + 246f * 0.84f).toInt().coerceIn(0, 255),
            (Argb.blue(palette.accent) * 30f + 255f * 0.88f).toInt().coerceIn(0, 255),
        )
        canvas.drawText(text, cx, baseline, paint)
    }
    canvas.restore()
}

private fun drawGlyph(
    canvas: android.graphics.Canvas,
    paint: android.graphics.Paint,
    glyph: Char,
    cx: Float,
    baseline: Float,
    alpha: Float,
    palette: PrismPalette,
    @Suppress("UNUSED_PARAMETER") aberration: Float,
) {
    val a = (alpha * 255f).toInt().coerceIn(0, 255)
    paint.setARGB(
        a,
        (Argb.red(palette.primary) * 55f + 240f * 0.78f).toInt().coerceIn(0, 255),
        (Argb.green(palette.secondary) * 40f + 246f * 0.84f).toInt().coerceIn(0, 255),
        (Argb.blue(palette.accent) * 30f + 255f * 0.88f).toInt().coerceIn(0, 255),
    )
    canvas.drawText(glyph.toString(), cx, baseline, paint)
}

/**
 * Reusable drawing objects, shared by all digit cards.
 *
 * Shaders are created ONCE in unit space (white → transparent / white → white)
 * and re-pointed per card via [android.graphics.Matrix] transforms + paint
 * color tinting. RadialGradient/LinearGradient instances are immutable, so
 * recreating them per frame would allocate; this approach allocates zero
 * shader objects during animation.
 */
private object DigitPaints {
    val cardPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    val fillPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    val strokePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE
    }
    val glyphPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.FILL
    }
    val cardPath = android.graphics.Path()
    val matrix = android.graphics.Matrix()

    // Unit-space radial: center (0,0), radius 1, white core → transparent edge.
    private val glowRadial = android.graphics.RadialGradient(
        0f, 0f, 1f,
        intArrayOf(Argb.WHITE, Argb.withAlpha(Argb.WHITE, 0f)),
        floatArrayOf(0f, 1f),
        android.graphics.Shader.TileMode.CLAMP,
    )

    // Unit-space vertical linear: top white → bottom transparent.
    private val verticalLinear = android.graphics.LinearGradient(
        0f, 0f, 0f, 1f,
        intArrayOf(Argb.WHITE, Argb.withAlpha(Argb.WHITE, 0f)),
        floatArrayOf(0f, 1f),
        android.graphics.Shader.TileMode.CLAMP,
    )

    val glowShader: android.graphics.Shader get() = glowRadial
    val highlightShader: android.graphics.Shader get() = verticalLinear

    // Cached SRC_IN tint filter: lets white unit shaders be tinted to the
    // active palette color without reallocating shaders.
    private var tintFilterColor: Int = 0
    private var cachedTintFilter: android.graphics.PorterDuffColorFilter? = null

    fun tintFilter(argb: Int): android.graphics.PorterDuffColorFilter {
        if (cachedTintFilter == null || tintFilterColor != argb) {
            tintFilterColor = argb
            cachedTintFilter = android.graphics.PorterDuffColorFilter(
                argb, android.graphics.PorterDuff.Mode.SRC_IN
            )
        }
        return cachedTintFilter!!
    }

    /** Position+scale the unit radial at (cx,cy) with the given radius. */
    fun placeRadial(cx: Float, cy: Float, radius: Float) {
        matrix.reset()
        matrix.postScale(radius, radius, 0f, 0f)
        matrix.postTranslate(cx, cy)
        glowRadial.setLocalMatrix(matrix)
    }

    /** Map the unit vertical linear to rect (x,top)–(x2,bottom). */
    fun placeVertical(top: Float, bottom: Float) {
        matrix.reset()
        matrix.postScale(1f, bottom - top, 0f, 0f)
        matrix.postTranslate(0f, top)
        verticalLinear.setLocalMatrix(matrix)
    }
}
