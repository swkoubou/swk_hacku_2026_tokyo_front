package com.example.myapplication

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.WorkManager
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit
import android.content.Context

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("todo_state", MODE_PRIVATE)

        prefs.edit()
            .putBoolean("step1", false)
            .putBoolean("step2", false)
            .putBoolean("step3", false)
            .apply()

        scheduleDailyReset(this)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val onbutton = findViewById<Button>(R.id.sampleButton)

        onbutton.setOnClickListener {

            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this, FlowWallpaperService::class.java)
            )

            startActivity(intent)

            // アプリを閉じる
            finish()
        }

        // ライブ壁紙OFFボタン
        val offButton = findViewById<Button>(R.id.offButton)

        offButton.setOnClickListener {

            val intent = Intent(WallpaperManager.ACTION_CROP_AND_SET_WALLPAPER)
            startActivity(intent)

            finish()
        }
    }

    fun scheduleDailyReset(context: Context) {

        val calendar = java.util.Calendar.getInstance()

        calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)

        val delay = calendar.timeInMillis - System.currentTimeMillis()

        val workRequest =
            PeriodicWorkRequestBuilder<ResetTodoWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "todo_reset_work",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
}