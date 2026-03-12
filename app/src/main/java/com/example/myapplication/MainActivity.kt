package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent // gradle修正後に有効になります
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.boguszpawlowski.composecalendar.StaticCalendar
import io.github.boguszpawlowski.composecalendar.rememberCalendarState
import io.github.boguszpawlowski.composecalendar.selection.DynamicSelectionState
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CalendarMainScreen()
                }
            }
        }
    }
}

@Composable
fun CalendarMainScreen() {
    // 状態管理
    val calendarState = rememberCalendarState()

    // キャストエラー回避のため、型を特定せずに selection を取得する書き方に変更
    val selectedDate = (calendarState.selectionState as? DynamicSelectionState)
        ?.selection
        ?.firstOrNull()
        ?: LocalDate.now()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1.2f).padding(8.dp)) {
            StaticCalendar(
                calendarState = calendarState,
                horizontalSwipeEnabled = true
            )
        }
        HorizontalDivider()
        Box(modifier = Modifier.weight(1f).padding(16.dp)) {
            ScheduleListSection(selectedDate)
        }
    }
}

@Composable
fun ScheduleListSection(date: LocalDate) {
    val dummySchedules = listOf("10:00 会議", "12:30 ランチ", "19:00 ジム")
    Column {
        Text(text = "${date.monthValue}月${date.dayOfMonth}日の予定", style = MaterialTheme.typography.titleLarge)
        LazyColumn {
            items(dummySchedules) { schedule ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(text = schedule, modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}