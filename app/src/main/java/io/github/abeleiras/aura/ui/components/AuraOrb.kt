package io.github.abeleiras.aura.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.abeleiras.aura.R

/**
 * The Aura logo. While recording it spins slowly and pulses a soft glow behind it:
 * the main screen's visual signal that recording is on (Constitution V).
 */
@Composable
fun AuraOrb(
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
) {
    val glowColor = MaterialTheme.colorScheme.primary
    Box(modifier = modifier.size(size * 1.8f), contentAlignment = Alignment.Center) {
        if (isRecording) {
            // The infinite transition only exists while recording, so idle costs nothing.
            val transition = rememberInfiniteTransition(label = "aura_orb")
            val rotation by transition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(durationMillis = 6000, easing = LinearEasing)),
                label = "rotation",
            )
            val glowAlpha by transition.animateFloat(
                initialValue = 0.15f,
                targetValue = 0.45f,
                animationSpec = infiniteRepeatable(tween(durationMillis = 1800, easing = LinearEasing), RepeatMode.Reverse),
                label = "glow_alpha",
            )
            Box(
                modifier = Modifier
                    .size(size * 1.6f)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(glowColor.copy(alpha = glowAlpha), glowColor.copy(alpha = 0f)),
                                center = Offset(this.size.width / 2f, this.size.height / 2f),
                                radius = this.size.minDimension / 2f,
                            ),
                        )
                    },
            )
            Image(
                painter = painterResource(R.drawable.ic_aura_orb),
                contentDescription = null,
                modifier = Modifier.size(size).rotate(rotation),
            )
        } else {
            Image(painter = painterResource(R.drawable.ic_aura_orb), contentDescription = null, modifier = Modifier.size(size))
        }
    }
}
