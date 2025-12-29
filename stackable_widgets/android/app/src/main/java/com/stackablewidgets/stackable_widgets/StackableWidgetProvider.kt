package com.stackablewidgets.stackable_widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import android.content.SharedPreferences

class StackableWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val prefs: SharedPreferences = context.getSharedPreferences(
            "FlutterSharedPreferences",
            Context.MODE_PRIVATE
        )

        val widgetCount = prefs.getInt("flutter.widget_count", 0)
        val topWidgetTitle = prefs.getString("flutter.widget_0_title", "No widgets") ?: "No widgets"

        val views = RemoteViews(context.packageName, R.layout.stackable_widget)
        views.setTextViewText(R.id.widget_count, widgetCount.toString())
        views.setTextViewText(
            R.id.widget_count_label,
            if (widgetCount == 1) "widget" else "widgets"
        )
        views.setTextViewText(R.id.top_widget_title, "Top: $topWidgetTitle")

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun onEnabled(context: Context) {
        // Called when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Called when the last widget is removed
    }
}
