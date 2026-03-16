package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Launcher alias dedicated entry point for Todo list.
 * Always forwards to ListActivity and finishes immediately.
 */
class TodoLauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openTodoList()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        openTodoList()
    }

    private fun openTodoList() {
        val intent = Intent(this, ListActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
    }
}
