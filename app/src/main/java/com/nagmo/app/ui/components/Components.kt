package com.nagmo.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nagmo.app.ui.theme.LocalNagmo
import com.nagmo.app.ui.theme.MascotBlush
import com.nagmo.app.ui.theme.MascotInk

enum class Mood { HAPPY, NAGGING, SLEEPY, PARTY }

/** Nagmo, drawn in the user's accent colour and floating gently. */
@Composable
fun Mascot(mood: Mood, size: Dp, modifier: Modifier = Modifier, animate: Boolean = true) {
    val extras = LocalNagmo.current
    val shapes = when (mood) {
        Mood.HAPPY -> MascotArt.happy
        Mood.NAGGING -> MascotArt.nagging
        Mood.SLEEPY -> MascotArt.sleepy
        Mood.PARTY -> MascotArt.party
    }
    val paths = remember(mood) { shapes.map { it to PathParser().parsePathString(it.pathData).toPath() } }
    val transition = rememberInfiniteTransition(label = "float")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(if (mood == Mood.NAGGING) 300 else 1800), RepeatMode.Reverse),
        label = "float",
    )
    val body = extras.mascotBody
    val fold = extras.mascotFold
    val markColor = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(
        modifier
            .size(size)
            .semantics { contentDescription = "Nagmo" }
            .graphicsLayer {
                if (animate) {
                    if (mood == Mood.NAGGING) rotationZ = (t - 0.5f) * 6f
                    else translationY = -t * size.toPx() * 0.04f
                }
            },
    ) {
        val s = this.size.minDimension / 100f
        scale(s, pivot = Offset.Zero) {
            for ((shape, path) in paths) {
                val color = when (shape.part) {
                    Part.BODY -> body
                    Part.FOLD -> fold
                    Part.BLUSH -> MascotBlush.copy(alpha = 0.45f)
                    Part.INK -> MascotInk
                    // Symbols outside the note sit on the background, so follow the text colour.
                    Part.MARK -> markColor
                }
                if (shape.strokeWidth > 0f) {
                    drawPath(path, color, style = Stroke(shape.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                } else {
                    drawPath(path, color, style = Fill)
                }
            }
        }
    }
}

@Composable
fun EmptyState(mood: Mood, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Mascot(mood, 88.dp)
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Small uppercase label above a group. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 4.dp, top = 28.dp, bottom = 8.dp),
    )
}

/** A rounded group with a hairline border, like an inset grouped list. */
@Composable
fun Group(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, LocalNagmo.current.hairline),
    ) {
        Column(content = content)
    }
}

@Composable
fun GroupDivider() {
    HorizontalDivider(Modifier.padding(start = 56.dp), thickness = 1.dp, color = LocalNagmo.current.hairline)
}

/** A row inside a [Group]: icon, title, optional value and trailing content. */
@Composable
fun SettingRow(
    icon: ImageVector?,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    valueColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (value != null) {
            Spacer(Modifier.width(12.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = if (valueColor == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

/** A thin round checkbox. */
@Composable
fun NagCheck(checked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    val ring by animateColorAsState(if (checked) color else MaterialTheme.colorScheme.outline, label = "ring")
    val fill by animateColorAsState(if (checked) color else Color.Transparent, label = "fill")
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(fill)
                .border(1.6.dp, ring, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(Icons.Filled.Check, contentDescription = "Done", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
fun Dot(color: Color, size: Dp = 8.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(color))
}

/** Colour swatch used for accents and labels. */
@Composable
fun Swatch(color: Color?, selected: Boolean, onClick: () -> Unit, size: Dp = 36.dp) {
    val ring = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent
    Box(
        Modifier
            .size(size + 8.dp)
            .clip(CircleShape)
            .border(1.5.dp, ring, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(color ?: MaterialTheme.colorScheme.surfaceVariant)
                .then(
                    if (color == null) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape) else Modifier
                ),
        )
    }
}

/** Minimal segmented tabs: selected item gets a soft filled pill. */
@Composable
fun <T> PillTabs(items: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp),
    ) {
        items.forEach { item ->
            val isSel = item == selected
            val bg by animateColorAsState(
                if (isSel) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent, label = "tab",
            )
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(bg)
                    .clickable { onSelect(item) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(item),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSel) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}
