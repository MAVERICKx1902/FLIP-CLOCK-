package com.example.liquidprismclock.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.liquidprismclock.R
import com.example.liquidprismclock.model.Argb
import com.example.liquidprismclock.model.PerformanceTier
import com.example.liquidprismclock.model.Settings
import com.example.liquidprismclock.model.ThemeMode
import com.example.liquidprismclock.model.TimeOfDay
import com.example.liquidprismclock.ui.components.DigitState
import com.example.liquidprismclock.ui.components.FlipClock
import com.example.liquidprismclock.ui.components.FrameState
import com.example.liquidprismclock.ui.components.FpsLiveStats
import com.example.liquidprismclock.ui.components.GlassButton
import com.example.liquidprismclock.ui.components.PerfOverlay
import com.example.liquidprismclock.ui.components.PrismBackground
import com.example.liquidprismclock.ui.components.rememberFpsMeter
import com.example.liquidprismclock.ui.components.rememberFrameLoop
import com.example.liquidprismclock.ui.components.rememberParticleController
import com.example.liquidprismclock.animation.LiquidFlipAnimation
import com.example.liquidprismclock.viewmodel.FlipClockViewModel
import kotlinx.coroutines.delay
import java.util.Locale

/** Mutable, never-recompose live refs read from the frame callback. */
private class LiveRefs : FpsLiveStats() {
    var particleScale = 0.7f
    var screenW = 1f
    var screenH = 1f
}

@Composable
fun ClockScreen(
    tier: PerformanceTier,
    onOpenSettings: () -> Unit,
    onToggleFullscreen: () -> Unit,
    sharedViewModel: FlipClockViewModel? = null,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as android.app.Application
    val viewModel: FlipClockViewModel =
        sharedViewModel ?: viewModel(factory = FlipClockViewModel.factory(app))
    val settings by viewModel.settings.collectAsState()
    val time by viewModel.time.collectAsState()
    val palette by viewModel.palette.collectAsState()

    val particleController = rememberParticleController(tier.particlePool, tier.ambientParticles)
    val fpsMeter = rememberFpsMeter()
    val digitStates = remember { Array(6) { DigitState() } }
    val live = remember { LiveRefs() }

    var controlsVisible by remember { mutableStateOf(true) }
    var interactionTick by remember { mutableIntStateOf(0) }

    // Keep live refs current without re-registering the frame loop.
    live.particleScale = settings.particleIntensity.scale
    val glowScale = settings.glowIntensity.scale
    val animScale = settings.animationIntensity.scale

    val frame = rememberFrameLoop { nanos, dt ->
        particleController.update(dt, live.particleScale, nanos / 1_000_000_000f)
        live.fps = fpsMeter.onFrame(dt)
        var flips = 0
        for (s in digitStates) {
            if (s.startNanos > 0L) {
                val el = (nanos - s.startNanos) / 1_000_000L
                if (el in 0L until LiquidFlipAnimation.TOTAL_MS.toLong()) flips++
            }
        }
        live.activeFlips = flips
    }

    // Seed ambient dust once.
    LaunchedEffect(Unit) { particleController.seedAmbient(live.particleScale) }

    // Auto-hide controls after ~3s of inactivity.
    LaunchedEffect(controlsVisible, interactionTick) {
        if (controlsVisible) {
            delay(3000)
            controlsVisible = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                live.screenW = coords.size.width.toFloat().coerceAtLeast(1f)
                live.screenH = coords.size.height.toFloat().coerceAtLeast(1f)
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        controlsVisible = true
                        interactionTick++
                    },
                )
            }
    ) {
        // Ambient animated background + particles (draw-only, frame clocked).
        PrismBackground(
            palette = palette,
            frame = frame,
            particleController = particleController,
            tier = tier,
            particleScale = settings.particleIntensity.scale,
            modifier = Modifier.fillMaxSize(),
        )

        // Center stack: time-of-day, clock, date.
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (settings.showTimeOfDay) {
                TimeOfDayLabel(time.timeOfDay, palette)
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                FlipClock(
                    time = time,
                    use24Hour = settings.use24Hour,
                    showSeconds = settings.showSeconds,
                    palette = palette,
                    frame = frame,
                    glowScale = glowScale,
                    animationScale = animScale,
                    states = digitStates,
                    onBurst = { x, y ->
                        val count = when (settings.particleIntensity) {
                            com.example.liquidprismclock.model.Intensity.OFF -> 0
                            com.example.liquidprismclock.model.Intensity.LOW -> 4
                            com.example.liquidprismclock.model.Intensity.MEDIUM -> 8
                            com.example.liquidprismclock.model.Intensity.HIGH -> 14
                        }
                        if (count > 0) {
                            particleController.burst(
                                x, y, live.screenW, live.screenH, count, live.particleScale
                            )
                        }
                    },
                )
            }

            if (settings.showDate) {
                Text(
                    text = time.dateString(Locale.getDefault()),
                    color = Color(Argb.withAlpha(palette.secondary, 0.72f)),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 28.dp),
                )
            }
        }

        // Developer overlay.
        if (settings.showFps) {
            PerfOverlay(
                frame = frame,
                live = live,
                showDebug = settings.showAnimationDebug,
                particleCountProvider = { particleController.pool.activeCount },
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            )
        }

        // Floating controls — fade out after inactivity, tap to reveal.
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 18.dp),
        ) {
            ControlsRow(
                settings = settings,
                palette = palette,
                onToggleFormat = { viewModel.toggleUse24Hour(); interactionTick++ },
                onToggleSeconds = { viewModel.toggleShowSeconds(); interactionTick++ },
                onCycleTheme = { viewModel.cycleThemeMode(); interactionTick++ },
                onToggleFullscreen = { onToggleFullscreen(); interactionTick++ },
                onToggleKeepAwake = { viewModel.setKeepScreenAwake(!settings.keepScreenAwake); interactionTick++ },
                onOpenSettings = { onOpenSettings(); interactionTick++ },
            )
        }
    }
}

