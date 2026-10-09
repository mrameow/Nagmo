package com.nagmo.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nagmo.app.R
import androidx.compose.material3.Icon

enum class Mood(val res: Int) {
    HAPPY(R.drawable.mascot_happy),
    NAGGING(R.drawable.mascot_nagging),
    SLEEPY(R.drawable.mascot_sleepy),
    PARTY(R.drawable.mascot_party),
}

/** Nagmo the sticky note, gently bobbing. */
@Composable
fun Mascot(mood: Mood, size: Dp, modifier: Modifier = Modifier, animate: Boolean = true) {
    val transition = rememberInfiniteTransition(label = "bob")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(if (mood == Mood.NAGGING) 260 else 1400), RepeatMode.Reverse),
        label = "bob",
    )
    Image(
        painter = painterResource(mood.res),
        contentDescription = "Nagmo",
        modifier = modifier
            .size(size)
            .graphicsLayer {
                if (animate) {
                    when (mood) {
                        Mood.NAGGING -> rotationZ = (bob - 0.5f) * 8f
                        Mood.SLEEPY -> scaleY = 1f + bob * 0.03f
                        else -> translationY = -bob * size.toPx() * 0.05f
                    }
                }
            },
    )
}

@Composable
fun EmptyState(mood: Mood, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Mascot(mood, 140.dp)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun SectionTitle(emoji: String, text: String, modifier: Modifier = Modifier) {
    Row(modifier.padding(top = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

/** A round, chunky checkbox that fits the sticky-note look. */
@Composable
fun NagCheck(checked: Boolean, onToggle: () -> Unit, tint: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(if (checked) tint else Color.Transparent)
            .border(2.5.dp, tint, CircleShape)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(Icons.Filled.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

/** A small pill used for metadata on cards. */
@Composable
fun Tag(text: String, color: Color, contentColor: Color, modifier: Modifier = Modifier) {
    Surface(color = color, contentColor = contentColor, shape = RoundedCornerShape(50), modifier = modifier) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            maxLines = 1,
        )
    }
}

@Composable
fun ColorDot(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

val SpacedBy8 = Arrangement.spacedBy(8.dp)
