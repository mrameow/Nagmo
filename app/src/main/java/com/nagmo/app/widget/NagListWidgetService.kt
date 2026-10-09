package com.nagmo.app.widget

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.nagmo.app.R
import com.nagmo.app.alarm.AlarmScheduler
import com.nagmo.app.data.Nag
import com.nagmo.app.data.NagRepository
import com.nagmo.app.data.Priority
import com.nagmo.app.util.TimeFormat

@Suppress("DEPRECATION")
class NagListWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = Factory(applicationContext)

    private class Factory(private val context: Context) : RemoteViewsFactory {
        private var items: List<Nag> = emptyList()

        override fun onCreate() {
            NagRepository.init(context)
        }

        override fun onDataSetChanged() {
            items = NagRepository.pendingSorted()
        }

        override fun onDestroy() {}
        override fun getCount() = items.size
        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount() = 1
        override fun getItemId(position: Int) = items.getOrNull(position)?.id?.toLong() ?: position.toLong()
        override fun hasStableIds() = true

        override fun getViewAt(position: Int): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_nag_item)
            val nag = items.getOrNull(position) ?: return views

            views.setTextViewText(R.id.item_title, nag.title)
            views.setInt(R.id.item_color, "setColorFilter", nag.color.argb.toInt())

            val now = System.currentTimeMillis()
            val sub = when {
                nag.isOverdue(now) -> "⚠ Overdue · " + TimeFormat.countdown(nag.dueAt!!, now)
                nag.dueAt != null -> "Due " + TimeFormat.relative(context, nag.dueAt)
                nag.nextAlarmAt != null -> "⏰ " + TimeFormat.relative(context, nag.nextAlarmAt!!)
                nag.details.isNotBlank() -> nag.details.lineSequence().first()
                else -> ""
            }
            views.setTextViewText(R.id.item_subtitle, sub)
            views.setViewVisibility(R.id.item_subtitle, if (sub.isEmpty()) View.GONE else View.VISIBLE)
            views.setTextColor(R.id.item_subtitle, if (nag.isOverdue(now)) 0xFFD93A4C.toInt() else 0xFF7A6A55.toInt())
            views.setViewVisibility(R.id.item_priority, if (nag.priority == Priority.HIGH) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.item_pin, if (nag.pinned) View.VISIBLE else View.GONE)

            views.setOnClickFillInIntent(R.id.item_root, Intent()
                .putExtra(WidgetActionActivity.EXTRA_ACTION, WidgetActionActivity.ACTION_OPEN)
                .putExtra(AlarmScheduler.EXTRA_NAG_ID, nag.id))
            views.setOnClickFillInIntent(R.id.item_check, Intent()
                .putExtra(WidgetActionActivity.EXTRA_ACTION, WidgetActionActivity.ACTION_DONE)
                .putExtra(AlarmScheduler.EXTRA_NAG_ID, nag.id))
            return views
        }
    }
}
