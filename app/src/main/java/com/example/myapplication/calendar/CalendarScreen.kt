package com.example.myapplication.calendar

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.boguszpawlowski.composecalendar.StaticCalendar
import io.github.boguszpawlowski.composecalendar.rememberCalendarState
import java.time.LocalDate

@Composable
fun FullMonthCalendarScreen() {
    val calendarState = rememberCalendarState()

    // サンプルの予定データ（本来はViewModelなどで管理）
    val scheduleMap = mapOf(
        LocalDate.now() to listOf("会議", "飲み会"),
        LocalDate.now().plusDays(1) to listOf("ジム"),
        LocalDate.now().plusDays(3) to listOf("バイト", "買い物")
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // 月の切り替えなどはデフォルトのヘッダーが処理してくれます
        StaticCalendar(
            calendarState = calendarState,
            modifier = Modifier.fillMaxSize(),
            // 日付セルの見た目をフルカスタム
            dayContent = { dayState ->
                val date = dayState.date
                val schedules = scheduleMap[date] ?: emptyList()

                // 当月以外の日付を薄くするための判定
                // ライブラリのバージョンにより dayState.isFromCurrentMonth など名称が異なる場合があるため
                // calendarStateから取得した表示月と比較するのが確実です
                val isCurrentMonth = date.month == calendarState.monthState.currentMonth.month

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(130.dp) // 高さを出してGoogleカレンダー風に
                        .border(0.2.dp, Color.LightGray)
                        .clickable {
                            // タップした時の処理（例：選択状態にする）
                            calendarState.selectionState.onDateSelected(date)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 日付の数字
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCurrentMonth)
                            MaterialTheme.colorScheme.onSurface
                        else
                            Color.LightGray,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    // 予定のラベル表示（最大3つ程度）
                    schedules.take(5).forEach { title ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp, vertical = 1.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 2.dp),
                                fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.8f // 少し小さく
                            )
                        }
                    }
                }
            }
        )
    }
}