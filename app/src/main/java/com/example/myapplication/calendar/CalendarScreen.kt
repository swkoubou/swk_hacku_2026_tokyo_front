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
import androidx.compose.ui.unit.sp
import io.github.boguszpawlowski.composecalendar.StaticCalendar
import io.github.boguszpawlowski.composecalendar.rememberCalendarState
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import java.time.LocalDate
import java.time.format.DateTimeParseException
import retrofit2.http.Header

data class EventResponse(
    val event_name: String,
    val start_date: String,
    val start_time: String?,
    val end_date: String,
    val user_uuid: String,
    val task_uuid: String
)

data class YearRequest(
    val year: String
)

interface ApiService {
    // 仕様に合わせてエンドポイントを修正し、Headerでuuidを受け取るように変更
    @POST("get_year_events")
    suspend fun getYearEvents(
        @Header("user_uuid") uuid: String,
        @Body request: YearRequest
    ): List<EventResponse>
}

// --- 2. カレンダー画面のメイン実装 ---

@Composable
fun FullMonthCalendarScreen(apiService: ApiService) {
    // 状態管理（APIから取得したイベントを日付ごとにMapで保持）
    // 型推論を助けるために型を明示的に指定
    var eventsMap by remember { mutableStateOf<Map<LocalDate, List<EventResponse>>>(emptyMap()) }
    val calendarState = rememberCalendarState()

    LaunchedEffect(Unit) {
        try {
            val response = apiService.getYearEvents(
                "3c7a9a24-9e34-4f65-bc1e-9a6e6c7d7f12",
                YearRequest("2026")
            )

            // 型安全な変換方法
            eventsMap = response.mapNotNull { event ->
                try {
                    val date = LocalDate.parse(event.start_date)
                    date to event
                } catch (e: DateTimeParseException) {
                    null
                }
            }.groupBy({ it.first }, { it.second }) // Pairの1つ目をキー、2つ目を値としてグループ化

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        StaticCalendar(
            calendarState = calendarState,
            modifier = Modifier.fillMaxSize(),
            dayContent = { dayState ->
                val date = dayState.date
                val dayEvents = eventsMap[date] ?: emptyList()
                val isCurrentMonth = date.month == calendarState.monthState.currentMonth.month

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp)
                        .border(0.2.dp, Color.LightGray)
                        .clickable {
                            calendarState.selectionState.onDateSelected(date)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCurrentMonth) MaterialTheme.colorScheme.onSurface else Color.LightGray,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    // 予定を表示
                    dayEvents.take(5).forEach { event ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp, vertical = 1.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text(
                                text = event.event_name,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }
                }
            }
        )
    }
}