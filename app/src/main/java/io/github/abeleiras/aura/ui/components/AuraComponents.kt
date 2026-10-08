package io.github.abeleiras.aura.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.abeleiras.aura.R
import io.github.abeleiras.aura.ui.theme.BorderWidth
import io.github.abeleiras.aura.ui.theme.aura

/** A solid block offset behind the element, the "raised paper" shadow of the Formula Farma look. Leave room for it in the layout. */
fun Modifier.hardShadow(offset: Dp = 4.dp, color: Color? = null): Modifier = composed {
    val shadow = color ?: MaterialTheme.aura.shadow
    drawBehind { drawRect(shadow, topLeft = Offset(offset.toPx(), offset.toPx()), size = size) }
}

/** Primary action: purple, square, outlined, raised. */
@Composable
fun AuraButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp).then(if (enabled) Modifier.hardShadow(3.dp) else Modifier),
        enabled = enabled,
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        border = BorderStroke(BorderWidth, if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant),
        content = content,
    )
}

/** Secondary action: surface colour, square, outlined. */
@Composable
fun AuraOutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = RectangleShape,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        border = BorderStroke(BorderWidth, if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant),
        content = content,
    )
}

/** Quiet action inside lists and dialogs. */
@Composable
fun AuraTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 44.dp),
        enabled = enabled,
        shape = RectangleShape,
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        content = content,
    )
}

/** A bordered block on the surface colour. */
@Composable
fun AuraCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
        border = BorderStroke(BorderWidth, MaterialTheme.colorScheme.outline),
        content = content,
    )
}

/** A bordered block in the attention accent (lime): what the user should notice or act on. */
@Composable
fun AuraAttentionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.aura.attention, contentColor = MaterialTheme.aura.onAttention),
        border = BorderStroke(BorderWidth, MaterialTheme.colorScheme.outline),
        content = content,
    )
}

/** Small mono label in a box, e.g. a state: filled for emphasis, outlined otherwise. */
@Composable
fun AuraTag(text: String, modifier: Modifier = Modifier, container: Color = Color.Transparent, content: Color = MaterialTheme.colorScheme.onSurface) {
    Box(
        modifier = modifier
            .background(container)
            .border(BorderWidth, MaterialTheme.colorScheme.outline)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = content)
    }
}

/** The Aura mark, drawn in the text colour so it works on light and dark. */
@Composable
fun AuraLogo(modifier: Modifier = Modifier, size: Dp = 40.dp, tint: Color = MaterialTheme.colorScheme.onBackground) {
    Image(
        painter = painterResource(R.drawable.ic_aura_logo),
        contentDescription = null,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}
