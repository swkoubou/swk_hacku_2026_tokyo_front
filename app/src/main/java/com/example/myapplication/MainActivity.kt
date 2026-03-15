package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable // これが必要
import androidx.compose.ui.Alignment // これが必要
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp // これが必要
import androidx.navigation.compose.NavHost // これが必要
import androidx.navigation.compose.composable // これが必要
import androidx.navigation.compose.rememberNavController // これが必要
import com.example.myapplication.calendar.ApiService
import com.example.myapplication.calendar.FullMonthCalendarScreen
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://hackutokyo2026.yoimiya.net/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(ApiService::class.java)

        setContent {
            MaterialTheme {
                // 1. 通り道を管理するコントローラー
                val navController = rememberNavController()

                // 2. ナビゲーションの定義
                NavHost(navController = navController, startDestination = "home") {

                    // ホーム画面
                    composable("home") {
                        HomeScreen(onCalendarClick = {
                            navController.navigate("calendar") // カレンダーへ飛ぶ
                        })
                    }

                    // カレンダー画面
                    composable("calendar") {
                        FullMonthCalendarScreen(apiService = apiService)
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(onCalendarClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "ホーム画面", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = onCalendarClick) {
            Text("カレンダーを表示する")
        }
    }
}