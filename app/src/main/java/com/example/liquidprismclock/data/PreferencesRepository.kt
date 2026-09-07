package com.example.liquidprismclock.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.liquidprismclock.model.Intensity
import com.example.liquidprismclock.model.Settings
import com.example.liquidprismclock.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "liquid_prism_settings"
)

/**
 * DataStore-backed persistence for all user settings.
 *
 * Exposes a cold [Flow]<Settings> and suspend setters; the ViewModel turns
 * these into a single observable state.
 */
class PreferencesRepository(private val context: Context) {

    private object Keys {
        val USE_24_HOUR = booleanPreferencesKey("use_24_hour")
        val SHOW_SECONDS = booleanPreferencesKey("show_seconds")
        val SHOW_DATE = booleanPreferencesKey("show_date")
        val SHOW_TOD = booleanPreferencesKey("show_tod")
        val THEME_MODE = intPreferencesKey("theme_mode")
        val ANIM_INTENSITY = intPreferencesKey("anim_intensity")
        val PARTICLE_INTENSITY = intPreferencesKey("particle_intensity")
        val GLOW_INTENSITY = intPreferencesKey("glow_intensity")
        val FULLSCREEN = booleanPreferencesKey("fullscreen")
        val KEEP_AWAKE = booleanPreferencesKey("keep_awake")
        val SHOW_FPS = booleanPreferencesKey("show_fps")
        val SHOW_DEBUG = booleanPreferencesKey("show_debug")
    }

    val settingsFlow: Flow<Settings> = context.settingsDataStore.data.map { p ->
        Settings(
            use24Hour = p[Keys.USE_24_HOUR] ?: false,
            showSeconds = p[Keys.SHOW_SECONDS] ?: true,
            showDate = p[Keys.SHOW_DATE] ?: true,
            showTimeOfDay = p[Keys.SHOW_TOD] ?: true,
            themeMode = ThemeMode.entries.getOrElse(p[Keys.THEME_MODE] ?: 0) { ThemeMode.AUTO },
            animationIntensity = intensity(p[Keys.ANIM_INTENSITY], Intensity.MEDIUM.ordinal),
            particleIntensity = intensity(p[Keys.PARTICLE_INTENSITY], Intensity.MEDIUM.ordinal),
            glowIntensity = intensity(p[Keys.GLOW_INTENSITY], Intensity.MEDIUM.ordinal),
            fullscreen = p[Keys.FULLSCREEN] ?: true,
            keepScreenAwake = p[Keys.KEEP_AWAKE] ?: true,
            showFps = p[Keys.SHOW_FPS] ?: false,
            showAnimationDebug = p[Keys.SHOW_DEBUG] ?: false,
        )
    }

    private fun intensity(stored: Int?, defaultOrdinal: Int): Intensity =
        Intensity.entries.getOrElse(stored ?: defaultOrdinal) { Intensity.DEFAULT }

    suspend fun setUse24Hour(v: Boolean) = edit { it[Keys.USE_24_HOUR] = v }
    suspend fun setShowSeconds(v: Boolean) = edit { it[Keys.SHOW_SECONDS] = v }
    suspend fun setShowDate(v: Boolean) = edit { it[Keys.SHOW_DATE] = v }
    suspend fun setShowTimeOfDay(v: Boolean) = edit { it[Keys.SHOW_TOD] = v }
    suspend fun setThemeMode(v: ThemeMode) = edit { it[Keys.THEME_MODE] = v.ordinal }
    suspend fun setAnimationIntensity(v: Intensity) = edit { it[Keys.ANIM_INTENSITY] = v.ordinal }
    suspend fun setParticleIntensity(v: Intensity) = edit { it[Keys.PARTICLE_INTENSITY] = v.ordinal }
    suspend fun setGlowIntensity(v: Intensity) = edit { it[Keys.GLOW_INTENSITY] = v.ordinal }
    suspend fun setFullscreen(v: Boolean) = edit { it[Keys.FULLSCREEN] = v }
    suspend fun setKeepScreenAwake(v: Boolean) = edit { it[Keys.KEEP_AWAKE] = v }
    suspend fun setShowFps(v: Boolean) = edit { it[Keys.SHOW_FPS] = v }
    suspend fun setShowAnimationDebug(v: Boolean) = edit { it[Keys.SHOW_DEBUG] = v }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.settingsDataStore.edit(block)
    }
}
