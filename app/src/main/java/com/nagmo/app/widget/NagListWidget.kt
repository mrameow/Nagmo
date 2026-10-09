package com.nagmo.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.nagmo.app.R
import com.nagmo.app.alarm.NagMessages
import com.nagmo.app.alarm.Notifications
import com.nagmo.app.data.NagRepository
import com.nagmo.app.data.Settings

/** "Nag list" widget: what you need to do, plus a button to add more. */
class NagListWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        NagRepository.init(context)
        Settings.init(context)
        ids.forEach { update(context, manager, it) }
        @Suppress("DEPRECATION")
        manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
    }

    companion object {
        fun update(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val pending = NagRepository.pendingSorted()
            val today = NagRepository.dueToday()
            val views = RemoteViews(context.packageName, R.layout.widget_nag_list)

            views.setTextViewText(
                R.id.widget_count,
                when {
                    today.isNotEmpty() -> context.resources.getQuantityString(R.plurals.summary_today, today.size, today.size)
                    pending.isNotEmpty() -> context.resources.getQuantityString(R.plurals.widget_pending, pending.size, pending.size)
                    else -> context.getString(R.string.widget_all_clear)
                },
            )
            views.setTextViewText(R.id.widget_empty_text, NagMessages.emptyLine(Settings.current.personality))

            @Suppress("DEPRECATION")
            run {
                val serviceIntent = Intent(context, NagListWidgetService::class.java)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                serviceIntent.data = Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME))
                views.setRemoteAdapter(R.id.widget_list, serviceIntent)
            }
            views.setEmptyView(R.id.widget_list, R.id.widget_empty)

            // Item clicks go through an invisible activity that either opens or completes the nag.
            val template = PendingIntent.getActivity(
                context, 100,
                Intent(context, WidgetActionActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            views.setPendingIntentTemplate(R.id.widget_list, template)

            views.setOnClickPendingIntent(R.id.widget_add, Notifications.addNagIntent(context, 10))
            views.setOnClickPendingIntent(R.id.widget_header, PendingIntent.getActivity(
                context, 101,
                context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent(),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ))
            views.setViewVisibility(R.id.widget_empty, if (pending.isEmpty()) View.VISIBLE else View.GONE)
            manager.updateAppWidget(widgetId, views)
        }
    }
}
