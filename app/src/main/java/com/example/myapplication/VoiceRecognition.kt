package com.example.myapplication

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.res.ColorStateList
import android.app.WallpaperManager
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.view.MotionEvent
import android.annotation.SuppressLint
import android.graphics.Color
import com.example.myapplication.calendar.CalendarActivity
import androidx.activity.enableEdgeToEdge


class VoiceRecognition : AppCompatActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var resultText: TextView
    private var isListening = false
    private var shouldAnalyzeWhenResultArrives = false
    private var isAnalyzing = false
    private var finalText = ""
    private val messageAnalysis by lazy { MessageAnalysis(this) }
    private lateinit var errorText: TextView
    private lateinit var recordButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_voice_recognition)
        WindowInsetsHelper.applySystemBarInsets(findViewById(R.id.main))

        //マイクの許可（権限）処理
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                1
            )
        }
        autoSetupWallpaperIfNeeded()

        val button = findViewById<Button>(R.id.button)
        recordButton = button
        resultText = findViewById(R.id.resultText)
        errorText = findViewById(R.id.errorText)
        updateRecordButtonAppearance(isRecording = false, isProcessing = false)

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")

        intent.putExtra(
            RecognizerIntent.EXTRA_PARTIAL_RESULTS,
            true
        )

        val toList = findViewById<Button>(R.id.toList)
        toList.setOnClickListener {
            val intent = Intent(this, ListActivity::class.java)
            startActivity(intent)
        }

        val toSetting = findViewById<Button>(R.id.toSetting)
        toSetting.setOnClickListener {
            val intent = Intent(this, FeatureSettingsActivity::class.java)
            startActivity(intent)
        }

        val toConfirm = findViewById<Button>(R.id.toConfirm)
        toConfirm.setOnClickListener {
            val intent = Intent(this, CalendarActivity::class.java)
            startActivity(intent)
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {

            override fun onResults(results: Bundle) {

                val matches =
                    results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                val text = matches?.get(0)

                if (text != null) {
                    finalText += text
                }

                resultText.text = finalText

                if (isListening) {
                    speechRecognizer.startListening(intent)
                } else if (shouldAnalyzeWhenResultArrives && !isAnalyzing) {
                    shouldAnalyzeWhenResultArrives = false
                    analyzeAfterRecognition()
                }
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {

                if (
                    error == SpeechRecognizer.ERROR_NO_MATCH ||
                    error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                    error == SpeechRecognizer.ERROR_CLIENT ||
                    error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY
                ) {

                    if (isListening) {
                        speechRecognizer.cancel()
                        speechRecognizer.startListening(intent)
                    } else if (shouldAnalyzeWhenResultArrives && !isAnalyzing) {
                        shouldAnalyzeWhenResultArrives = false
                        analyzeAfterRecognition()
                    }

                    return
                }

                errorText.text = "Error: $error"
                shouldAnalyzeWhenResultArrives = false
                isAnalyzing = false
                findViewById<Button>(R.id.button).text = "長押しして話す"
                updateRecordButtonAppearance(isRecording = false, isProcessing = false)
            }
            @SuppressLint("SetTextI18n")
            override fun onPartialResults(partialResults: Bundle?) {
                val matches =
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                val text = matches?.get(0)

                resultText.text = finalText + (text ?: "")
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        @SuppressLint("ClickableViewAccessibility")
        button.setOnTouchListener { v, event ->

            when (event.action) {

                MotionEvent.ACTION_DOWN -> {

                    finalText = ""
                    resultText.text = ""
                    errorText.text = ""
                    shouldAnalyzeWhenResultArrives = false
                    isAnalyzing = false

                    speechRecognizer.startListening(intent)
                    button.text = "認識中"
                    isListening = true
                    updateRecordButtonAppearance(isRecording = true, isProcessing = false)
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {

                    isListening = false
                    shouldAnalyzeWhenResultArrives = true
                    button.text = "認識結果を処理中..."
                    speechRecognizer.stopListening()
                    updateRecordButtonAppearance(isRecording = false, isProcessing = true)

                    v.performClick()
                }
            }

            false
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isListening && !isAnalyzing) {
            finalText = ""
            resultText.text = ""
            errorText.text = ""
            shouldAnalyzeWhenResultArrives = false
            recordButton.text = "長押しして話す"
            updateRecordButtonAppearance(isRecording = false, isProcessing = false)
        }
    }

    private fun autoSetupWallpaperIfNeeded() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_LIVE_WALLPAPER)) {
            return
        }

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val alreadyPrompted = prefs.getBoolean("wallpaper_auto_prompt_done", false)
        if (alreadyPrompted) {
            return
        }

        val wallpaperInfo = WallpaperManager.getInstance(this).wallpaperInfo
        val expectedServiceName = EventWallpaperService::class.java.name
        val isEventWallpaperActive = wallpaperInfo?.serviceName == expectedServiceName

        if (isEventWallpaperActive) {
            prefs.edit().putBoolean("wallpaper_auto_prompt_done", true).apply()
            return
        }

        prefs.edit().putBoolean("wallpaper_auto_prompt_done", true).apply()

        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this@VoiceRecognition, EventWallpaperService::class.java)
            )
        }
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
        }
    }

    private fun analyzeAfterRecognition() {
        val spokenText = finalText.trim()
        if (spokenText.isEmpty()) {
            findViewById<Button>(R.id.button).text = "長押しして話す"
            errorText.text = "音声を認識できませんでした"
            updateRecordButtonAppearance(isRecording = false, isProcessing = false)
            return
        }

        isAnalyzing = true
        messageAnalysis.messageAnalysisLv1(
            spokenText,
            callback = { response, elapsedMs ->
                runOnUiThread {
                    val intent = Intent(this, ConfirmVoiceRecognition::class.java)
                    intent.putExtra("lv", response.lv)
                    intent.putExtra("processing_elapsed_ms", elapsedMs)
                    intent.putExtra("input_message", spokenText)
                    intent.putExtra("message", response.message)
                    intent.putExtra("event_name", response.eventName)
                    intent.putExtra("start_date", response.startDate)
                    intent.putExtra("start_time", response.startTime)
                    intent.putExtra("end_date", response.endDate)
                    findViewById<Button>(R.id.button).text = "長押しして話す"
                    isAnalyzing = false
                    updateRecordButtonAppearance(isRecording = false, isProcessing = false)
                    startActivity(intent)
                }
            },
            onError = { error ->
                runOnUiThread {
                    findViewById<Button>(R.id.button).text = "長押しして話す"
                    errorText.text = "解析エラー: $error"
                    isAnalyzing = false
                    updateRecordButtonAppearance(isRecording = false, isProcessing = false)
                }
            }
        )
    }

    private fun updateRecordButtonAppearance(isRecording: Boolean, isProcessing: Boolean) {
        val color = when {
            isRecording -> Color.parseColor("#EF5350") // 録音中は赤系
            isProcessing -> Color.parseColor("#FB8C00") // 処理中はオレンジ系
            else -> Color.parseColor("#1976D2") // 通常は青系
        }
        recordButton.backgroundTintList = ColorStateList.valueOf(color)
    }

}