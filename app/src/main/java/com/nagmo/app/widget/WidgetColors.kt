package com.nagmo.app.widget

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import com.nagmo.app.data.Accent
import com.nagmo.app.data.NoteColor
import com.nagmo.app.data.Settings

/** Runtime colours for widgets (they follow the system light/dark mode). */
object WidgetColors {
    fun isNight(context: Context) =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    fun accent(context: Context): Int {
        val a = Settings.current.accent
        val night = isNight(context)
        if (a == Accent.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return context.getColor(if (night) android.R.color.system_accent1_200 else android.R.color.system_accent1_600)
        }
        return (if (night) a.dark else a.light).toInt()
    }

    fun label(context: Context, color: NoteColor): Int? =
        if (color == NoteColor.NONE) null else (if (isNight(context)) color.darkArgb else color.argb).toInt()
}
