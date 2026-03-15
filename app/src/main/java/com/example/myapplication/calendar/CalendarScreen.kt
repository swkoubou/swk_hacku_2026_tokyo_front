package com.example.myapplication.calendar

import kotlinx.coroutines.launch
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
import retrofit2.http.Header
import java.time.LocalDate
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.ui.platform.LocalContext
import java.util.*
import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.myapplication.UserConfig

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

enum class EventPosition { START, MIDDLE, END, SINGLE }

data class CalendarEventDisplay(
    val event: EventResponse,
    val position: EventPosition,
    val rowIndex: Int // 追加: その日の上から何番目に表示するか
)

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
@Composable
fun FullMonthCalendarScreen() {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://hackutokyo2026.yoimiya.net/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    val apiService = retrofit.create(ApiService::class.java)

    var eventsMap by remember { mutableStateOf<Map<LocalDate, List<CalendarEventDisplay>>>(emptyMap()) }
    val calendarState = rememberCalendarState()
    val userUuid = UserConfig.USER_UUID

    var detailDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedEvent by remember { mutableStateOf<EventResponse?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun refreshEvents() {
        scope.launch {
            try {
                val response = apiService.getYearEvents(userUuid, YearRequest("2026"))

                // 1. 重複排除（UUIDが同じものは1つにする）
                val uniqueEvents = response.distinctBy { it.task_uuid }
                    .sortedBy { LocalDate.parse(it.start_date) } // 開始順にソート

                val newMap = mutableMapOf<LocalDate, MutableList<CalendarEventDisplay>>()

                // 2. イベントごとに「何行目に表示するか」を決定する
                // 各日付でどの行が埋まっているかを管理するマップ
                val rowOccupancy = mutableMapOf<LocalDate, MutableSet<Int>>()

                uniqueEvents.forEach { event ->
                    val start = LocalDate.parse(event.start_date)
                    val end = LocalDate.parse(event.end_date)

                    // このイベントが入れる一番上の行を探す
                    var targetRow = 0
                    while (true) {
                        var isRowAvailable = true
                        var d = start
                        while (!d.isAfter(end)) {
                            if (rowOccupancy[d]?.contains(targetRow) == true) {
                                isRowAvailable = false
                                break
                            }
                            d = d.plusDays(1)
                        }
                        if (isRowAvailable) break else targetRow++
                    }

                    // 決定した行(targetRow)にイベントを配置
                    var current = start
                    while (!current.isAfter(end)) {
                        val pos = when {
                            start == end -> EventPosition.SINGLE
                            current == start -> EventPosition.START
                            current == end -> EventPosition.END
                            else -> EventPosition.MIDDLE
                        }

                        val list = newMap.getOrPut(current) { mutableListOf() }
                        list.add(CalendarEventDisplay(event, pos, targetRow))

                        // 行の使用状況を記録
                        rowOccupancy.getOrPut(current) { mutableSetOf() }.add(targetRow)
                        current = current.plusDays(1)
                    }
                }

                // 最後に表示順を rowIndex 順に確定させる
                eventsMap = newMap.mapValues { it.value.sortedBy { display -> display.rowIndex } }

            } catch (e: Exception) {
                Log.e("CalendarDebug", "Error", e)
            }
        }
    }

    LaunchedEffect(Unit) { refreshEvents() }

    Box(modifier = Modifier.fillMaxSize()) {
        if (detailDate == null) {
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
                            .border(0.2.dp, Color.LightGray) // 枠線
                            .clickable { detailDate = date } // クリックイベント
                            .padding(top = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // --- 1. 日付の数字部分 ---
                        Text(
                            text = date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCurrentMonth) Color.Unspecified else Color.LightGray,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )

                        // --- 2. イベントバー表示部分 ---
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // 最大3行分（または4行分）の枠を用意
                            (0..2).forEach { rowIndex ->
                                val display = dayEvents.find { it.rowIndex == rowIndex }

                                if (display != null) {
                                    val shape = when (display.position) {
                                        EventPosition.START -> RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)
                                        EventPosition.END -> RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
                                        EventPosition.MIDDLE -> RoundedCornerShape(0.dp)
                                        EventPosition.SINGLE -> RoundedCornerShape(4.dp)
                                    }

                                    // バー同士をピッタリくっつけるためのパディング調整
                                    val horizontalPadding = when (display.position) {
                                        EventPosition.START -> PaddingValues(start = 4.dp, end = 0.dp)
                                        EventPosition.END -> PaddingValues(start = 0.dp, end = 4.dp)
                                        EventPosition.MIDDLE -> PaddingValues(0.dp)
                                        EventPosition.SINGLE -> PaddingValues(horizontal = 4.dp)
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(18.dp)
                                            .padding(vertical = 1.dp)
                                            .padding(horizontalPadding),
                                        color = if (display.event.event_name.contains("旅行")) Color(0xFF81C784) else MaterialTheme.colorScheme.primaryContainer,
                                        shape = shape
                                    ) {
                                        // 開始日または単発の時だけ文字を出す
                                        if (display.position == EventPosition.START || display.position == EventPosition.SINGLE) {
                                            Text(
                                                text = display.event.event_name,
                                                fontSize = 9.sp,
                                                maxLines = 1,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                } else {
                                    // 予定がない行も Spacer で埋めて高さを一定にする
                                    Spacer(modifier = Modifier.height(18.dp).padding(vertical = 1.dp))
                                }
                            }

                            // 4件以上の表示
                            if (dayEvents.size > 3) {
                                Text(
                                    text = "+他${dayEvents.size - 3}件",
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }
            )
        } else {
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
                    dayEvents.forEach { display ->
                        // 修正3: display.event を使用
                        val event = display.event
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
                                supportingContent = {
                                    val timeLabel = if (display.position == EventPosition.SINGLE || display.position == EventPosition.START) {
                                        event.start_time ?: "時間未設定"
                                    } else {
                                        "（連日予定）"
                                    }
                                    Text(timeLabel)
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- (EditEventDialog の呼び出し部分は変更なし) ---
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
// --- 編集ダイアログ ---
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
    var endDate by remember { mutableStateOf(event.end_date) } // 終了日の状態を追加
    var startTime by remember { mutableStateOf(event.start_time ?: "09:00:00") }

    // 開始日選択：選んだら終了日を自動で +1日 する
    fun showStartDatePicker() {
        val dateParts = startDate.split("-").map { it.toInt() }
        DatePickerDialog(context, { _, year, month, dayOfMonth ->
            val selected = LocalDate.of(year, month + 1, dayOfMonth)
            startDate = selected.toString()
            // 開始日を変えたら、終了日は自動的にその翌日に設定する
            endDate = selected.plusDays(0).toString()
        }, dateParts[0], dateParts[1] - 1, dateParts[2]).show()
    }

    // 終了日選択：個別に編集可能
    fun showEndDatePicker() {
        val dateParts = endDate.split("-").map { it.toInt() }
        DatePickerDialog(context, { _, year, month, dayOfMonth ->
            endDate = LocalDate.of(year, month + 1, dayOfMonth).toString()
        }, dateParts[0], dateParts[1] - 1, dateParts[2]).show()
    }

    fun showTimePicker() {
        val timeParts = startTime.split(":").map { it.toInt() }
        TimePickerDialog(context, { _, hourOfDay, minute ->
            startTime = "%02d:%02d:00".format(hourOfDay, minute)
        }, timeParts[0], timeParts[1], true).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("イベントの編集") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("イベント名") }, modifier = Modifier.fillMaxWidth()
                )

                // 開始日
                OutlinedCard(onClick = { showStartDatePicker() }, modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("開始日") },
                        supportingContent = { Text(startDate) },
                        trailingContent = { Text("変更", color = MaterialTheme.colorScheme.primary) }
                    )
                }

                // 終了日 (追加)
                OutlinedCard(onClick = { showEndDatePicker() }, modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("終了日") },
                        supportingContent = { Text(endDate) },
                        trailingContent = { Text("変更", color = MaterialTheme.colorScheme.primary) }
                    )
                }

                OutlinedCard(onClick = { showTimePicker() }, modifier = Modifier.fillMaxWidth()) {
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
                    new_end_date = endDate, // 修正されたendDateを送信
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

