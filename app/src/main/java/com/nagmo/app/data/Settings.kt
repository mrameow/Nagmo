package com.nagmo.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class Personality(val label: String, val blurb: String) {
    GENTLE("Gentle", "Soft, encouraging reminders"),
    SASSY("Sassy", "Playful guilt trips, the Nagmo classic"),
    DRILL("Drill sergeant", "NO EXCUSES. MOVE IT."),
}

enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

/**
 * Accent colours. [light]/[dark] are the accent in each theme, [soft] is the
 * pastel used for the mascot's body. DYNAMIC follows the wallpaper (Android 12+).
 */
enum class Accent(val label: String, val light: Long, val dark: Long, val soft: Long, val fold: Long) {
    HONEY("Honey", 0xFFA86F00, 0xFFF2C14E, 0xFFFFD66B, 0xFFF2B937),
    CORAL("Coral", 0xFFC8503C, 0xFFFF8A75, 0xFFFFB9AB, 0xFFF4937F),
    ROSE("Rose", 0xFFC0436A, 0xFFFF8FB0, 0xFFFFC4D5, 0xFFF39AB6),
    LAVENDER("Lavender", 0xFF6150C8, 0xFFAFA2FF, 0xFFD3CBFF, 0xFFB1A4F5),
    OCEAN("Ocean", 0xFF2A66CC, 0xFF8AB4FF, 0xFFBCD4FF, 0xFF93B6F2),
    SAGE("Sage", 0xFF2F7D5B, 0xFF7FD1A8, 0xFFBFE8D2, 0xFF93D2B2),
    GRAPHITE("Graphite", 0xFF2B2B2E, 0xFFE6E6E8, 0xFFDCDCDF, 0xFFBDBDC2),
    DYNAMIC("Wallpaper", 0xFF6750A4, 0xFFD0BCFF, 0xFFEADDFF, 0xFFCDBDF5),
}

data class NagmoSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: Accent = Accent.HONEY,
    val personality: Personality = Personality.SASSY,
    val lockScreenSummary: Boolean = true,
    /** Minutes before a deadline to send a warning (0 = off). */
    val deadlineWarningMinutes: Int = 60,
    val snoozeMinutes: Int = 10,
    val morningDigest: Boolean = true,
    val digestHour: Int = 8,
    val digestMinute: Int = 0,
    val vibrate: Boolean = true,
)

object Settings {
    private lateinit var prefs: SharedPreferences
    private val _state = MutableStateFlow(NagmoSettings())
    val state: StateFlow<NagmoSettings> = _state

    val current: NagmoSettings get() = _state.value

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.getSharedPreferences("nagmo_settings", Context.MODE_PRIVATE)
        val d = NagmoSettings()
        _state.value = NagmoSettings(
            personality = runCatching { Personality.valueOf(prefs.getString("personality", d.personality.name)!!) }
                .getOrDefault(d.personality),
            themeMode = runCatching { ThemeMode.valueOf(prefs.getString("themeMode", d.themeMode.name)!!) }
                .getOrDefault(d.themeMode),
            accent = runCatching { Accent.valueOf(prefs.getString("accent", d.accent.name)!!) }
                .getOrDefault(d.accent),
            lockScreenSummary = prefs.getBoolean("lockScreenSummary", d.lockScreenSummary),
            deadlineWarningMinutes = prefs.getInt("deadlineWarningMinutes", d.deadlineWarningMinutes),
            snoozeMinutes = prefs.getInt("snoozeMinutes", d.snoozeMinutes),
            morningDigest = prefs.getBoolean("morningDigest", d.morningDigest),
            digestHour = prefs.getInt("digestHour", d.digestHour),
            digestMinute = prefs.getInt("digestMinute", d.digestMinute),
            vibrate = prefs.getBoolean("vibrate", d.vibrate),
        )
    }

    fun update(transform: (NagmoSettings) -> NagmoSettings) {
        val s = transform(_state.value)
        _state.value = s
        prefs.edit()
            .putString("personality", s.personality.name)
            .putString("themeMode", s.themeMode.name)
            .putString("accent", s.accent.name)
            .putBoolean("lockScreenSummary", s.lockScreenSummary)
            .putInt("deadlineWarningMinutes", s.deadlineWarningMinutes)
            .putInt("snoozeMinutes", s.snoozeMinutes)
            .putBoolean("morningDigest", s.morningDigest)
            .putInt("digestHour", s.digestHour)
            .putInt("digestMinute", s.digestMinute)
            .putBoolean("vibrate", s.vibrate)
            .apply()
    }
}
