package com.example.myapplication

import android.Manifest
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

class VoiceRecognition : AppCompatActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var resultText: TextView
    private var isListening = false
    private var finalText = ""
    private val messageAnalysis = MessageAnalysis()
    private lateinit var errorText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_recognition)

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

        val button = findViewById<Button>(R.id.button)
        resultText = findViewById(R.id.resultText)
        errorText = findViewById(R.id.errorText)

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
                    }

                    return
                }

                errorText.text = "Error: $error"
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

                    speechRecognizer.startListening(intent)
                    button.text = "認識中"
                    isListening = true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {

                    speechRecognizer.stopListening()
                    button.text = "長押しして話す"
                    isListening = false

                    messageAnalysis.messageAnalysisLv1(finalText) { response ->

                        runOnUiThread {

                            val intent = Intent(this, ConfirmVoiceRecognition::class.java)

                            intent.putExtra("message", response.message)
                            intent.putExtra("event_name", response.eventName)
                            intent.putExtra("start_date", response.startDate)
                            intent.putExtra("start_time", response.startTime)
                            intent.putExtra("end_date", response.endDate)

                            startActivity(intent)
                        }
                    }

                    v.performClick()
                }
            }

            false
        }
    }
}