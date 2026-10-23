package com.example.liquidprismclock

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.liquidprismclock.model.PerformanceTier
import com.example.liquidprismclock.ui.ClockScreen
import com.example.liquidprismclock.ui.SettingsScreen
import com.example.liquidprismclock.ui.theme.LiquidPrismTheme
import com.example.liquidprismclock.viewmodel.FlipClockViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Coarse hardware tier used to scale particles/effects.
        val tier = PerformanceTier.detect(this)

        setContent {
            LiquidPrismTheme {
                val app = application
                val viewModel: FlipClockViewModel = viewModel(factory = FlipClockViewModel.factory(app))
                val settings by viewModel.settings.collectAsState()
                var showSettings by remember { mutableStateOf(false) }

                // Apply display flags as settings change.
                SideEffect {
                    applyFullscreen(settings.fullscreen)
                    applyKeepScreenOn(settings.keepScreenAwake)
                }

                AnimatedContent(
                    targetState = showSettings,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "screen",
                    modifier = Modifier.fillMaxSize(),
                ) { settingsOpen ->
                    if (settingsOpen) {
                        SettingsScreen(
                            settings = settings,
                            onBack = { showSettings = false },
                            onUse24Hour = { viewModel.toggleUse24Hour() },
                            onShowSeconds = { viewModel.setShowSeconds(it) },
                            onShowDate = { viewModel.setShowDate(it) },
                            onShowTimeOfDay = { viewModel.setShowTimeOfDay(it) },
                            onThemeMode = { viewModel.setThemeMode(it) },
                            onAnimationIntensity = { viewModel.setAnimationIntensity(it) },
                            onParticleIntensity = { viewModel.setParticleIntensity(it) },
                            onGlowIntensity = { viewModel.setGlowIntensity(it) },
                            onFullscreen = { viewModel.setFullscreen(it) },
                            onKeepAwake = { viewModel.setKeepScreenAwake(it) },
                            onShowFps = { viewModel.setShowFps(it) },
                            onShowDebug = { viewModel.setShowAnimationDebug(it) },
                        )
                    } else {
                        ClockScreen(
                            tier = tier,
                            onOpenSettings = { showSettings = true },
                            onToggleFullscreen = {
                                viewModel.setFullscreen(!settings.fullscreen)
                            },
                            sharedViewModel = viewModel,
                        )
                    }
                }
            }
        }
    }

    /**
     * Immersive sticky mode via the modern WindowInsetsController API (the
     * old SYSTEM_UI_FLAG fullscreen flags are deprecated). Sticky immersion
     * works with gesture navigation: swiping temporarily reveals the bars and
     * they auto-hide again.
     */
    private fun applyFullscreen(fullscreen: Boolean) {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (fullscreen) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    /**
     * Keeps THIS ACTIVITY'S display on while the clock is visible.
     *
     * This is deliberately NOT a system Always-On Display (AOD) — regular apps
     * cannot replace the system AOD; it only adds FLAG_KEEP_SCREEN_ON while the
     * clock Activity is in the foreground.
     */
    private fun applyKeepScreenOn(keepOn: Boolean) {
        if (keepOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
