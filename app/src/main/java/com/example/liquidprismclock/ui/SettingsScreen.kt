package com.example.liquidprismclock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.liquidprismclock.R
import com.example.liquidprismclock.model.Intensity
import com.example.liquidprismclock.model.Settings
import com.example.liquidprismclock.model.ThemeMode
import com.example.liquidprismclock.ui.theme.PrismViolet
import com.example.liquidprismclock.ui.theme.TextSecondary

/**
 * Material 3 settings surface. Grouped cards for Clock / Appearance / Display /
 * Developer; all controls write straight through to the ViewModel, which
 * persists via DataStore.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    onBack: () -> Unit,
    onUse24Hour: (Boolean) -> Unit,
    onShowSeconds: (Boolean) -> Unit,
    onShowDate: (Boolean) -> Unit,
    onShowTimeOfDay: (Boolean) -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onAnimationIntensity: (Intensity) -> Unit,
    onParticleIntensity: (Intensity) -> Unit,
    onGlowIntensity: (Intensity) -> Unit,
    onFullscreen: (Boolean) -> Unit,
    onKeepAwake: (Boolean) -> Unit,
    onShowFps: (Boolean) -> Unit,
    onShowDebug: (Boolean) -> Unit,
) {
    Scaffold(
        containerColor = Color(0xFF05030F),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xCC0A0718),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ---------- CLOCK ----------
            SettingsCard(stringResource(R.string.settings_clock)) {
                SwitchRow(stringResource(R.string.settings_use_24_hour), settings.use24Hour, onUse24Hour)
                SwitchRow(stringResource(R.string.settings_show_seconds), settings.showSeconds, onShowSeconds)
                SwitchRow(stringResource(R.string.settings_show_date), settings.showDate, onShowDate)
                SwitchRow(stringResource(R.string.settings_show_tod), settings.showTimeOfDay, onShowTimeOfDay)
            }

            // ---------- APPEARANCE ----------
            SettingsCard(stringResource(R.string.settings_appearance)) {
                Text(
                    stringResource(R.string.settings_manual_theme),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { onThemeMode(mode) },
                            label = { Text(themeLabel(mode)) },
                            colors = filterChipColors(),
                        )
                    }
                }
                Text(
                    stringResource(R.string.settings_anim_intensity),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                )
                IntensityRow(settings.animationIntensity, onAnimationIntensity)
                Text(
                    stringResource(R.string.settings_particle_intensity),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                )
                IntensityRow(settings.particleIntensity, onParticleIntensity)
                Text(
                    stringResource(R.string.settings_glow_intensity),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                )
                IntensityRow(settings.glowIntensity, onGlowIntensity)
            }

            // ---------- DISPLAY ----------
            SettingsCard(stringResource(R.string.settings_display)) {
                SwitchRow(stringResource(R.string.settings_fullscreen), settings.fullscreen, onFullscreen)
                SwitchRow(stringResource(R.string.settings_keep_awake), settings.keepScreenAwake, onKeepAwake)
                Text(
                    stringResource(R.string.settings_keep_awake_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 4.dp),
                )
            }

            // ---------- DEVELOPER ----------
            SettingsCard(stringResource(R.string.settings_developer)) {
                SwitchRow(stringResource(R.string.settings_show_fps), settings.showFps, onShowFps)
                SwitchRow(stringResource(R.string.settings_show_debug), settings.showAnimationDebug, onShowDebug)
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x14FFFFFF)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = PrismViolet,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            content()
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IntensityRow(current: Intensity, onSelect: (Intensity) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Intensity.entries.forEach { level ->
            FilterChip(
                selected = current == level,
                onClick = { onSelect(level) },
                label = { Text(intensityLabel(level)) },
                colors = filterChipColors(),
            )
        }
    }
}

@Composable
private fun filterChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = Color(0x339D8CFF),
    selectedLabelColor = Color(0xFFF0F2FF),
    containerColor = Color(0x0FFFFFFF),
    labelColor = TextSecondary,
)

@Composable
private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.AUTO -> stringResource(R.string.theme_auto)
    ThemeMode.MORNING -> stringResource(R.string.theme_morning)
    ThemeMode.AFTERNOON -> stringResource(R.string.theme_afternoon)
    ThemeMode.EVENING -> stringResource(R.string.theme_evening)
    ThemeMode.NIGHT -> stringResource(R.string.theme_night)
}

@Composable
private fun intensityLabel(level: Intensity): String = when (level) {
    Intensity.OFF -> stringResource(R.string.intensity_off)
    Intensity.LOW -> stringResource(R.string.intensity_low)
    Intensity.MEDIUM -> stringResource(R.string.intensity_medium)
    Intensity.HIGH -> stringResource(R.string.intensity_high)
}
