package com.example.myapplication.calendar

import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import retrofit2.http.Header
import java.time.LocalDate
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.ui.platform.LocalContext
import java.util.*

// --- データモデル ---
data class EventResponse(
    val event_name: String,
    val start_date: String,
    val start_time: String?,
    val end_date: String,
    val user_uuid: String,
    val task_uuid: String
)

data class YearRequest(val year: String)
data class UpdateEventRequest(
    val task_uuid: String,
    val new_start_date: String,
    val new_start_time: String,
    val new_end_date: String,
    val new_event_name: String
)
data class DeleteEventRequest(val task_uuid: String)
data class SuccessResponse(val success: Boolean)

// --- API ---
interface ApiService {
    @POST("get_year_events")
    suspend fun getYearEvents(
        @Header("user_uuid") uuid: String,
        @Body request: YearRequest
    ): List<EventResponse>

    @POST("update_event")
    suspend fun updateEvent(
        @Header("user_uuid") uuid: String,
        @Body request: UpdateEventRequest
    ): SuccessResponse

    @POST("delete_event")
    suspend fun deleteEvent(
        @Header("user_uuid") uuid: String,
        @Body request: DeleteEventRequest
    ): SuccessResponse
}

// --- メイン画面 ---
// --- メイン画面（月間カレンダー内の表示をリッチにする修正） ---
@Composable
fun FullMonthCalendarScreen(apiService: ApiService) {
    var eventsMap by remember { mutableStateOf<Map<LocalDate, List<EventResponse>>>(emptyMap()) }
    val calendarState = rememberCalendarState()
    val userUuid = "3c7a9a24-9e34-4f65-bc1e-9a6e6c7d7f12"

    var detailDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedEvent by remember { mutableStateOf<EventResponse?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun refreshEvents() {
        scope.launch {
            try {
                val response = apiService.getYearEvents(userUuid, YearRequest("2026"))
                eventsMap = response.mapNotNull { event ->
                    try { LocalDate.parse(event.start_date) to event } catch (e: Exception) { null }
                }.groupBy({ it.first }, { it.second })
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    LaunchedEffect(Unit) { refreshEvents() }

    Box(modifier = Modifier.fillMaxSize()) {
        if (detailDate == null) {
            // カレンダーモード
            StaticCalendar(
                calendarState = calendarState,
                modifier = Modifier.fillMaxSize(),
                dayContent = { dayState ->
                    val date = dayState.date
                    val dayEvents = eventsMap[date] ?: emptyList()
                    // 今月以外の日付を薄くするための判定
                    val isCurrentMonth = date.month == calendarState.monthState.currentMonth.month

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp) // マスの最小高さを確保
                            .border(0.2.dp, Color.LightGray)
                            .clickable { detailDate = date }
                            .padding(2.dp),
                        horizontalAlignment = Alignment.Start // 左寄せの方が予定が見やすい
                    ) {
                        // 日付の数字
                        Text(
                            text = date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCurrentMonth) Color.Unspecified else Color.LightGray,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // 予定の表示（最大3件まで）
                        dayEvents.take(3).forEach { event ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(2.dp)
                            ) {
                                Text(
                                    text = event.event_name,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    lineHeight = 10.sp,
                                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp)
                                )
                            }
                        }

                        // 4件以上ある場合は「+他○件」と表示
                        if (dayEvents.size > 3) {
                            Text(
                                text = "+他${dayEvents.size - 3}件",
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    }
                }
            )
        } else {
            // ...（前回提示した詳細モードのコードをそのまま使用）
            // 戻るボタンなどがある詳細画面のロジック
            val targetDate = detailDate!!
            val dayEvents = eventsMap[targetDate] ?: emptyList()

            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { detailDate = null }) { Text("← カレンダーに戻る") }
                }
                Text(
                    text = "${targetDate.year}年${targetDate.monthValue}月${targetDate.dayOfMonth}日",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                if (dayEvents.isEmpty()) {
                    Text("予定はありません", modifier = Modifier.padding(top = 16.dp))
                } else {
                    dayEvents.forEach { event ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    selectedEvent = event
                                    showEditDialog = true
                                }
                        ) {
                            ListItem(
                                headlineContent = { Text(event.event_name) },
                                supportingContent = { Text(event.start_time ?: "時間未設定") }
                            )
                        }
                    }
                }
            }
        }

        // 編集ダイアログ（前回提示したコードをそのまま末尾に保持してください）
        if (showEditDialog && selectedEvent != null) {
            EditEventDialog(
                event = selectedEvent!!,
                onDismiss = { showEditDialog = false },
                onUpdate = { updatedRequest ->
                    scope.launch {
                        try {
                            val res = apiService.updateEvent(userUuid, updatedRequest)
                            if (res.success) {
                                refreshEvents()
                                showEditDialog = false
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                },
                onDelete = { uuid ->
                    scope.launch {
                        try {
                            val res = apiService.deleteEvent(userUuid, DeleteEventRequest(uuid))
                            if (res.success) {
                                refreshEvents()
                                showEditDialog = false
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
            )
        }
    }
}
// --- 編集ダイアログ（ここに定義がないと Unresolved reference になります） ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventDialog(
    event: EventResponse,
    onDismiss: () -> Unit,
    onUpdate: (UpdateEventRequest) -> Unit,
    onDelete: (String) -> Unit
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf(event.event_name) }
    var startDate by remember { mutableStateOf(event.start_date) }
    var startTime by remember { mutableStateOf(event.start_time ?: "09:00:00") }

    // 日付選択ダイアログを表示する関数
    fun showDatePicker() {
        val dateParts = startDate.split("-").map { it.toInt() }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                // 月は 0-indexed なので +1 する。フォーマットを YYYY-MM-DD に整える
                startDate = "%04d-%02d-%02d".format(year, month + 1, dayOfMonth)
            },
            dateParts[0], dateParts[1] - 1, dateParts[2]
        ).show()
    }

    // 時刻選択ダイアログを表示する関数
    fun showTimePicker() {
        val timeParts = startTime.split(":").map { it.toInt() }
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                // フォーマットを HH:mm:ss に整える
                startTime = "%02d:%02d:00".format(hourOfDay, minute)
            },
            timeParts[0], timeParts[1], true // 24時間表記
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("イベントの編集") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("イベント名") },
                    modifier = Modifier.fillMaxWidth()
                )

                // 日付選択ボタン
                OutlinedCard(
                    onClick = { showDatePicker() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListItem(
                        headlineContent = { Text("日付") },
                        supportingContent = { Text(startDate) },
                        trailingContent = { Text("変更", color = MaterialTheme.colorScheme.primary) }
                    )
                }

                // 時刻選択ボタン
                OutlinedCard(
                    onClick = { showTimePicker() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListItem(
                        headlineContent = { Text("時刻") },
                        supportingContent = { Text(startTime) },
                        trailingContent = { Text("変更", color = MaterialTheme.colorScheme.primary) }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onUpdate(UpdateEventRequest(
                    task_uuid = event.task_uuid,
                    new_start_date = startDate,
                    new_start_time = startTime,
                    new_end_date = event.end_date, // 必要に応じて end_date も同様に実装可能
                    new_event_name = name
                ))
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = { onDelete(event.task_uuid) }) {
                Text("削除", color = Color.Red)
            }
        }
    )
}