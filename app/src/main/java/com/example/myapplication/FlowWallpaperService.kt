package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

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
            draw()
        }

        private fun draw() {

            val holder = surfaceHolder

            if (!holder.surface.isValid) return

            val canvas = holder.lockCanvas() ?: return

            canvas.drawColor(Color.BLACK)

            val step1 = prefs.getBoolean("step1", false)
            val step2 = prefs.getBoolean("step2", false)
            val step3 = prefs.getBoolean("step3", false)

            drawBox(canvas, 200f, 200f, "Start", step1)
            drawBox(canvas, 200f, 400f, "Task1", step2)
            drawBox(canvas, 200f, 600f, "Task2", step3)

            drawArrow(canvas, 300f, 300f, 300f, 400f)
            drawArrow(canvas, 300f, 500f, 300f, 600f)

            holder.unlockCanvasAndPost(canvas)
        }

        private fun drawBox(
            canvas: Canvas,
            x: Float,
            y: Float,
            text: String,
            done: Boolean
        ) {

            paint.style = Paint.Style.FILL
            paint.color = if (done) Color.GREEN else Color.GRAY

            canvas.drawRect(x, y, x + 200, y + 100, paint)

            paint.color = Color.WHITE
            paint.textSize = 40f

            canvas.drawText(text, x + 30, y + 60, paint)
        }

        private fun drawArrow(
            canvas: Canvas,
            x1: Float,
            y1: Float,
            x2: Float,
            y2: Float
        ) {

            paint.color = Color.WHITE
            paint.strokeWidth = 5f

            canvas.drawLine(x1, y1, x2, y2, paint)
        }
    }
}