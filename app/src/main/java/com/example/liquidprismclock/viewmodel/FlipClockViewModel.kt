package com.example.liquidprismclock.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.liquidprismclock.data.PreferencesRepository
import com.example.liquidprismclock.model.Intensity
import com.example.liquidprismclock.model.PrismPalette
import com.example.liquidprismclock.model.PrismPalettes
import com.example.liquidprismclock.model.Settings
import com.example.liquidprismclock.model.ThemeMode
import com.example.liquidprismclock.model.TimeData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Single source of truth for the clock.
 *
 * Exposes three [StateFlow]s (settings, authoritative [TimeData], and the
 * resolved [PrismPalette]). The time is ALWAYS re-read from the real device
 * clock ([TimeData.now]) — the loop never synthesizes time; it only schedules
 * wake-ups aligned to the actual wall-clock second boundary so the seconds
 * digit flips precisely.
 *
 * Digit flip animations themselves are owned by the UI's per-slot state and
 * driven by the shared frame clock (draw-only), so they do not flow through
 * this ViewModel.
 */
class FlipClockViewModel(
    app: Application,
    private val repository: PreferencesRepository,
) : AndroidViewModel(app) {

    private val _settings = MutableStateFlow(Settings.DEFAULT)
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private val _time = MutableStateFlow(TimeData.now())
    val time: StateFlow<TimeData> = _time.asStateFlow()

    private val _palette = MutableStateFlow(PrismPalettes.NIGHT)
    val palette: StateFlow<PrismPalette> = _palette.asStateFlow()

    init {
        // Load persisted settings and keep state in sync.
        repository.settingsFlow
            .onEach { _settings.value = it }
            .launchIn(viewModelScope)

        // Wall-clock loop aligned to the real second boundary.
        viewModelScope.launch(Dispatchers.Default) {
            publish(TimeData.now())
            while (isActive) {
                val now = TimeData.now()

                // Long sleep until just before the next second boundary.
                val msToBoundary = (1000 - now.millis).toLong()
                delay((msToBoundary - 16).coerceAtLeast(1L))

                // Short bounded spin to catch the exact boundary (never a busy
                // loop: capped iterations, then waits out the remainder).
                var guard = 0
                while (isActive && guard < 40) {
                    val sample = TimeData.now()
                    if (sample.second != _time.value.second) {
                        publish(sample)
                        break
                    }
                    delay(4)
                    guard++
                }

                // Wait out the rest of the second.
                val after = TimeData.now()
                delay((1000 - after.millis).toLong().coerceIn(1L, 1000L))
            }
        }
    }

    private fun publish(now: TimeData) {
        _time.value = now
        _palette.value = resolvePalette(_settings.value, now)
    }

    private fun resolvePalette(settings: Settings, time: TimeData): PrismPalette =
        when (settings.themeMode) {
            ThemeMode.AUTO -> PrismPalettes.automatic(time.hourFloat)
            else -> PrismPalettes.of(settings.themeMode)
        }

    // ---- Settings mutators (persist via DataStore off the main thread) ----

    fun toggleUse24Hour() = viewModelScope.launch {
        repository.setUse24Hour(!_settings.value.use24Hour)
    }

    fun setShowSeconds(v: Boolean) = viewModelScope.launch { repository.setShowSeconds(v) }
    fun toggleShowSeconds() = viewModelScope.launch {
        repository.setShowSeconds(!_settings.value.showSeconds)
    }

    fun setShowDate(v: Boolean) = viewModelScope.launch { repository.setShowDate(v) }
    fun toggleShowDate() = viewModelScope.launch {
        repository.setShowDate(!_settings.value.showDate)
    }

    fun setShowTimeOfDay(v: Boolean) = viewModelScope.launch {
        repository.setShowTimeOfDay(v)
    }

    fun cycleThemeMode() = viewModelScope.launch {
        val modes = ThemeMode.entries
        val cur = _settings.value.themeMode
        repository.setThemeMode(modes[(modes.indexOf(cur) + 1) % modes.size])
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repository.setThemeMode(mode) }
    fun setAnimationIntensity(v: Intensity) = viewModelScope.launch {
        repository.setAnimationIntensity(v)
    }
    fun setParticleIntensity(v: Intensity) = viewModelScope.launch {
        repository.setParticleIntensity(v)
    }
    fun setGlowIntensity(v: Intensity) = viewModelScope.launch { repository.setGlowIntensity(v) }
    fun setFullscreen(v: Boolean) = viewModelScope.launch { repository.setFullscreen(v) }
    fun setKeepScreenAwake(v: Boolean) = viewModelScope.launch { repository.setKeepScreenAwake(v) }
    fun setShowFps(v: Boolean) = viewModelScope.launch { repository.setShowFps(v) }
    fun setShowAnimationDebug(v: Boolean) = viewModelScope.launch {
        repository.setShowAnimationDebug(v)
    }

    companion object {
        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return FlipClockViewModel(app, PreferencesRepository(app)) as T
                }
            }
    }
}
