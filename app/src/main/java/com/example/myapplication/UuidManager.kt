package com.example.myapplication

import android.content.Context
import androidx.core.content.edit

object UuidManager {

    private const val PREF_NAME = "app_prefs"
    private const val KEY_UUID = "uuid"

    fun saveUuid(context: Context, uuid: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(KEY_UUID, uuid)
        }
    }

    fun getUuid(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_UUID, null)
    }
}