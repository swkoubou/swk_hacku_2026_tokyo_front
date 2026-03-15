package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import org.json.JSONObject
class ConfirmVoiceRecognition : AppCompatActivity() {

    private val messageAnalysis = MessageAnalysis()

    private var currentLv = 1
    private lateinit var message: String
    private lateinit var confirmText: TextView

    private lateinit var versionCountText: TextView

    private var startDateVar: String? = null
    private var startTimeVar: String? = null
    private var endDateVar: String? = null
    private var eventNameVar: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm_voice_recognition)

        message = intent.getStringExtra("message") ?: ""

        val eventName = intent.getStringExtra("event_name")
        val startDate = intent.getStringExtra("start_date")
        val startTime = intent.getStringExtra("start_time")
        val endDate = intent.getStringExtra("end_date")

        eventNameVar = eventName
        startDateVar = startDate
        startTimeVar = startTime
        endDateVar = endDate

        confirmText = findViewById(R.id.confirmText)
        versionCountText = findViewById(R.id.versionCountText)

        versionCountText.text = "あと2回バージョンアップできます"

        val registerButton = findViewById<Button>(R.id.registerButton)
        val cancelButton = findViewById<Button>(R.id.cancelButton)
        val versionUpButton = findViewById<Button>(R.id.versionUpButton)

        updateText(eventName, startDate, startTime, endDate)

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
                .addHeader("user_uuid", "3c7a9a24-9e34-4f65-bc1e-9a6e6c7d7f12")
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
                        val msg = if (success) "予定を追加しました" else "登録に失敗しました"
                        AlertDialog.Builder(this@ConfirmVoiceRecognition)
                            .setMessage(msg)
                            .setPositiveButton("OK") { _, _ ->
                                if (success) finishAffinity()
                            }
                            .show()
                    }
                }
            })
        }

        // 再録音
        cancelButton.setOnClickListener {
            finish()
        }

        // バージョンアップ
        versionUpButton.setOnClickListener {

            when (currentLv) {

                1 -> {
                    messageAnalysis.messageAnalysisLv2(message) { response ->

                        runOnUiThread {

                            updateText(
                                response.eventName,
                                response.startDate,
                                response.startTime,
                                response.endDate
                            )

                            eventNameVar = response.eventName
                            startDateVar = response.startDate
                            startTimeVar = response.startTime
                            endDateVar = response.endDate

                            currentLv = 2
                            versionCountText.text = "あと1回バージョンアップできます"
                        }
                    }
                }

                2 -> {
                    messageAnalysis.messageAnalysisLv3(message) { response ->

                        runOnUiThread {

                            updateText(
                                response.eventName,
                                response.startDate,
                                response.startTime,
                                response.endDate
                            )

                            eventNameVar = response.eventName
                            startDateVar = response.startDate
                            startTimeVar = response.startTime
                            endDateVar = response.endDate

                            currentLv = 3

                            versionUpButton.visibility = Button.GONE
                            versionCountText.visibility = TextView.GONE
                        }
                    }
                }
            }
        }
    }

    private fun updateText(
        eventName: String?,
        startDate: String?,
        startTime: String?,
        endDate: String?
    ) {

        confirmText.text = """
        イベント: ${eventName ?: ""}
        
        開始: ${startDate ?: ""} ${startTime ?: "00:00:00"}
        
        終了: ${endDate ?: ""}
        
        """.trimIndent()
    }
}