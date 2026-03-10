package com.example.myapplication

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class TodoWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray
    ) {

        for (id in ids) {

            val views = RemoteViews(context.packageName, R.layout.widget_todo)

            // Step1
            val intent1 = Intent(context, TodoWidget::class.java)
            intent1.action = "toggle_step1"

            val pending1 = PendingIntent.getBroadcast(
                context,
                1,
                intent1,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(R.id.checkbox1, pending1)

            // Step2
            val intent2 = Intent(context, TodoWidget::class.java)
            intent2.action = "toggle_step2"

            val pending2 = PendingIntent.getBroadcast(
                context,
                2,
                intent2,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(R.id.checkbox2, pending2)

            manager.updateAppWidget(id, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val prefs = context.getSharedPreferences("todo_state", Context.MODE_PRIVATE)

        when (intent.action) {

            "toggle_step1" -> {
                val v = prefs.getBoolean("step1", false)
                prefs.edit().putBoolean("step1", !v).apply()
            }

            "toggle_step2" -> {
                val v = prefs.getBoolean("step2", false)
                prefs.edit().putBoolean("step2", !v).apply()
            }
        }
    }
}