@Composable
private fun TimeOfDayLabel(tod: TimeOfDay, palette: com.example.liquidprismclock.model.PrismPalette) {
    val res = when (tod) {
        TimeOfDay.MORNING -> R.string.tod_morning
        TimeOfDay.AFTERNOON -> R.string.tod_afternoon
        TimeOfDay.EVENING -> R.string.tod_evening
        TimeOfDay.NIGHT -> R.string.tod_night
    }
    Text(
        text = stringResource(res),
        color = Color(Argb.withAlpha(palette.primary, 0.85f)),
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 3.sp,
        modifier = Modifier.padding(bottom = 34.dp),
    )
}

@Composable
private fun ControlsRow(
    settings: Settings,
    palette: com.example.liquidprismclock.model.PrismPalette,
    onToggleFormat: () -> Unit,
    onToggleSeconds: () -> Unit,
    onCycleTheme: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onToggleKeepAwake: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassButton(
            icon = Icons.Filled.Schedule,
            contentDescription = stringResource(R.string.cd_toggle_format),
            palette = palette,
            active = settings.use24Hour,
            onClick = onToggleFormat,
        )
        GlassButton(
            icon = Icons.Filled.AvTimer,
            contentDescription = stringResource(R.string.cd_toggle_seconds),
            palette = palette,
            active = settings.showSeconds,
            onClick = onToggleSeconds,
        )
        GlassButton(
            icon = Icons.Filled.Palette,
            contentDescription = stringResource(R.string.cd_cycle_theme),
            palette = palette,
            active = settings.themeMode != ThemeMode.AUTO,
            onClick = onCycleTheme,
        )
        GlassButton(
            icon = if (settings.fullscreen) Icons.Filled.Fullscreen else Icons.Filled.FullscreenExit,
            contentDescription = stringResource(R.string.cd_toggle_fullscreen),
            palette = palette,
            active = settings.fullscreen,
            onClick = onToggleFullscreen,
        )
        GlassButton(
            icon = if (settings.keepScreenAwake) Icons.Filled.LightMode else Icons.Filled.Bedtime,
            contentDescription = stringResource(R.string.cd_toggle_keep_awake),
            palette = palette,
            active = settings.keepScreenAwake,
            onClick = onToggleKeepAwake,
        )
        GlassButton(
            icon = Icons.Filled.Settings,
            contentDescription = stringResource(R.string.cd_open_settings),
            palette = palette,
            active = false,
            onClick = onOpenSettings,
        )
    }
}

