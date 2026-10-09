package com.nagmo.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagmo.app.data.NoteColor

val Ink = Color(0xFF4A3222)
val InkSoft = Color(0xFF7A6A55)
val NagYellow = Color(0xFFFFD95A)
val NagPink = Color(0xFFFF8FA3)
val NagMint = Color(0xFF8FD9C3)
val OverdueRed = Color(0xFFD93A4C)

private val LightColors = lightColorScheme(
    primary = Color(0xFFD9506A),
    onPrimary = Color.White,
    primaryContainer = NagYellow,
    onPrimaryContainer = Ink,
    secondary = Color(0xFF2E8C72),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDF3E6),
    onSecondaryContainer = Color(0xFF103A2E),
    tertiary = Color(0xFF6A5ACD),
    background = Color(0xFFFFF8E1),
    onBackground = Ink,
    surface = Color(0xFFFFFBEF),
    onSurface = Ink,
    surfaceVariant = Color(0xFFF6EBCB),
    onSurfaceVariant = InkSoft,
    outline = Color(0xFFCDB98F),
    error = OverdueRed,
)

private val DarkColors = darkColorScheme(
    primary = NagPink,
    onPrimary = Color(0xFF4A1020),
    primaryContainer = Color(0xFF5C4A12),
    onPrimaryContainer = Color(0xFFFFF0C2),
    secondary = NagMint,
    onSecondary = Color(0xFF0E3328),
    secondaryContainer = Color(0xFF1F4A3A),
    onSecondaryContainer = Color(0xFFCDF3E6),
    tertiary = Color(0xFFB9AEFF),
    background = Color(0xFF1E1A14),
    onBackground = Color(0xFFF3E9D2),
    surface = Color(0xFF26211A),
    onSurface = Color(0xFFF3E9D2),
    surfaceVariant = Color(0xFF3A3226),
    onSurfaceVariant = Color(0xFFD5C6A8),
    outline = Color(0xFF8A7B60),
    error = Color(0xFFFF8A8A),
)

private val Rounded = FontFamily.SansSerif

private val NagTypography = Typography(
    headlineMedium = TextStyle(fontFamily = Rounded, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp),
    titleLarge = TextStyle(fontFamily = Rounded, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = Rounded, fontWeight = FontWeight.Bold, fontSize = 17.sp),
    bodyLarge = TextStyle(fontFamily = Rounded, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Rounded, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = Rounded, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
)

private val NagShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
)

@Composable
fun NagmoTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = NagTypography,
        shapes = NagShapes,
        content = content,
    )
}

/** Sticky-note colour for the current theme. */
@Composable
fun NoteColor.surface(): Color = Color(if (isSystemInDarkTheme()) darkArgb else argb)

@Composable
fun NoteColor.onSurface(): Color = if (isSystemInDarkTheme()) Color(0xFFF7EEDC) else Ink
