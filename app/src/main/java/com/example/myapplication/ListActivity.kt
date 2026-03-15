package com.example.myapplication

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
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
import java.time.format.DateTimeParseException
import java.time.LocalTime

open class ListActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private var isUpdating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        recyclerView = findViewById(R.id.recyclerView)
        taskAdapter = TaskAdapter { task ->
            if (isUpdating) return@TaskAdapter
            val userUuid = UuidManager.getUuid(this) ?: return@TaskAdapter
            isUpdating = true
            pushApi(userUuid, task.task_uuid, task.done) {
                runOnUiThread {
                    loadTaskList()
                    Handler(Looper.getMainLooper()).postDelayed({
                        isUpdating = false
                    }, 350)
                }
            }
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = taskAdapter
        recyclerView.itemAnimator = DefaultItemAnimator()
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        loadTaskList()
    }

    fun loadTaskList() {

        callApi { responseBody ->

            if (responseBody == null) return@callApi

            runOnUiThread {
                val taskList = parseJson(responseBody)
                taskAdapter.submitList(taskList)
            }
        }
    }

    fun callApi(callback: (String?) -> Unit) {

        val uuid = UuidManager.getUuid(this) ?: return

        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://hackutokyo2026.yoimiya.net/get_today_events")
            .post(ByteArray(0).toRequestBody(null))
            .addHeader("user_uuid", uuid)
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

        return taskList.sortedWith { a, b ->
            // 壁紙と同じ: 未達成を先、達成を後ろにする
            if (a.done != b.done) {
                return@sortedWith if (a.done) 1 else -1
            }

            // 壁紙と同じ: 未達成の中では当日締切+時刻ありを最優先
            if (!a.done) {
                val priorityCompare = getUndonePriority(a).compareTo(getUndonePriority(b))
                if (priorityCompare != 0) return@sortedWith priorityCompare
            }

            val byEndDate = compareByEndDate(a, b)
            if (byEndDate != 0) return@sortedWith byEndDate

            compareByTime(a, b)
        }
    }

    private fun getUndonePriority(task: Task): Int {
        if (task.done) return 99
        val endDate = parseDate(task.end_date)
        val dueToday = endDate != null && endDate.isEqual(LocalDate.now())
        val hasTime = parseTime(task.start_time) != null
        return when {
            dueToday && hasTime -> 0
            dueToday -> 1
            else -> 2
        }
    }

    private fun compareByEndDate(a: Task, b: Task): Int {
        val endDateA = parseDate(a.end_date)
        val endDateB = parseDate(b.end_date)
        if (endDateA != null && endDateB != null) return endDateA.compareTo(endDateB)
        if (endDateA == null && endDateB != null) return 1
        if (endDateA != null) return -1
        return 0
    }

    private fun compareByTime(a: Task, b: Task): Int {
        val timeA = parseTime(a.start_time)
        val timeB = parseTime(b.start_time)
        if (timeA == null && timeB == null) return 0
        if (timeA == null) return 1
        if (timeB == null) return -1
        return timeA.compareTo(timeB)
    }

    private fun parseDate(value: String?): LocalDate? {
        if (value.isNullOrBlank()) return null
        val v = value.trim()
        return try {
            LocalDate.parse(v)
        } catch (_: DateTimeParseException) {
            if (v.length >= 10) {
                try {
                    LocalDate.parse(v.substring(0, 10))
                } catch (_: DateTimeParseException) {
                    null
                }
            } else {
                null
            }
        }
    }

    private fun parseTime(value: String?): LocalTime? {
        if (value.isNullOrBlank()) return null
        val v = value.trim()
        return try {
            LocalTime.parse(v)
        } catch (_: DateTimeParseException) {
            if (v.length >= 5) {
                try {
                    LocalTime.parse(v.substring(0, 5))
                } catch (_: DateTimeParseException) {
                    null
                }
            } else {
                null
            }
        }
    }
}

class TaskAdapter(
    private val onTaskClick: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(DIFF) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).task_uuid.hashCode().toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_checkbox, parent, false)
        return TaskViewHolder(view, onTaskClick)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TaskViewHolder(
        itemView: View,
        private val onTaskClick: (Task) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)

        fun bind(task: Task) {
            checkBox.text = "${task.event_name}\n${formatDateTime(task)}"
            checkBox.isChecked = task.done
            itemView.setBackgroundColor(
                if (task.done) Color.parseColor("#CCFFCC") else Color.WHITE
            )
            itemView.setOnClickListener { onTaskClick(task) }
        }

        private fun formatDateTime(task: Task): String {
            val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val outputFormatter = DateTimeFormatter.ofPattern("MM/dd")
            val startDate = LocalDate.parse(task.start_date, inputFormatter).format(outputFormatter)
            val endDate = LocalDate.parse(task.end_date, inputFormatter).format(outputFormatter)
            val timeText = if (task.start_time.isNullOrEmpty()) {
                "時刻未定"
            } else {
                task.start_time.substring(0, 5)
            }
            return "$startDate $timeText ～ $endDate"
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Task>() {
            override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
                return oldItem.task_uuid == newItem.task_uuid
            }

            override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
                return oldItem == newItem
            }
        }
    }
}