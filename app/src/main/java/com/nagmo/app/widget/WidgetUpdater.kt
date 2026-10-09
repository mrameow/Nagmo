package com.nagmo.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.nagmo.app.R

object WidgetUpdater {
    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        val listIds = manager.getAppWidgetIds(ComponentName(context, NagListWidget::class.java))
        if (listIds.isNotEmpty()) {
            listIds.forEach { NagListWidget.update(context, manager, it) }
            @Suppress("DEPRECATION")
            manager.notifyAppWidgetViewDataChanged(listIds, R.id.widget_list)
        }
        val quickIds = manager.getAppWidgetIds(ComponentName(context, QuickAddWidget::class.java))
        quickIds.forEach { QuickAddWidget.update(context, manager, it) }
    }
}
