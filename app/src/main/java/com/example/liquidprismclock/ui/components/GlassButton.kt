package com.example.liquidprismclock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.liquidprismclock.model.Argb
import com.example.liquidprismclock.model.PrismPalette

/**
 * A minimal floating circular glass control: translucent dark body, hairline
 * border, faint prism sheen, 48dp touch target and a Material ripple.
 */
@Composable
fun GlassButton(
    icon: ImageVector,
    contentDescription: String,
    palette: PrismPalette,
    active: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val borderColor = if (active)
        Color(Argb.withAlpha(palette.primary, 0.65f))
    else
        Color(Argb.withAlpha(Argb.WHITE, 0.18f))

    val background = if (active)
        Brush.linearGradient(
            listOf(
                Color(Argb.withAlpha(palette.primary, 0.22f)),
                Color(Argb.withAlpha(palette.secondary, 0.10f)),
            )
        )
    else
        Brush.linearGradient(
            listOf(
                Color(0x18FFFFFF),
                Color(0x080A14),
            )
        )

    val tint = if (active)
        Color(Argb.lighten(palette.primary, 0.25f))
    else
        Color(0xFFE8EEFB)

    Box(
        modifier
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            }
            .size(48.dp)
            .scale(if (pressed) 0.92f else 1f)
            .clip(CircleShape)
            .background(background)
            .border(1.dp, borderColor, CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // described on the button
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}
