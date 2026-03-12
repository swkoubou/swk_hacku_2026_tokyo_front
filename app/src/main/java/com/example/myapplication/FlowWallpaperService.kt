package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import org.json.JSONArray

class FlowWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return FlowEngine()
    }

    inner class FlowEngine : Engine(),
        SharedPreferences.OnSharedPreferenceChangeListener {

        private val paint = Paint()
        private lateinit var prefs: SharedPreferences

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)

            prefs = applicationContext.getSharedPreferences(
                "todo_state",
                Context.MODE_PRIVATE
            )

            prefs.registerOnSharedPreferenceChangeListener(this)

            draw()
        }

        override fun onDestroy() {
            prefs.unregisterOnSharedPreferenceChangeListener(this)
            super.onDestroy()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            draw()
        }

        override fun onSharedPreferenceChanged(
            sharedPreferences: SharedPreferences?,
            key: String?
        ) {

            if (key == "step3") {

                val step3 = prefs.getBoolean("step3", false)
                val tasks = loadTasks()

                if (step3) {
                    shiftUp(tasks)
                } else {

                    if (tasks[1] != "") {
                        shiftDown(tasks)
                    }
                }

                saveTasks(tasks)
            }

            draw()
        }

        private fun draw() {

            val holder = surfaceHolder
            if (!holder.surface.isValid) return

            val canvas = holder.lockCanvas() ?: return

            val cwidth = (canvas.width - 10f) / 2

            canvas.drawColor(Color.BLACK)

            val tasks = loadTasks()

            val step1 = prefs.getBoolean("step1", false)
            val step2 = prefs.getBoolean("step2", false)
            val step3 = prefs.getBoolean("step3", false)
            val step4 = prefs.getBoolean("step4", false)
            val step5 = prefs.getBoolean("step5", false)

            drawBox(canvas, 100f, 200f, tasks[0], step1)
            drawBox(canvas, 100f, 600f, tasks[1], step2)
            drawBox(canvas, 100f, 1000f, tasks[2], step3)
            drawBox(canvas, 100f, 1400f, tasks[3], step4)
            drawBox(canvas, 100f, 1800f, tasks[4], step5)

            drawArrow(canvas, cwidth, 450f, cwidth, 600f)
            drawArrow(canvas, cwidth, 850f, cwidth, 1000f)
            drawArrow(canvas, cwidth, 1250f, cwidth, 1400f)
            drawArrow(canvas, cwidth, 1650f, cwidth, 1800f)

            holder.unlockCanvasAndPost(canvas)
        }

        private fun drawBox(
            canvas: Canvas,
            x: Float,
            y: Float,
            text: String,
            done: Boolean
        ) {

            val width = canvas.width - 200
            val rect = RectF(x, y, x + width, y + 250)

            paint.style = Paint.Style.FILL
            paint.color = if (done) Color.GREEN else Color.GRAY

            canvas.drawRoundRect(rect, 40f, 40f, paint)

            paint.color = Color.WHITE
            paint.textSize = 100f
            paint.textAlign = Paint.Align.CENTER
            paint.isAntiAlias = true

            val textX = rect.centerX()
            val textY = rect.centerY() - (paint.ascent() + paint.descent()) / 2

            canvas.drawText(text, textX, textY, paint)
        }

        private fun drawArrow(
            canvas: Canvas,
            x1: Float,
            y1: Float,
            x2: Float,
            y2: Float
        ) {

            paint.color = Color.WHITE
            paint.strokeWidth = 10f

            canvas.drawLine(x1, y1, x2, y2, paint)
        }

        fun loadTasks(): MutableList<String> {

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

        private fun saveTasks(tasks: List<String>) {

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
}