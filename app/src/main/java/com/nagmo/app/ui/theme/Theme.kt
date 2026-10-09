package com.nagmo.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nagmo.app.data.Accent
import com.nagmo.app.data.NoteColor
import com.nagmo.app.data.Settings
import com.nagmo.app.data.ThemeMode

val Danger = Color(0xFFD64545)
val DangerDark = Color(0xFFFF8080)
val MascotInk = Color(0xFF26221C)
val MascotBlush = Color(0xFFFF7A8A)

/** Extra theme values Material doesn't model. */
@Immutable
data class NagmoExtras(
    val dark: Boolean,
    val hairline: Color,
    val mascotBody: Color,
    val mascotFold: Color,
    val danger: Color,
)

val LocalNagmo = staticCompositionLocalOf {
    NagmoExtras(false, Color(0xFFE7E5E0), Color(0xFFFFD66B), Color(0xFFF2B937), Danger)
}

private fun neutralLight(accent: Color, onAccent: Color) = lightColorScheme(
    primary = accent,
    onPrimary = onAccent,
    primaryContainer = accent.copy(alpha = 0.12f).compositeOver(Color.White),
    onPrimaryContainer = accent,
    secondary = Color(0xFF55555C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFEEEB),
    onSecondaryContainer = Color(0xFF17171A),
    background = Color(0xFFFAFAF8),
    onBackground = Color(0xFF17171A),
    surface = Color(0xFFFAFAF8),
    onSurface = Color(0xFF17171A),
    surfaceVariant = Color(0xFFF1F0ED),
    onSurfaceVariant = Color(0xFF6E6E74),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF4F3F0),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFEDECE8),
    outline = Color(0xFFD9D7D2),
    outlineVariant = Color(0xFFE7E5E0),
    error = Danger,
)

private fun neutralDark(accent: Color, onAccent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = onAccent,
    primaryContainer = accent.copy(alpha = 0.16f).compositeOver(Color(0xFF151517)),
    onPrimaryContainer = accent,
    secondary = Color(0xFFB4B4BB),
    onSecondary = Color(0xFF151517),
    secondaryContainer = Color(0xFF26262A),
    onSecondaryContainer = Color(0xFFF2F2F3),
    background = Color(0xFF0E0E10),
    onBackground = Color(0xFFF2F2F3),
    surface = Color(0xFF0E0E10),
    onSurface = Color(0xFFF2F2F3),
    surfaceVariant = Color(0xFF1E1E21),
    onSurfaceVariant = Color(0xFF9C9CA4),
    surfaceContainerLowest = Color(0xFF0B0B0C),
    surfaceContainerLow = Color(0xFF17171A),
    surfaceContainer = Color(0xFF19191C),
    surfaceContainerHigh = Color(0xFF1C1C1F),
    surfaceContainerHighest = Color(0xFF26262A),
    outline = Color(0xFF3A3A40),
    outlineVariant = Color(0xFF2A2A2F),
    error = DangerDark,
)

private val SystemSans = FontFamily.SansSerif

private val NagTypography = Typography(
    displaySmall = TextStyle(fontFamily = SystemSans, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(fontFamily = SystemSans, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontFamily = SystemSans, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontFamily = SystemSans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = SystemSans, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = (-0.1).sp),
    titleSmall = TextStyle(fontFamily = SystemSans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = SystemSans, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = SystemSans, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = SystemSans, fontSize = 12.5.sp, lineHeight = 17.sp, letterSpacing = 0.1.sp),
    labelLarge = TextStyle(fontFamily = SystemSans, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = SystemSans, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontFamily = SystemSans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.8.sp),
)

private val NagShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun NagmoTheme(content: @Composable () -> Unit) {
    val settings by Settings.state.collectAsStateWithLifecycle()
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val accent = settings.accent
    val context = LocalContext.current

    val scheme: ColorScheme
    val mascotBody: Color
    val mascotFold: Color
    if (accent == Accent.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val dyn = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        val base = if (dark) neutralDark(dyn.primary, dyn.onPrimary) else neutralLight(dyn.primary, dyn.onPrimary)
        scheme = base
        val light = dynamicLightColorScheme(context)
        mascotBody = light.primaryContainer
        mascotFold = light.primary.copy(alpha = 0.35f).compositeOver(light.primaryContainer)
    } else {
        val a = Color(if (dark) accent.dark else accent.light)
        val onA = if (dark) Color(0xFF141416) else Color.White
        scheme = if (dark) neutralDark(a, onA) else neutralLight(a, onA)
        mascotBody = Color(accent.soft)
        mascotFold = Color(accent.fold)
    }

    val extras = NagmoExtras(
        dark = dark,
        hairline = scheme.outlineVariant,
        mascotBody = mascotBody,
        mascotFold = mascotFold,
        danger = if (dark) DangerDark else Danger,
    )
    CompositionLocalProvider(LocalNagmo provides extras) {
        MaterialTheme(colorScheme = scheme, typography = NagTypography, shapes = NagShapes, content = content)
    }
}

/** Label colour for the current theme, or null for "no label". */
@Composable
fun NoteColor.dot(): Color? =
    if (this == NoteColor.NONE) null else Color(if (LocalNagmo.current.dark) darkArgb else argb)
