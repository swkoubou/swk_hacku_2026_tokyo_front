package com.example.myapplication

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class ResetTodoWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {

        val prefs = applicationContext.getSharedPreferences(
            "todo_state",
            Context.MODE_PRIVATE
        )

        prefs.edit()
            .putBoolean("step1", false)
            .putBoolean("step2", false)
            .putBoolean("step3", false)
            .apply()

        return Result.success()
    }
}