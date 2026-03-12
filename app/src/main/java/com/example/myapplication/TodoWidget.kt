package com.example.myapplication

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.RemoteViews
import org.json.JSONArray

class TodoWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray
    ) {

        for (id in ids) {

            val views = RemoteViews(context.packageName, R.layout.widget_todo)

            val intent1 = Intent(context, TodoWidget::class.java)
            intent1.action = "toggle_step1"

            val p1 = PendingIntent.getBroadcast(
                context,
                1,
                intent1,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(R.id.checkbox1, p1)

            val intent2 = Intent(context, TodoWidget::class.java)
            intent2.action = "toggle_step2"

            val p2 = PendingIntent.getBroadcast(
                context,
                2,
                intent2,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(R.id.checkbox2, p2)

            manager.updateAppWidget(id, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {

        super.onReceive(context, intent)

        val prefs = context.getSharedPreferences(
            "todo_state",
            Context.MODE_PRIVATE
        )

        val tasks = loadTasks(prefs)

        when (intent.action) {

            "toggle_step1" -> {

                val s1 = prefs.getBoolean("step1", false)

                prefs.edit().putBoolean("step1", !s1).apply()

                if (s1) {
                    shiftDown(tasks)
                    saveTasks(prefs, tasks)
                }
            }

            "toggle_step2" -> {

                val s1 = prefs.getBoolean("step1", false)
                val s2 = prefs.getBoolean("step2", false)

                prefs.edit().putBoolean("step2", !s2).apply()

                if (s1 && !s2) {

                    if (tasks[2] != "") {

                        shiftUp(tasks)
                        saveTasks(prefs, tasks)
                    }
                }
            }
        }
    }

    private fun loadTasks(prefs: SharedPreferences): MutableList<String> {

        val json = prefs.getString("tasks", null)

        val list = mutableListOf<String>()

        if (json != null) {

            val array = JSONArray(json)

            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }

        } else {

            list.add("Task1")
            list.add("Task2")
            list.add("Task3")
            list.add("Task4")
            list.add("Task5")
        }

        return list
    }

    private fun saveTasks(
        prefs: SharedPreferences,
        tasks: List<String>
    ) {

        val array = JSONArray()

        for (task in tasks) {
            array.put(task)
        }

        prefs.edit()
            .putString("tasks", array.toString())
            .apply()
    }

    private fun shiftUp(tasks: MutableList<String>) {

        tasks.removeAt(0)
        tasks.add("")
    }

    private fun shiftDown(tasks: MutableList<String>) {

        tasks.removeLast()
        tasks.add(0, "")
    }
}