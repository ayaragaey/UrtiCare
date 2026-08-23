package com.example.urticare

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class ConsumptionWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateConsumptionWidget(context, appWidgetManager, appWidgetId)
        }
    }
}

internal fun updateConsumptionWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val views = RemoteViews(context.packageName, R.layout.consumption_widget_layout)

    // Launch MainActivity with extra to trigger the input dialog
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("OPEN_CONSUMPTION_FROM_WIDGET", true)
    }

    val pendingIntent = PendingIntent.getActivity(
        context,
        1,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

    appWidgetManager.updateAppWidget(appWidgetId, views)
}
