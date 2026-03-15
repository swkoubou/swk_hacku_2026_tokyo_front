package com.example.myapplication

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class FeatureSettingsActivity : AppCompatActivity() {

    private val colorPalette = arrayOf(
        "#0B1020", "#111827", "#1F2937", "#334155",
        "#1B1F2E", "#222D45", "#273554", "#334E68",
        "#60A5FA", "#38BDF8", "#22D3EE", "#34D399",
        "#A78BFA", "#F472B6", "#FB7185", "#F59E0B",
        "#FBBF24", "#84CC16", "#10B981", "#94A3B8",
        "#FFFFFF", "#F8FAFC", "#F1F5F9", "#E2E8F0",
        "#E5E7EB", "#D1D5DB", "#CBD5E1", "#F5F5F5"
    )

    private lateinit var topSpacerValueView: TextView
    private lateinit var maxVisibleEventsValueView: TextView
    private lateinit var userUuidInput: EditText

    private lateinit var colorBackgroundPreview: android.view.View
    private lateinit var colorPanelPreview: android.view.View
    private lateinit var colorHeaderPreview: android.view.View
    private lateinit var colorCardPreview: android.view.View
    private lateinit var colorAccentPreview: android.view.View
    private lateinit var colorDueTodayPreview: android.view.View
    private lateinit var colorDoneCardPreview: android.view.View
    private lateinit var colorDoneTextPreview: android.view.View

    private lateinit var colorBackground: String
    private lateinit var colorPanel: String
    private lateinit var colorHeader: String
    private lateinit var colorCard: String
    private lateinit var colorAccent: String
    private lateinit var colorDueToday: String
    private lateinit var colorDoneCard: String
    private lateinit var colorDoneText: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feature_settings)

        topSpacerValueView = findViewById(R.id.tv_top_spacer_value)
        maxVisibleEventsValueView = findViewById(R.id.tv_max_visible_events_value)
        userUuidInput = findViewById(R.id.et_user_uuid)
        colorBackgroundPreview = findViewById(R.id.view_color_background)
        colorPanelPreview = findViewById(R.id.view_color_panel)
        colorHeaderPreview = findViewById(R.id.view_color_header)
        colorCardPreview = findViewById(R.id.view_color_card)
        colorAccentPreview = findViewById(R.id.view_color_accent)
        colorDueTodayPreview = findViewById(R.id.view_color_due_today)
        colorDoneCardPreview = findViewById(R.id.view_color_done_card)
        colorDoneTextPreview = findViewById(R.id.view_color_done_text)

        setupTopSpacerControls()
        setupMaxVisibleEventsControls()
        setupUserUuidControls()
        setupColorControls()
        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        findViewById<Button>(R.id.btn_apply_wallpaper).setOnClickListener {
            if (saveUserUuidFromInput()) {
                saveColorsFromState()
                openWallpaperApplyScreen()
            }
        }
    }

    private fun setupTopSpacerControls() {
        val preferences = getSharedPreferences(WallpaperSettings.PREFS_NAME, MODE_PRIVATE)
        val topSpacerSeekBar = findViewById<SeekBar>(R.id.seek_top_spacer)
        topSpacerSeekBar.max = WallpaperSettings.MAX_TOP_SPACER_DP

        val savedValue = preferences.getInt(
            WallpaperSettings.KEY_TOP_SPACER_DP,
            WallpaperSettings.DEFAULT_TOP_SPACER_DP
        )
        topSpacerSeekBar.progress = savedValue
        renderTopSpacerLabel(savedValue)

        topSpacerSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                renderTopSpacerLabel(progress)
                preferences.edit().putInt(WallpaperSettings.KEY_TOP_SPACER_DP, progress).apply()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
    }

    private fun renderTopSpacerLabel(valueDp: Int) {
        topSpacerValueView.text = getString(R.string.top_spacer_value_format, valueDp)
    }

    private fun setupMaxVisibleEventsControls() {
        val preferences = getSharedPreferences(WallpaperSettings.PREFS_NAME, MODE_PRIVATE)
        val maxVisibleEventsSeekBar = findViewById<SeekBar>(R.id.seek_max_visible_events)

        val maxRange = WallpaperSettings.MAX_MAX_VISIBLE_EVENTS - WallpaperSettings.MIN_MAX_VISIBLE_EVENTS
        maxVisibleEventsSeekBar.max = maxRange

        val savedValue = preferences.getInt(
            WallpaperSettings.KEY_MAX_VISIBLE_EVENTS,
            WallpaperSettings.DEFAULT_MAX_VISIBLE_EVENTS
        ).coerceIn(
            WallpaperSettings.MIN_MAX_VISIBLE_EVENTS,
            WallpaperSettings.MAX_MAX_VISIBLE_EVENTS
        )

        maxVisibleEventsSeekBar.progress = savedValue - WallpaperSettings.MIN_MAX_VISIBLE_EVENTS
        renderMaxVisibleEventsLabel(savedValue)

        maxVisibleEventsSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = WallpaperSettings.MIN_MAX_VISIBLE_EVENTS + progress
                renderMaxVisibleEventsLabel(value)
                preferences.edit().putInt(WallpaperSettings.KEY_MAX_VISIBLE_EVENTS, value).apply()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
    }

    private fun renderMaxVisibleEventsLabel(value: Int) {
        maxVisibleEventsValueView.text = getString(R.string.max_visible_events_value_format, value)
    }

    private fun setupUserUuidControls() {
        val preferences = getSharedPreferences(WallpaperSettings.PREFS_NAME, MODE_PRIVATE)
        val wallpaperUuid = preferences.getString(WallpaperSettings.KEY_USER_UUID, "").orEmpty().trim()
        val appUuid = UuidManager.getUuid(this)?.trim().orEmpty()
        val initialUuid = if (wallpaperUuid.isNotEmpty()) wallpaperUuid else appUuid

        userUuidInput.setText(initialUuid)
        if (initialUuid.isNotEmpty()) {
            preferences.edit().putString(WallpaperSettings.KEY_USER_UUID, initialUuid).apply()
            UuidManager.saveUuid(this, initialUuid)
        }

        findViewById<Button>(R.id.btn_save_user_uuid).setOnClickListener {
            if (saveUserUuidFromInput()) {
                Toast.makeText(this, R.string.user_uuid_saved, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveUserUuidFromInput(): Boolean {
        val userUuid = userUuidInput.text?.toString()?.trim().orEmpty()
        if (userUuid.isEmpty()) {
            Toast.makeText(this, R.string.user_uuid_required, Toast.LENGTH_SHORT).show()
            return false
        }
        val preferences = getSharedPreferences(WallpaperSettings.PREFS_NAME, MODE_PRIVATE)
        preferences.edit().putString(WallpaperSettings.KEY_USER_UUID, userUuid).apply()
        UuidManager.saveUuid(this, userUuid)
        return true
    }

    private fun setupColorControls() {
        colorBackground = readColor(WallpaperSettings.KEY_COLOR_BACKGROUND, WallpaperSettings.DEFAULT_COLOR_BACKGROUND)
        colorPanel = readColor(WallpaperSettings.KEY_COLOR_PANEL, WallpaperSettings.DEFAULT_COLOR_PANEL)
        colorHeader = readColor(WallpaperSettings.KEY_COLOR_HEADER, WallpaperSettings.DEFAULT_COLOR_HEADER)
        colorCard = readColor(WallpaperSettings.KEY_COLOR_CARD, WallpaperSettings.DEFAULT_COLOR_CARD)
        colorAccent = readColor(WallpaperSettings.KEY_COLOR_ACCENT, WallpaperSettings.DEFAULT_COLOR_ACCENT)
        colorDueToday = readColor(WallpaperSettings.KEY_COLOR_DUE_TODAY, WallpaperSettings.DEFAULT_COLOR_DUE_TODAY)
        colorDoneCard = readColor(WallpaperSettings.KEY_COLOR_DONE_CARD, WallpaperSettings.DEFAULT_COLOR_DONE_CARD)
        colorDoneText = readColor(WallpaperSettings.KEY_COLOR_DONE_TEXT, WallpaperSettings.DEFAULT_COLOR_DONE_TEXT)
        renderAllColorPreviews()

        findViewById<LinearLayout>(R.id.row_color_background).setOnClickListener {
            showColorPaletteDialog(getString(R.string.color_background_label), colorBackground) { selected ->
                colorBackground = selected
                updateColorPreview(colorBackgroundPreview, colorBackground)
                saveColorsFromState()
            }
        }
        findViewById<LinearLayout>(R.id.row_color_panel).setOnClickListener {
            showColorPaletteDialog(getString(R.string.color_panel_label), colorPanel) { selected ->
                colorPanel = selected
                updateColorPreview(colorPanelPreview, colorPanel)
                saveColorsFromState()
            }
        }
        findViewById<LinearLayout>(R.id.row_color_header).setOnClickListener {
            showColorPaletteDialog(getString(R.string.color_header_label), colorHeader) { selected ->
                colorHeader = selected
                updateColorPreview(colorHeaderPreview, colorHeader)
                saveColorsFromState()
            }
        }
        findViewById<LinearLayout>(R.id.row_color_card).setOnClickListener {
            showColorPaletteDialog(getString(R.string.color_card_label), colorCard) { selected ->
                colorCard = selected
                updateColorPreview(colorCardPreview, colorCard)
                saveColorsFromState()
            }
        }
        findViewById<LinearLayout>(R.id.row_color_accent).setOnClickListener {
            showColorPaletteDialog(getString(R.string.color_accent_label), colorAccent) { selected ->
                colorAccent = selected
                updateColorPreview(colorAccentPreview, colorAccent)
                saveColorsFromState()
            }
        }
        findViewById<LinearLayout>(R.id.row_color_due_today).setOnClickListener {
            showColorPaletteDialog(getString(R.string.color_due_today_label), colorDueToday) { selected ->
                colorDueToday = selected
                updateColorPreview(colorDueTodayPreview, colorDueToday)
                saveColorsFromState()
            }
        }
        findViewById<LinearLayout>(R.id.row_color_done_card).setOnClickListener {
            showColorPaletteDialog(getString(R.string.color_done_card_label), colorDoneCard) { selected ->
                colorDoneCard = selected
                updateColorPreview(colorDoneCardPreview, colorDoneCard)
                saveColorsFromState()
            }
        }
        findViewById<LinearLayout>(R.id.row_color_done_text).setOnClickListener {
            showColorPaletteDialog(getString(R.string.color_done_text_label), colorDoneText) { selected ->
                colorDoneText = selected
                updateColorPreview(colorDoneTextPreview, colorDoneText)
                saveColorsFromState()
            }
        }

        findViewById<Button>(R.id.btn_save_colors).setOnClickListener {
            saveColorsFromState()
            Toast.makeText(this, R.string.colors_saved, Toast.LENGTH_SHORT).show()
            openWallpaperApplyScreen()
        }

        findViewById<Button>(R.id.btn_reset_colors).setOnClickListener {
            colorBackground = WallpaperSettings.DEFAULT_COLOR_BACKGROUND
            colorPanel = WallpaperSettings.DEFAULT_COLOR_PANEL
            colorHeader = WallpaperSettings.DEFAULT_COLOR_HEADER
            colorCard = WallpaperSettings.DEFAULT_COLOR_CARD
            colorAccent = WallpaperSettings.DEFAULT_COLOR_ACCENT
            colorDueToday = WallpaperSettings.DEFAULT_COLOR_DUE_TODAY
            colorDoneCard = WallpaperSettings.DEFAULT_COLOR_DONE_CARD
            colorDoneText = WallpaperSettings.DEFAULT_COLOR_DONE_TEXT
            renderAllColorPreviews()
            saveColorsFromState()
            Toast.makeText(this, R.string.colors_reset, Toast.LENGTH_SHORT).show()
            openWallpaperApplyScreen()
        }
    }

    private fun readColor(key: String, defaultValue: String): String {
        val preferences = getSharedPreferences(WallpaperSettings.PREFS_NAME, MODE_PRIVATE)
        val value = preferences.getString(key, defaultValue).orEmpty().trim()
        if (value.isEmpty()) {
            return defaultValue
        }
        return try {
            Color.parseColor(value)
            value.uppercase()
        } catch (_: IllegalArgumentException) {
            defaultValue
        }
    }

    private fun saveColorsFromState() {
        val preferences = getSharedPreferences(WallpaperSettings.PREFS_NAME, MODE_PRIVATE)
        preferences.edit()
            .putString(WallpaperSettings.KEY_COLOR_BACKGROUND, colorBackground)
            .putString(WallpaperSettings.KEY_COLOR_PANEL, colorPanel)
            .putString(WallpaperSettings.KEY_COLOR_HEADER, colorHeader)
            .putString(WallpaperSettings.KEY_COLOR_CARD, colorCard)
            .putString(WallpaperSettings.KEY_COLOR_ACCENT, colorAccent)
            .putString(WallpaperSettings.KEY_COLOR_DUE_TODAY, colorDueToday)
            .putString(WallpaperSettings.KEY_COLOR_DONE_CARD, colorDoneCard)
            .putString(WallpaperSettings.KEY_COLOR_DONE_TEXT, colorDoneText)
            .apply()
    }

    private fun renderAllColorPreviews() {
        updateColorPreview(colorBackgroundPreview, colorBackground)
        updateColorPreview(colorPanelPreview, colorPanel)
        updateColorPreview(colorHeaderPreview, colorHeader)
        updateColorPreview(colorCardPreview, colorCard)
        updateColorPreview(colorAccentPreview, colorAccent)
        updateColorPreview(colorDueTodayPreview, colorDueToday)
        updateColorPreview(colorDoneCardPreview, colorDoneCard)
        updateColorPreview(colorDoneTextPreview, colorDoneText)
    }

    private fun updateColorPreview(preview: android.view.View, hexColor: String) {
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(8)
            setColor(Color.parseColor(hexColor))
            setStroke(dpInt(1), Color.parseColor("#444444"))
        }
        preview.background = drawable
    }

    private fun showColorPaletteDialog(
        title: String,
        currentColor: String,
        onSelected: (String) -> Unit
    ) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpInt(12), dpInt(12), dpInt(12), dpInt(4))
        }

        val columns = 4
        val itemSize = dpInt(44)
        val itemMargin = dpInt(8)
        var currentRow: LinearLayout? = null
        var dialogRef: AlertDialog? = null

        colorPalette.forEachIndexed { index, hex ->
            if (index % columns == 0) {
                currentRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                container.addView(currentRow)
            }

            val swatch = Button(this).apply {
                layoutParams = LinearLayout.LayoutParams(itemSize, itemSize).also { lp ->
                    lp.setMargins(itemMargin, itemMargin, itemMargin, itemMargin)
                }
                text = if (hex.equals(currentColor, ignoreCase = true)) "✓" else ""
                setTextColor(if (isLightColor(hex)) Color.BLACK else Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)

                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(10)
                    setColor(Color.parseColor(hex))
                    setStroke(dpInt(2), if (hex.equals(currentColor, ignoreCase = true)) Color.WHITE else Color.parseColor("#333333"))
                }

                setOnClickListener {
                    onSelected(hex)
                    dialogRef?.dismiss()
                }
            }

            currentRow?.addView(swatch)
        }

        dialogRef = AlertDialog.Builder(this)
            .setTitle(title)
            .setView(container)
            .setNegativeButton(R.string.close, null)
            .create()
        dialogRef.show()
    }

    private fun isLightColor(hex: String): Boolean {
        val color = Color.parseColor(hex)
        val luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0
        return luminance > 0.65
    }

    private fun dp(value: Int): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        )
    }

    private fun dpInt(value: Int): Int = Math.round(dp(value))

    private fun isBlank(value: String?): Boolean = TextUtils.isEmpty(value?.trim())

    private fun getFirstNonBlank(first: String?, second: String?): String {
        return if (!isBlank(first)) first!!.trim() else second?.trim().orEmpty()
    }

    private fun openWallpaperApplyScreen() {
        val preferences = getSharedPreferences(WallpaperSettings.PREFS_NAME, MODE_PRIVATE)
        val wallpaperUuid = preferences.getString(WallpaperSettings.KEY_USER_UUID, null)
        val appUuid = UuidManager.getUuid(this)
        val synced = getFirstNonBlank(wallpaperUuid, appUuid)
        if (synced.isNotEmpty()) {
            preferences.edit().putString(WallpaperSettings.KEY_USER_UUID, synced).apply()
            UuidManager.saveUuid(this, synced)
        }

        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
        intent.putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            ComponentName(this, EventWallpaperService::class.java)
        )

        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
            Toast.makeText(this, R.string.wallpaper_choose_fallback, Toast.LENGTH_LONG).show()
        }
    }
}
