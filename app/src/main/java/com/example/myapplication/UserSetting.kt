package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import android.content.Intent
import android.widget.Toast
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import com.google.gson.Gson

class UserSetting : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_setting)
        WindowInsetsHelper.applySystemBarInsets(findViewById(R.id.main))

        val button = findViewById<Button>(R.id.toHome)
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        button.setOnClickListener {
            getUuid()
        }
    }

    private fun getUuid() {

        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://hackutokyo2026.yoimiya.net/gen_uuid")
            .post("".toRequestBody())
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@UserSetting, "通信エラー", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {

                val body = response.body?.string() ?: return

                val uuidResponse = Gson().fromJson(body, UuidResponse::class.java)
                val uuid = uuidResponse.user_uuid

                UuidManager.saveUuid(this@UserSetting, uuid)

                runOnUiThread {

                    AlertDialog.Builder(this@UserSetting)
                        .setTitle("完了")
                        .setMessage("登録が完了しました。")
                        .setPositiveButton("OK") { _, _ ->

                            val intent = Intent(this@UserSetting, VoiceRecognition::class.java)
                            startActivity(intent)

                        }
                        .show()

                }
            }
        })
    }

}