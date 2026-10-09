package com.nagmo.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.nagmo.app.R
import com.nagmo.app.alarm.Notifications
import com.nagmo.app.data.NagRepository
import com.nagmo.app.data.Settings
import com.nagmo.app.util.TimeFormat

/** Small widget: Nagmo + "Nag me" button, with the very next thing to do. */
class QuickAddWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        NagRepository.init(context)
        Settings.init(context)
        ids.forEach { update(context, manager, it) }
    }

    companion object {
        fun update(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_quick_add)
            val next = NagRepository.pendingSorted().firstOrNull()
            val overdue = NagRepository.pendingSorted().count { it.isOverdue() }
            views.setImageViewResource(
                R.id.quick_mascot,
                when {
                    overdue > 0 -> R.drawable.mascot_nagging
                    next == null -> R.drawable.mascot_sleepy
                    else -> R.drawable.mascot_happy
                },
            )
            views.setTextViewText(R.id.quick_next, next?.let { n ->
                n.title
            } ?: context.getString(R.string.widget_nothing_next))
            val whenAt = next?.let { it.dueAt ?: it.nextAlarmAt }
            views.setTextViewText(
                R.id.quick_label,
                when {
                    next == null -> context.getString(R.string.widget_all_clear)
                    next.isOverdue() -> "Overdue"
                    whenAt != null -> "Up next · " + TimeFormat.countdown(whenAt)
                    else -> context.getString(R.string.widget_up_next)
                },
            )
            views.setInt(R.id.quick_add_bg, "setColorFilter", WidgetColors.accent(context))
            views.setOnClickPendingIntent(R.id.quick_root, Notifications.addNagIntent(context, 20))
            manager.updateAppWidget(widgetId, views)
        }
    }
}
