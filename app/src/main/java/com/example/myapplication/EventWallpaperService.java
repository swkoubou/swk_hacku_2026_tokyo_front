package com.example.myapplication;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.service.wallpaper.WallpaperService;
import android.text.TextUtils;
import android.view.SurfaceHolder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EventWallpaperService extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new EventWallpaperEngine();
    }

    private class EventWallpaperEngine extends Engine {
        private static final String ENDPOINT = "https://hackutokyo2026.yoimiya.net/get_today_events";
        private final DateTimeFormatter displayDateFormatter = DateTimeFormatter.ofPattern("MM/dd");
        private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        private final Paint titlePaint = new Paint();
        private final Paint headerMetaPaint = new Paint();
        private final Paint eventNamePaint = new Paint();
        private final Paint eventMetaPaint = new Paint();
        private final Paint doneEventNamePaint = new Paint();
        private final Paint doneEventMetaPaint = new Paint();
        private final Paint undoneCountPaint = new Paint();
        private final Paint statusPaint = new Paint();
        private final Paint panelPaint = new Paint();
        private final Paint headerPaint = new Paint();
        private final Paint cardPaint = new Paint();
        private final Paint doneCardPaint = new Paint();
        private final Paint cardStrokePaint = new Paint();
        private final Paint doneCardStrokePaint = new Paint();
        private final Paint dueTodayCardStrokePaint = new Paint();
        private final Paint dueTodayAccentPaint = new Paint();
        private final Paint doneAccentPaint = new Paint();
        private final Paint accentPaint = new Paint();
        private final Handler mainHandler = new Handler(Looper.getMainLooper());
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        private final Runnable dateChangeWatcher = new Runnable() {
            @Override
            public void run() {
                if (!visible) {
                    return;
                }
                if (shouldRefetchForDateChange()) {
                    statusMessage = "日付が変わったため再取得中...";
                    drawFrame();
                    fetchEvents();
                }
                mainHandler.postDelayed(this, 10_000L);
            }
        };
        private final List<EventItem> latestEvents = new ArrayList<>();
        private String latestFetchTime = "--:--:--";
        private String latestFetchedDate = "";
        private String statusMessage = "壁紙表示待ち...";
        private boolean visible;
        private boolean isFetching;
        private int backgroundColor = Color.parseColor(WallpaperSettings.DEFAULT_COLOR_BACKGROUND);

        EventWallpaperEngine() {
            titlePaint.setColor(Color.WHITE);
            titlePaint.setTextSize(56f);
            titlePaint.setAntiAlias(true);
            titlePaint.setFakeBoldText(true);

            headerMetaPaint.setColor(Color.parseColor("#C7D2FE"));
            headerMetaPaint.setTextSize(36f);
            headerMetaPaint.setAntiAlias(true);

            eventNamePaint.setColor(Color.WHITE);
            eventNamePaint.setTextSize(46f);
            eventNamePaint.setAntiAlias(true);

            eventMetaPaint.setColor(Color.parseColor("#C7D2FE"));
            eventMetaPaint.setTextSize(36f);
            eventMetaPaint.setAntiAlias(true);

            doneEventNamePaint.set(eventNamePaint);
            doneEventNamePaint.setStrikeThruText(true);

            doneEventMetaPaint.set(eventMetaPaint);
            doneEventMetaPaint.setStrikeThruText(true);

            undoneCountPaint.setColor(Color.parseColor(WallpaperSettings.DEFAULT_COLOR_UNDONE_COUNT));
            undoneCountPaint.setTextSize(40f);
            undoneCountPaint.setAntiAlias(true);
            undoneCountPaint.setFakeBoldText(true);

            statusPaint.setColor(Color.parseColor("#FDE68A"));
            statusPaint.setTextSize(38f);
            statusPaint.setAntiAlias(true);

            panelPaint.setAntiAlias(true);
            headerPaint.setAntiAlias(true);
            cardPaint.setAntiAlias(true);
            doneCardPaint.setAntiAlias(true);

            cardStrokePaint.setColor(Color.parseColor("#3E4B6D"));
            cardStrokePaint.setStyle(Paint.Style.STROKE);
            cardStrokePaint.setStrokeWidth(2f);
            cardStrokePaint.setAntiAlias(true);

            doneCardStrokePaint.setColor(Color.parseColor("#4A5568"));
            doneCardStrokePaint.setStyle(Paint.Style.STROKE);
            doneCardStrokePaint.setStrokeWidth(2f);
            doneCardStrokePaint.setAntiAlias(true);

            dueTodayCardStrokePaint.setStyle(Paint.Style.STROKE);
            dueTodayCardStrokePaint.setStrokeWidth(3f);
            dueTodayCardStrokePaint.setAntiAlias(true);

            accentPaint.setAntiAlias(true);
            dueTodayAccentPaint.setAntiAlias(true);

            doneAccentPaint.setColor(Color.parseColor("#94A3B8"));
            doneAccentPaint.setAntiAlias(true);

            applyStoredColors();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            this.visible = visible;
            if (visible) {
                statusMessage = "イベント取得中...";
                drawFrame();
                fetchEvents();
                startDateWatcher();
            } else {
                stopDateWatcher();
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            drawFrame();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            executor.shutdownNow();
            mainHandler.removeCallbacksAndMessages(null);
        }

        private void fetchEvents() {
            if (isFetching) {
                return;
            }
            String userUuid = getUserUuid();
            if (TextUtils.isEmpty(userUuid)) {
                statusMessage = "user_uuid が未設定です";
                drawFrame();
                return;
            }
            isFetching = true;
            executor.execute(() -> {
                String nextStatusMessage = "";
                List<EventItem> nextEvents = new ArrayList<>();
                String nextFetchTime = latestFetchTime;
                String nextFetchedDate = latestFetchedDate;
                HttpURLConnection connection = null;
                try {
                    URL url = new URL(ENDPOINT);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("user_uuid", userUuid);
                    connection.setRequestProperty("Accept", "application/json");
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);
                    connection.connect();

                    int status = connection.getResponseCode();
                    InputStream stream = status >= 200 && status < 300
                            ? connection.getInputStream()
                            : connection.getErrorStream();
                    String body = readStream(stream);
                    if (status >= 200 && status < 300) {
                        nextEvents = parseEvents(body);
                        nextFetchTime = LocalTime.now().format(timeFormatter);
                        nextFetchedDate = LocalDate.now().toString();
                        if (nextEvents.isEmpty()) {
                            nextStatusMessage = "予定はありません";
                        }
                    } else {
                        nextStatusMessage = "取得失敗";
                    }
                } catch (Exception e) {
                    nextStatusMessage = "通信エラー: " + e.getClass().getSimpleName();
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }

                String finalStatusMessage = nextStatusMessage;
                List<EventItem> finalEvents = nextEvents;
                String finalFetchTime = nextFetchTime;
                String finalFetchedDate = nextFetchedDate;
                mainHandler.post(() -> {
                    latestEvents.clear();
                    latestEvents.addAll(finalEvents);
                    latestFetchTime = finalFetchTime;
                    latestFetchedDate = finalFetchedDate;
                    statusMessage = finalStatusMessage;
                    isFetching = false;
                    if (visible) {
                        drawFrame();
                    }
                });
            });
        }

        private void startDateWatcher() {
            mainHandler.removeCallbacks(dateChangeWatcher);
            mainHandler.postDelayed(dateChangeWatcher, 10_000L);
        }

        private void stopDateWatcher() {
            mainHandler.removeCallbacks(dateChangeWatcher);
        }

        private boolean shouldRefetchForDateChange() {
            String today = LocalDate.now().toString();
            if (latestFetchedDate.isEmpty()) {
                return false;
            }
            return !today.equals(latestFetchedDate);
        }

        private void drawFrame() {
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas == null) {
                    return;
                }
                applyStoredColors();
                canvas.drawColor(backgroundColor);

                accentPaint.setAlpha(28);
                canvas.drawCircle(60f, canvas.getHeight() - 120f, 140f, accentPaint);
                accentPaint.setAlpha(255);

                float margin = 28f;
                float topSpacerPx = dpToPx(getTopSpacerDp());
                float panelRadius = 36f;
                float panelTop = margin + topSpacerPx;
                float panelBottom = canvas.getHeight() - margin;
                if (panelTop > panelBottom - 240f) {
                    panelTop = panelBottom - 240f;
                }
                RectF panelRect = new RectF(margin, panelTop, canvas.getWidth() - margin, panelBottom);
                canvas.drawRoundRect(panelRect, panelRadius, panelRadius, panelPaint);

                RectF headerRect = new RectF(
                        panelRect.left + 20f,
                        panelRect.top + 20f,
                        panelRect.right - 20f,
                        panelRect.top + 190f
                );
                canvas.drawRoundRect(headerRect, 28f, 28f, headerPaint);
                canvas.drawRect(headerRect.left, headerRect.bottom - 18f, headerRect.right, headerRect.bottom, headerPaint);

                float textLeft = headerRect.left + 24f;
                float headerY = headerRect.top + 66f;
                canvas.drawText("最終取得時間  " + latestFetchTime, textLeft, headerY, headerMetaPaint);
                canvas.drawText(getTodayTitle(), textLeft, headerY + 84f, titlePaint);
                String undoneCountText = "未完了：" + getUndoneCount();
                float undoneX = headerRect.right - 24f - undoneCountPaint.measureText(undoneCountText);
                canvas.drawText(undoneCountText, undoneX, headerY + 84f, undoneCountPaint);

                float y = headerRect.bottom + 24f;
                if (!TextUtils.isEmpty(statusMessage)) {
                    canvas.drawText(statusMessage, textLeft, y + 34f, statusPaint);
                    y += 72f;
                }

                int maxVisibleEvents = getMaxVisibleEvents();
                int maxToShow = Math.min(maxVisibleEvents, latestEvents.size());
                float cardLeft = panelRect.left + 20f;
                float cardRight = panelRect.right - 20f;
                float cardHeight = 132f;
                for (int i = 0; i < maxToShow; i++) {
                    float cardTop = y;
                    float cardBottom = cardTop + cardHeight;
                    if (cardBottom > panelRect.bottom - 24f) {
                        break;
                    }

                    EventItem item = latestEvents.get(i);
                    RectF cardRect = new RectF(cardLeft, cardTop, cardRight, cardBottom);
                    boolean dueToday = isDueToday(item);
                    Paint fillPaint = item.done ? doneCardPaint : cardPaint;
                    Paint strokePaint = item.done ? doneCardStrokePaint : cardStrokePaint;
                    Paint namePaint = item.done ? doneEventNamePaint : eventNamePaint;
                    Paint metaPaint = item.done ? doneEventMetaPaint : eventMetaPaint;
                    Paint dotPaint = item.done ? doneAccentPaint : accentPaint;

                    if (dueToday && !item.done) {
                        strokePaint = dueTodayCardStrokePaint;
                        dotPaint = dueTodayAccentPaint;
                    }

                    canvas.drawRoundRect(cardRect, 24f, 24f, fillPaint);
                    canvas.drawRoundRect(cardRect, 24f, 24f, strokePaint);

                    float dotCx = cardRect.left + 18f;
                    float dotCy = cardRect.top + (cardHeight / 2f);
                    canvas.drawCircle(dotCx, dotCy, 8f, dotPaint);

                    float contentLeft = cardRect.left + 34f;
                    float maxWidth = cardRect.width() - 46f;
                    String eventName = ellipsizeLine(item.eventName, namePaint, maxWidth);
                    String meta = buildEventMeta(item);
                    String eventMeta = ellipsizeLine(meta, metaPaint, maxWidth);
                    canvas.drawText(eventName, contentLeft, cardRect.top + 52f, namePaint);
                    canvas.drawText(eventMeta, contentLeft, cardRect.top + 108f, metaPaint);
                    y += cardHeight + 18f;
                }

                if (latestEvents.size() > maxVisibleEvents && y + 40f < panelRect.bottom) {
                    canvas.drawText("他に予定あり", cardLeft, y + 30f, statusPaint);
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas);
                }
            }
        }

        private String readStream(InputStream stream) throws Exception {
            if (stream == null) {
                return "";
            }
            StringBuilder builder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (builder.length() > 0) {
                        builder.append('\n');
                    }
                    builder.append(line);
                }
            }
            return builder.toString();
        }

        private List<EventItem> parseEvents(String body) throws Exception {
            List<EventItem> events = new ArrayList<>();
            if (body == null || body.trim().isEmpty()) {
                return new ArrayList<>();
            }

            String trimmed = body.trim();
            JSONArray array;
            if (trimmed.startsWith("[")) {
                array = new JSONArray(trimmed);
            } else if (trimmed.startsWith("{")) {
                JSONObject object = new JSONObject(trimmed);
                JSONArray eventsArray = object.optJSONArray("events");
                if (eventsArray == null) {
                    eventsArray = object.optJSONArray("data");
                }
                if (eventsArray == null) {
                    return new ArrayList<>();
                }
                array = eventsArray;
            } else {
                return new ArrayList<>();
            }

            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                String eventName = item.optString("event_name", "");
                if (TextUtils.isEmpty(eventName)) {
                    eventName = "(名称未設定)";
                }
                String startDate = item.optString("start_date", "-");
                String endDate = item.optString("end_date", startDate);
                String startTime = item.isNull("start_time") ? null : item.optString("start_time", null);
                if (TextUtils.isEmpty(startTime)) {
                    startTime = null;
                }
                String endTime = item.isNull("end_time") ? null : item.optString("end_time", null);
                if (TextUtils.isEmpty(endTime)) {
                    endTime = null;
                }
                boolean done = parseDone(item);
                events.add(new EventItem(eventName, startDate, endDate, startTime, endTime, done));
            }

            sortEventsByRule(events);
            return events;
        }

        private boolean parseDone(JSONObject item) {
            Object doneValue = item.opt("done");
            if (doneValue instanceof Boolean) {
                return (Boolean) doneValue;
            }
            if (doneValue instanceof String) {
                return Boolean.parseBoolean(((String) doneValue).trim());
            }
            return false;
        }

        private void sortEventsByRule(List<EventItem> events) {
            events.sort((a, b) -> {
                if (a.done != b.done) {
                    return a.done ? 1 : -1;
                }

                if (!a.done) {
                    int priorityCompare = Integer.compare(getUndonePriority(a), getUndonePriority(b));
                    if (priorityCompare != 0) {
                        return priorityCompare;
                    }
                }

                int byEndDate = compareByEndDate(a, b);
                if (byEndDate != 0) {
                    return byEndDate;
                }

                return compareByTime(a, b);
            });
        }

        private int getUndonePriority(EventItem item) {
            if (item.done) {
                return 99;
            }
            LocalDate endDate = parseDate(item.endDate);
            boolean dueToday = endDate != null && endDate.isEqual(LocalDate.now());
            boolean hasTime = resolveDisplayTime(item) != null;
            if (dueToday && hasTime) {
                return 0;
            }
            if (dueToday) {
                return 1;
            }
            return 2;
        }

        private boolean isDueToday(EventItem item) {
            LocalDate endDate = parseDate(item.endDate);
            if (endDate == null) {
                return false;
            }
            return endDate.isEqual(LocalDate.now());
        }

        private int compareByEndDate(EventItem a, EventItem b) {
            LocalDate endDateA = parseDate(a.endDate);
            LocalDate endDateB = parseDate(b.endDate);
            if (endDateA != null && endDateB != null) {
                return endDateA.compareTo(endDateB);
            }
            if (endDateA == null && endDateB != null) {
                return 1;
            }
            if (endDateA != null) {
                return -1;
            }
            return 0;
        }

        private int compareByTime(EventItem a, EventItem b) {
            LocalTime timeA = resolveDisplayTime(a);
            LocalTime timeB = resolveDisplayTime(b);
            if (timeA == null && timeB == null) {
                return 0;
            }
            if (timeA == null) {
                return 1;
            }
            if (timeB == null) {
                return -1;
            }
            return timeA.compareTo(timeB);
        }

        private LocalTime resolveDisplayTime(EventItem item) {
            LocalTime parsedEnd = parseTime(item.endTime);
            if (parsedEnd != null) {
                return parsedEnd;
            }
            return parseTime(item.startTime);
        }

        private LocalTime resolveStartDisplayTime(EventItem item) {
            LocalTime parsedStart = parseTime(item.startTime);
            if (parsedStart != null) {
                return parsedStart;
            }
            return parseTime(item.endTime);
        }

        private LocalDate parseDate(String value) {
            if (TextUtils.isEmpty(value)) {
                return null;
            }
            String v = value.trim();
            try {
                return LocalDate.parse(v);
            } catch (DateTimeParseException ignored) {
            }
            if (v.length() >= 10) {
                String head = v.substring(0, 10);
                try {
                    return LocalDate.parse(head);
                } catch (DateTimeParseException ignored) {
                }
            }
            return null;
        }

        private LocalTime parseTime(String value) {
            if (TextUtils.isEmpty(value)) {
                return null;
            }
            String v = value.trim();
            try {
                return LocalTime.parse(v);
            } catch (DateTimeParseException ignored) {
            }
            if (v.length() >= 5) {
                String hm = v.substring(0, 5);
                try {
                    return LocalTime.parse(hm);
                } catch (DateTimeParseException ignored) {
                }
            }
            return null;
        }

        private String formatDateForDisplay(String rawDate) {
            LocalDate date = parseDate(rawDate);
            if (date != null) {
                return date.format(displayDateFormatter);
            }
            if (TextUtils.isEmpty(rawDate)) {
                return "--/--";
            }
            String v = rawDate.trim();
            if (v.length() >= 10) {
                return v.substring(5, 7) + "/" + v.substring(8, 10);
            }
            return rawDate;
        }

        private String formatTimeForDisplay(LocalTime time) {
            if (time == null) {
                return null;
            }
            return String.format("%02d:%02d", time.getHour(), time.getMinute());
        }

        private String getTodayTitle() {
            return LocalDate.now().getDayOfMonth() + "日の予定";
        }

        private int getUndoneCount() {
            int count = 0;
            for (EventItem item : latestEvents) {
                if (!item.done) {
                    count++;
                }
            }
            return count;
        }

        private String buildEventMeta(EventItem item) {
            String start = formatDateForDisplay(item.startDate);
            String end = formatDateForDisplay(item.endDate);
            LocalTime startTime = resolveStartDisplayTime(item);
            String timeText = formatTimeForDisplay(startTime);
            if (timeText == null) {
                return start + " 時刻未定 ～ " + end;
            }
            return start + " " + timeText + " ～ " + end;
        }

        private String ellipsizeLine(String source, Paint paint, float maxWidth) {
            if (paint.measureText(source) <= maxWidth) {
                return source;
            }
            String suffix = "...";
            float targetWidth = maxWidth - paint.measureText(suffix);
            if (targetWidth <= 0) {
                return suffix;
            }
            int count = paint.breakText(source, true, targetWidth, null);
            if (count <= 0) {
                return suffix;
            }
            return source.substring(0, count) + suffix;
        }

        private int getTopSpacerDp() {
            SharedPreferences preferences = getSharedPreferences(
                    WallpaperSettings.PREFS_NAME,
                    MODE_PRIVATE
            );
            return preferences.getInt(
                    WallpaperSettings.KEY_TOP_SPACER_DP,
                    WallpaperSettings.DEFAULT_TOP_SPACER_DP
            );
        }

        private int getStoredColor(String key, String fallbackHex) {
            SharedPreferences preferences = getSharedPreferences(
                    WallpaperSettings.PREFS_NAME,
                    MODE_PRIVATE
            );
            String value = preferences.getString(key, fallbackHex);
            if (value == null || value.trim().isEmpty()) {
                value = fallbackHex;
            }
            try {
                return Color.parseColor(value.trim());
            } catch (IllegalArgumentException e) {
                return Color.parseColor(fallbackHex);
            }
        }

        private void applyStoredColors() {
            backgroundColor = getStoredColor(
                    WallpaperSettings.KEY_COLOR_BACKGROUND,
                    WallpaperSettings.DEFAULT_COLOR_BACKGROUND
            );
            panelPaint.setColor(getStoredColor(
                    WallpaperSettings.KEY_COLOR_PANEL,
                    WallpaperSettings.DEFAULT_COLOR_PANEL
            ));
            headerPaint.setColor(getStoredColor(
                    WallpaperSettings.KEY_COLOR_HEADER,
                    WallpaperSettings.DEFAULT_COLOR_HEADER
            ));
            int headerTextColor = getStoredColor(
                    WallpaperSettings.KEY_COLOR_HEADER_TEXT,
                    WallpaperSettings.DEFAULT_COLOR_HEADER_TEXT
            );
            titlePaint.setColor(headerTextColor);
            headerMetaPaint.setColor(headerTextColor);
            cardPaint.setColor(getStoredColor(
                    WallpaperSettings.KEY_COLOR_CARD,
                    WallpaperSettings.DEFAULT_COLOR_CARD
            ));
            int cardTextColor = getStoredColor(
                    WallpaperSettings.KEY_COLOR_CARD_TEXT,
                    WallpaperSettings.DEFAULT_COLOR_CARD_TEXT
            );
            eventNamePaint.setColor(cardTextColor);
            eventMetaPaint.setColor(cardTextColor);
            int accentColor = getStoredColor(
                    WallpaperSettings.KEY_COLOR_ACCENT,
                    WallpaperSettings.DEFAULT_COLOR_ACCENT
            );
            accentPaint.setColor(accentColor);

            int dueTodayColor = getStoredColor(
                    WallpaperSettings.KEY_COLOR_DUE_TODAY,
                    WallpaperSettings.DEFAULT_COLOR_DUE_TODAY
            );
            dueTodayCardStrokePaint.setColor(dueTodayColor);
            dueTodayAccentPaint.setColor(dueTodayColor);

            doneCardPaint.setColor(getStoredColor(
                    WallpaperSettings.KEY_COLOR_DONE_CARD,
                    WallpaperSettings.DEFAULT_COLOR_DONE_CARD
            ));
            int doneTextColor = getStoredColor(
                    WallpaperSettings.KEY_COLOR_DONE_TEXT,
                    WallpaperSettings.DEFAULT_COLOR_DONE_TEXT
            );
            doneEventNamePaint.setColor(doneTextColor);
            doneEventMetaPaint.setColor(doneTextColor);
            undoneCountPaint.setColor(getStoredColor(
                    WallpaperSettings.KEY_COLOR_UNDONE_COUNT,
                    WallpaperSettings.DEFAULT_COLOR_UNDONE_COUNT
            ));
        }

        private int getMaxVisibleEvents() {
            SharedPreferences preferences = getSharedPreferences(
                    WallpaperSettings.PREFS_NAME,
                    MODE_PRIVATE
            );
            int value = preferences.getInt(
                    WallpaperSettings.KEY_MAX_VISIBLE_EVENTS,
                    WallpaperSettings.DEFAULT_MAX_VISIBLE_EVENTS
            );
            if (value < WallpaperSettings.MIN_MAX_VISIBLE_EVENTS) {
                return WallpaperSettings.MIN_MAX_VISIBLE_EVENTS;
            }
            return Math.min(value, WallpaperSettings.MAX_MAX_VISIBLE_EVENTS);
        }

        private String getUserUuid() {
            SharedPreferences preferences = getSharedPreferences(
                    WallpaperSettings.PREFS_NAME,
                    MODE_PRIVATE
            );
            String value = preferences.getString(WallpaperSettings.KEY_USER_UUID, "");
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
            SharedPreferences appPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
            String appValue = appPreferences.getString("uuid", "");
            if (appValue == null) {
                return "";
            }
            return appValue.trim();
        }

        private float dpToPx(int dp) {
            return dp * getResources().getDisplayMetrics().density;
        }

        private class EventItem {
            private final String eventName;
            private final String startDate;
            private final String endDate;
            private final String startTime;
            private final String endTime;
            private final boolean done;

            EventItem(String eventName, String startDate, String endDate, String startTime, String endTime, boolean done) {
                this.eventName = eventName;
                this.startDate = startDate;
                this.endDate = endDate;
                this.startTime = startTime;
                this.endTime = endTime;
                this.done = done;
            }
        }
    }
}
