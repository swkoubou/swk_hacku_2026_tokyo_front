package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import org.json.JSONObject

class ConfirmVoiceRecognition : AppCompatActivity() {

    private val messageAnalysis by lazy { MessageAnalysis(this) }

    private var currentLv = 1
    private lateinit var message: String
    private lateinit var inputMessageText: TextView
    private lateinit var eventValueText: TextView
    private lateinit var startDateValueText: TextView
    private lateinit var startTimeValueText: TextView
    private lateinit var endDateValueText: TextView
    private lateinit var metaInfoText: TextView

    private lateinit var versionCountText: TextView
    private lateinit var versionUpButton: Button
    private lateinit var editResultButton: Button
    private lateinit var loadingContainer: LinearLayout

    private var startDateVar: String? = null
    private var startTimeVar: String? = null
    private var endDateVar: String? = null
    private var eventNameVar: String? = null
    private var processingElapsedMs: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_confirm_voice_recognition)
        WindowInsetsHelper.applySystemBarInsets(findViewById(R.id.main))

        message = intent.getStringExtra("message") ?: ""

        val eventName = intent.getStringExtra("event_name")
        val startDate = intent.getStringExtra("start_date")
        val startTime = intent.getStringExtra("start_time")
        val endDate = intent.getStringExtra("end_date")
        val inputVoiceMessage = intent.getStringExtra("input_message") ?: message
        currentLv = intent.getIntExtra("lv", 1)
        processingElapsedMs = intent.getLongExtra("processing_elapsed_ms", -1L)

        eventNameVar = eventName
        startDateVar = startDate
        startTimeVar = startTime
        endDateVar = endDate

        inputMessageText = findViewById(R.id.tvInputMessage)
        eventValueText = findViewById(R.id.tvEventValue)
        startDateValueText = findViewById(R.id.tvStartDateValue)
        startTimeValueText = findViewById(R.id.tvStartTimeValue)
        endDateValueText = findViewById(R.id.tvEndDateValue)
        metaInfoText = findViewById(R.id.tvMetaInfo)
        versionCountText = findViewById(R.id.versionCountText)
        versionUpButton = findViewById(R.id.versionUpButton)
        editResultButton = findViewById(R.id.editResultButton)
        loadingContainer = findViewById(R.id.loadingContainer)

        updateVersionCountText()
        updateActionButtons()

        val registerButton = findViewById<Button>(R.id.registerButton)
        val cancelButton = findViewById<Button>(R.id.cancelButton)
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        inputMessageText.text = "入力音声: ${inputVoiceMessage.ifBlank { "（なし）" }}"
        updateText(currentLv, eventName, startDate, startTime, endDate, processingElapsedMs)

        // 登録
        registerButton.setOnClickListener {

            // JSONボディ作成
            val json = JSONObject().apply {
                put("start_date", startDateVar)
                put("start_time", startTimeVar)
                put("end_date", endDateVar)
                put("event_name", eventNameVar)
            }

            val body = json.toString().toRequestBody("application/json".toMediaType())

            // リクエスト作成
            val request = Request.Builder()
                .url("https://hackutokyo2026.yoimiya.net/def_event")
                .addHeader("user_uuid", UuidManager.getUuid(this) ?: "")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            // OkHttpClientで非同期送信
            val client = OkHttpClient()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                    runOnUiThread {
                        AlertDialog.Builder(this@ConfirmVoiceRecognition)
                            .setMessage("登録に失敗しました")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val resBody = response.body?.string() ?: ""
                    val success = try {
                        JSONObject(resBody).optBoolean("success", false)
                    } catch (e: Exception) {
                        false
                    }

                    runOnUiThread {
                        if (success) {
                            // 音声入力フローでは追加成功後の確認ダイアログを出さない
                            finishAffinity()
                        } else {
                            AlertDialog.Builder(this@ConfirmVoiceRecognition)
                                .setMessage("登録に失敗しました")
                                .setPositiveButton("OK", null)
                                .show()
                        }
                    }
                }
            })
        }

        // 再録音
        cancelButton.setOnClickListener {
            finish()
        }

        editResultButton.setOnClickListener {
            showEditResultDialog()
        }

        // バージョンアップ
        versionUpButton.setOnClickListener {

            when (currentLv) {

                1 -> {
                    setLoading(true)
                    messageAnalysis.messageAnalysisLv2(
                        message,
                        callback = { response, elapsedMs ->
                            runOnUiThread {
                                setLoading(false)
                                updateText(
                                    response.lv,
                                    response.eventName,
                                    response.startDate,
                                    response.startTime,
                                    response.endDate,
                                    elapsedMs
                                )

                                eventNameVar = response.eventName
                                startDateVar = response.startDate
                                startTimeVar = response.startTime
                                endDateVar = response.endDate

                                currentLv = response.lv
                                processingElapsedMs = elapsedMs
                                updateVersionCountText()
                                updateActionButtons()
                            }
                        },
                        onError = { error ->
                            runOnUiThread {
                                setLoading(false)
                                AlertDialog.Builder(this)
                                    .setMessage("再生成に失敗しました: $error")
                                    .setPositiveButton("OK", null)
                                    .show()
                            }
                        }
                    )
                }

                2 -> {
                    setLoading(true)
                    messageAnalysis.messageAnalysisLv3(
                        message,
                        callback = { response, elapsedMs ->
                            runOnUiThread {
                                setLoading(false)
                                updateText(
                                    response.lv,
                                    response.eventName,
                                    response.startDate,
                                    response.startTime,
                                    response.endDate,
                                    elapsedMs
                                )

                                eventNameVar = response.eventName
                                startDateVar = response.startDate
                                startTimeVar = response.startTime
                                endDateVar = response.endDate

                                currentLv = response.lv
                                processingElapsedMs = elapsedMs
                                updateVersionCountText()
                                updateActionButtons()
                            }
                        },
                        onError = { error ->
                            runOnUiThread {
                                setLoading(false)
                                AlertDialog.Builder(this)
                                    .setMessage("再生成に失敗しました: $error")
                                    .setPositiveButton("OK", null)
                                    .show()
                            }
                        }
                    )
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        loadingContainer.visibility = if (loading) LinearLayout.VISIBLE else LinearLayout.GONE
        versionUpButton.isEnabled = !loading
        editResultButton.isEnabled = !loading
        versionUpButton.text = if (loading) "再生成中..." else "処理レベルを上げて再生成する"
    }

    private fun updateActionButtons() {
        editResultButton.visibility = Button.VISIBLE
        if (currentLv >= 3) {
            versionUpButton.visibility = Button.GONE
            versionCountText.text = "最終レベルです。必要なら編集してください"
        } else {
            versionUpButton.visibility = Button.VISIBLE
        }
    }

    private fun updateVersionCountText() {
        val remaining = (3 - currentLv).coerceAtLeast(0)
        versionCountText.text = when (remaining) {
            0 -> "これ以上バージョンアップできません"
            1 -> "あと1回バージョンアップできます"
            else -> "あと${remaining}回バージョンアップできます"
        }
    }

    private fun updateText(
        lv: Int,
        eventName: String?,
        startDate: String?,
        startTime: String?,
        endDate: String?,
        elapsedMs: Long
    ) {
        val elapsedLabel = if (elapsedMs >= 0) "${elapsedMs}ms" else "--"
        eventValueText.text = "イベント: ${eventName ?: ""}"
        startDateValueText.text = "開始: ${startDate ?: ""}"
        startTimeValueText.text = "時間: ${formatDisplayTime(startTime)}"
        endDateValueText.text = "終了: ${endDate ?: ""}"
        metaInfoText.text = "処理レベル: $lv / 処理時間: $elapsedLabel"
    }

    private fun showEditResultDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 24, 40, 0)
        }

        val eventInput = EditText(this).apply {
            hint = "イベント名"
            setText(eventNameVar.orEmpty())
        }
        val startDateInput = EditText(this).apply {
            hint = "開始日 (YYYY-MM-DD)"
            setText(startDateVar.orEmpty())
        }
        val startTimeInput = EditText(this).apply {
            hint = "時間 (HH:mm) 未設定なら空欄"
            setText(formatEditableTime(startTimeVar))
        }
        val endDateInput = EditText(this).apply {
            hint = "終了日 (YYYY-MM-DD)"
            setText(endDateVar.orEmpty())
        }

        container.addView(eventInput)
        container.addView(startDateInput)
        container.addView(startTimeInput)
        container.addView(endDateInput)

        AlertDialog.Builder(this)
            .setTitle("認識結果を編集")
            .setView(container)
            .setPositiveButton("保存") { _, _ ->
                eventNameVar = eventInput.text?.toString()?.trim().orEmpty()
                startDateVar = startDateInput.text?.toString()?.trim().orEmpty()
                val editedTime = startTimeInput.text?.toString()?.trim().orEmpty()
                startTimeVar = normalizeTimeForApi(editedTime)
                endDateVar = endDateInput.text?.toString()?.trim().orEmpty()
                updateText(
                    currentLv,
                    eventNameVar,
                    startDateVar,
                    startTimeVar,
                    endDateVar,
                    processingElapsedMs
                )
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun formatDisplayTime(time: String?): String {
        if (time.isNullOrBlank()) return "未設定"
        return if (time.length >= 5) time.substring(0, 5) else time
    }

    private fun formatEditableTime(time: String?): String {
        if (time.isNullOrBlank()) return ""
        return if (time.length >= 5) time.substring(0, 5) else time
    }

    private fun normalizeTimeForApi(input: String): String? {
        if (input.isBlank()) return null
        val trimmed = input.trim()
        return when (trimmed.length) {
            5 -> "$trimmed:00" // HH:mm -> HH:mm:00
            else -> trimmed
        }
    }
}