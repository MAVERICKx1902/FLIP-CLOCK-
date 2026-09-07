package com.example.liquidprismclock.model

/**
 * Persistent user settings. Immutable snapshot exposed through StateFlow.
 */
data class Settings(
    val use24Hour: Boolean = false,
    val showSeconds: Boolean = true,
    val showDate: Boolean = true,
    val showTimeOfDay: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.AUTO,
    val animationIntensity: Intensity = Intensity.MEDIUM,
    val particleIntensity: Intensity = Intensity.MEDIUM,
    val glowIntensity: Intensity = Intensity.MEDIUM,
    val fullscreen: Boolean = true,
    val keepScreenAwake: Boolean = true,
    val showFps: Boolean = false,
    val showAnimationDebug: Boolean = false,
) {
    companion object {
        val DEFAULT = Settings()
    }
}
