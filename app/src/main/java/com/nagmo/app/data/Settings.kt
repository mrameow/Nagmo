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

data class NagmoSettings(
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
