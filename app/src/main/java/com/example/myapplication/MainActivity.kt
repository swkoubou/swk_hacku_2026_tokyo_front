package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import com.example.myapplication.calendar.ApiService
import com.example.myapplication.calendar.FullMonthCalendarScreen
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Retrofitのインスタンスを作成
        val retrofit = Retrofit.Builder()
            .baseUrl("https://hackutokyo2026.yoimiya.net/") // ベースURL
            .addConverterFactory(GsonConverterFactory.create()) // JSON変換用
            .build()

        // 2. ApiServiceの実体を作成
        val apiService = retrofit.create(ApiService::class.java)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // 3. 作成したapiServiceを画面に渡す
                    FullMonthCalendarScreen(apiService = apiService)
                }
            }
        }
    }
}