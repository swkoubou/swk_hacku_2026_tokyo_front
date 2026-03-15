package com.example.myapplication

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.LocalTime

class ListActivity : AppCompatActivity() {
    lateinit var listView: ListView
    var isUpdating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        listView = findViewById<ListView>(R.id.listView)

        loadTaskList()
    }

    fun loadTaskList() {

        callApi { responseBody ->

            if (responseBody == null) return@callApi

            runOnUiThread {

                val taskList = parseJson(responseBody)
                val adapter = TaskAdapter(this, taskList)

                listView.adapter = adapter

                for (i in taskList.indices) {
                    if (taskList[i].done) {
                        listView.setItemChecked(i, true)
                    }
                }

                listView.setOnItemClickListener { _, _, position, _ ->

                    // 処理中なら何もしない
                    if (isUpdating) return@setOnItemClickListener

                    isUpdating = true

                    val task = taskList[position]

                    val userUuid = task.user_uuid
                    val taskUuid = task.task_uuid
                    val done = task.done

                    pushApi(userUuid, taskUuid, done) {
                        // サーバー更新後に再取得
                        runOnUiThread {
                            loadTaskList()
                            Handler(Looper.getMainLooper()).postDelayed({
                                isUpdating = false
                            }, 1000) // 1000ms = 1秒
                        }
                    }
                }
            }
        }
    }

    fun callApi(callback: (String?) -> Unit) {
        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://hackutokyo2026.yoimiya.net/get_today_events")
            .post(ByteArray(0).toRequestBody(null))
            .addHeader("user_uuid", "3c7a9a24-9e34-4f65-bc1e-9a6e6c7d7f12")
            .build()

        Thread{
            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                callback(responseBody)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    fun pushApi(userUuid: String, taskUuid: String, done: Boolean, callback: () -> Unit) {
        val client = OkHttpClient()
        val json = "{\"task_uuid\": \"" + taskUuid + "\"}"
        val body = json.toRequestBody("application/json".toMediaType())

        if (done) {
            val request = Request.Builder()
                .url("https://hackutokyo2026.yoimiya.net/rollback_today_event")
                .post(body) // POST
                .addHeader("user_uuid", userUuid)
                .addHeader("Content-Type", "application/json")
                .build()

            Thread{
                try {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()

                    callback()

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        } else {
            val request = Request.Builder()
                .url("https://hackutokyo2026.yoimiya.net/do_today_event")
                .post(body) // POST
                .addHeader("user_uuid", userUuid)
                .addHeader("Content-Type", "application/json")
                .build()

            Thread{
                try {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()

                    callback()

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }
    }

    fun parseJson(json: String): List<Task> {

        val gson = Gson()

        val type = object : TypeToken<List<Task>>() {}.type

        val taskList: List<Task> = gson.fromJson(json, type)

        val sortedList = taskList.sortedWith(
            compareBy<Task> { LocalDate.parse(it.end_date) }
                .thenBy {
                    if (it.start_time.isNullOrEmpty())
                        LocalTime.MAX
                    else
                        LocalTime.parse(it.start_time)
                }
        )

        return sortedList
    }
}

class TaskAdapter(
    context: Context,
    private val tasks: List<Task>
) : ArrayAdapter<Task>(context, R.layout.list_item_checkbox, tasks) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.list_item_checkbox, parent, false)

        val checkBox = view.findViewById<CheckBox>(R.id.checkBox)

        val task = tasks[position]

        val dateText = formatDateTime(task)

        checkBox.text = "${task.event_name}\n$dateText"

        // ここで初期チェック状態を設定
        checkBox.isChecked = task.done

        // done=true のとき背景色を薄緑にする
        if (task.done) {
            view.setBackgroundColor(Color.parseColor("#CCFFCC"))
        } else {
            view.setBackgroundColor(Color.WHITE)
        }

        return view
    }

    fun formatDateTime(task: Task): String {

        val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val outputFormatter = DateTimeFormatter.ofPattern("MM/dd")

        val startDate = LocalDate.parse(task.start_date, inputFormatter)
            .format(outputFormatter)

        val endDate = LocalDate.parse(task.end_date, inputFormatter)
            .format(outputFormatter)

        val timeText = if (task.start_time.isNullOrEmpty()) {
            "時刻未定"
        } else {
            task.start_time.substring(0,5) // 10:40:00 → 10:40
        }

        return "$startDate $timeText ～ $endDate"
    }
}