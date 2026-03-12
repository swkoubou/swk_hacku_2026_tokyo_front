package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog

class ConfirmVoiceRecognition : AppCompatActivity() {

    private val messageAnalysis = MessageAnalysis()

    private var currentLv = 1
    private lateinit var message: String
    private lateinit var confirmText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm_voice_recognition)

        message = intent.getStringExtra("message") ?: ""

        val eventName = intent.getStringExtra("event_name")
        val startDate = intent.getStringExtra("start_date")
        val startTime = intent.getStringExtra("start_time")
        val endDate = intent.getStringExtra("end_date")

        confirmText = findViewById(R.id.confirmText)

        val registerButton = findViewById<Button>(R.id.registerButton)
        val cancelButton = findViewById<Button>(R.id.cancelButton)
        val versionUpButton = findViewById<Button>(R.id.versionUpButton)

        updateText(eventName, startDate, startTime, endDate)

        // 登録
        registerButton.setOnClickListener {

            AlertDialog.Builder(this)
                .setMessage("予定を追加しました")
                .setPositiveButton("OK") { _, _ ->
                    finishAffinity()
                }
                .show()
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

                            currentLv = 2
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

                            currentLv = 3

                            versionUpButton.visibility = Button.GONE
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
            イベント: $eventName
            
            開始: $startDate $startTime
            
            終了: $endDate
        """.trimIndent()
    }
}