package com.example.myapplication.calendar

import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import android.app.Activity
import java.util.*
import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.myapplication.UuidManager

private fun formatDisplayTime(time: String?): String {
    if (time.isNullOrBlank()) return "時間未設定"
    return if (time.length >= 5) time.substring(0, 5) else time
}

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
data class DefEventRequest(
    val start_date: String,
    val start_time: String?,
    val end_date: String,
    val event_name: String
)
data class UpdateEventRequest(
    val task_uuid: String,
    val new_start_date: String,
    val new_start_time: String?,
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

    @POST("def_event")
    suspend fun defEvent(
        @Header("user_uuid") uuid: String,
        @Body request: DefEventRequest
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
    // Contextを取得
    val context = LocalContext.current

    // UUIDを取得（String? 型）
    val savedUuid = UuidManager.getUuid(context)

    // UUIDがnullだった場合のデフォルト値を設定、または空文字にする
    val userUuid = savedUuid ?: ""

    if (userUuid.isEmpty()) {
        Log.e("CalendarDebug", "UUIDが見つかりません。設定画面で登録してください。")
    }

    var detailDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedEvent by remember { mutableStateOf<EventResponse?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddSuccessDialog by remember { mutableStateOf(false) }
    val today = LocalDate.now()

    val scope = rememberCoroutineScope()

    BackHandler(enabled = detailDate != null) {
        detailDate = null
    }

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { (context as? Activity)?.finish() }) {
                    Text("← 戻る")
                }
            }
            StaticCalendar(
                calendarState = calendarState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 56.dp),
                dayContent = { dayState ->
                    val date = dayState.date
                    val dayEvents = eventsMap[date] ?: emptyList()
                    val isToday = date == today
                    // 今月以外の日付を薄くするための判定
                    val isCurrentMonth = date.month == calendarState.monthState.currentMonth.month

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 124.dp) // 表示件数を増やすため少し広げる
                            .background(
                                color = if (isToday) Color(0xFFFFF7D6) else Color.Transparent,
                                shape = RoundedCornerShape(4.dp)
                            )
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
                            // 最大5行分を表示
                            (0..4).forEach { rowIndex ->
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
                                            .height(16.dp)
                                            .padding(vertical = 1.dp)
                                            .padding(horizontalPadding),
                                        color = MaterialTheme.colorScheme.primaryContainer,
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
                                    Spacer(modifier = Modifier.height(16.dp).padding(vertical = 1.dp))
                                }
                            }

                            // 6件以上の表示
                            if (dayEvents.size > 5) {
                                Text(
                                    text = "+他${dayEvents.size - 5}件",
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

            Column(modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)
                ) {
                    Button(onClick = { detailDate = null }) { Text("← カレンダーに戻る") }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    item {
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "${targetDate.year}年${targetDate.monthValue}月${targetDate.dayOfMonth}日",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "この日の予定 ${dayEvents.size}件",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    if (dayEvents.isEmpty()) {
                        item {
                            Text(
                                "予定はありません",
                                modifier = Modifier.padding(top = 16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(dayEvents) { display ->
                            val event = display.event
                            val timeLabel = formatDisplayTime(event.start_time)
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable {
                                        selectedEvent = event
                                        showEditDialog = true
                                    }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = event.event_name,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "開始: ${event.start_date}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                    Text(
                                        text = "時間: $timeLabel",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    Text(
                                        text = "終了: ${event.end_date}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    Text(
                                        text = "タップで編集",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
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

        if (showAddDialog) {
            AddEventDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { request ->
                    scope.launch {
                        try {
                            val res = apiService.defEvent(userUuid, request)
                            if (res.success) {
                                refreshEvents()
                                showAddDialog = false
                                showAddSuccessDialog = true
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            )
        }

        if (showAddSuccessDialog) {
            AlertDialog(
                onDismissRequest = { showAddSuccessDialog = false },
                title = { Text("予定を追加しました") },
                text = { Text("カレンダーに反映しました。内容を確認してください。") },
                confirmButton = {
                    TextButton(onClick = { showAddSuccessDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("+")
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
    var startTime by remember { mutableStateOf(event.start_time) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // 開始日選択：終了日は自動変更しない
    fun showStartDatePicker() {
        val dateParts = startDate.split("-").map { it.toInt() }
        DatePickerDialog(context, { _, year, month, dayOfMonth ->
            val selected = LocalDate.of(year, month + 1, dayOfMonth)
            startDate = selected.toString()
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
        val fallbackTime = java.time.LocalTime.now()
        val timeSource = startTime ?: String.format("%02d:%02d:00", fallbackTime.hour, fallbackTime.minute)
        val timeParts = timeSource.split(":").map { it.toInt() }
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
                        supportingContent = { Text(formatDisplayTime(startTime)) },
                        trailingContent = { Text("変更", color = MaterialTheme.colorScheme.primary) }
                    )
                }

                TextButton(
                    onClick = { startTime = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("時間未設定にする")
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
            TextButton(onClick = { showDeleteConfirm = true }) {
                Text("削除", color = Color.Red)
            }
        }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("予定を削除しますか？") },
            text = { Text("この操作は元に戻せません。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete(event.task_uuid)
                }) {
                    Text("削除", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(
    onDismiss: () -> Unit,
    onAdd: (DefEventRequest) -> Unit
) {
    val context = LocalContext.current
    val today = LocalDate.now()

    var name by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(today.toString()) }
    var endDate by remember { mutableStateOf(today.toString()) }
    var startTime by remember { mutableStateOf<String?>(null) }

    fun showStartDatePicker() {
        val dateParts = startDate.split("-").map { it.toInt() }
        DatePickerDialog(context, { _, year, month, dayOfMonth ->
            startDate = LocalDate.of(year, month + 1, dayOfMonth).toString()
        }, dateParts[0], dateParts[1] - 1, dateParts[2]).show()
    }

    fun showEndDatePicker() {
        val dateParts = endDate.split("-").map { it.toInt() }
        DatePickerDialog(context, { _, year, month, dayOfMonth ->
            endDate = LocalDate.of(year, month + 1, dayOfMonth).toString()
        }, dateParts[0], dateParts[1] - 1, dateParts[2]).show()
    }

    fun showTimePicker() {
        val fallbackTime = java.time.LocalTime.now()
        val timeSource = startTime ?: String.format("%02d:%02d:00", fallbackTime.hour, fallbackTime.minute)
        val timeParts = timeSource.split(":").map { it.toInt() }
        TimePickerDialog(context, { _, hourOfDay, minute ->
            startTime = "%02d:%02d:00".format(hourOfDay, minute)
        }, timeParts[0], timeParts[1], true).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("予定を追加") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("イベント名") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedCard(onClick = { showStartDatePicker() }, modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("開始日") },
                        supportingContent = { Text(startDate) },
                        trailingContent = { Text("変更", color = MaterialTheme.colorScheme.primary) }
                    )
                }

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
                        supportingContent = { Text(formatDisplayTime(startTime)) },
                        trailingContent = { Text("変更", color = MaterialTheme.colorScheme.primary) }
                    )
                }

                TextButton(
                    onClick = { startTime = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("時間未設定にする")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(
                            DefEventRequest(
                                start_date = startDate,
                                start_time = startTime,
                                end_date = endDate,
                                event_name = name.trim()
                            )
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("追加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

