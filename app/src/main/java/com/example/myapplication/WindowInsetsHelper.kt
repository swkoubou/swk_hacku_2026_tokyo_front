package com.example.myapplication

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.max

object WindowInsetsHelper {

    fun applySystemBarInsets(view: View, includeBottomInset: Boolean = true) {
        val initialLeft = view.paddingLeft
        val initialTop = view.paddingTop
        val initialRight = view.paddingRight
        val initialBottom = view.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(view) { target, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutoutTop = insets.getInsets(WindowInsetsCompat.Type.displayCutout()).top
            val notchTopInset = max(systemBars.top, cutoutTop)
            val bottomInset = if (includeBottomInset) systemBars.bottom else 0

            target.setPadding(
                initialLeft + systemBars.left,
                initialTop + notchTopInset,
                initialRight + systemBars.right,
                initialBottom + bottomInset
            )
            insets
        }

        ViewCompat.requestApplyInsets(view)
    }
}
