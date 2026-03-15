package com.example.myapplication

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import com.google.gson.Gson
import android.content.Context
import android.os.SystemClock

class MessageAnalysis(private val context: Context) {
    private val client = OkHttpClient()

    val uuid = UuidManager.getUuid(context) ?: ""

    //----------------------------------------------------------
    // 関数名:messageAnalysisLv1
    // 処理:受け取ったメッセージをlv1 APIへ送信し、そのレスポンスを受け取る
    //----------------------------------------------------------
    fun messageAnalysisLv1(
        text: String,
        callback: (AnalysisResponse, Long) -> Unit,
        onError: (String) -> Unit = {}
    ) {

        val json = JSONObject()
        json.put("message", text)

        val body = json.toString().toRequestBody(
            "application/json".toMediaType()
        )

        // SERVER_URL_LV1:これをAPI(URL)に置き換える
        // SAMPLE_UUID:これを対応しているuuidに置き換える
        val request = Request.Builder()
            .url("https://hackutokyo2026.yoimiya.net/lv1")
            .addHeader("user_uuid", uuid)
            .post(body)
            .build()

        val requestStart = SystemClock.elapsedRealtime()
        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                onError(e.message ?: "通信エラー")
            }

            override fun onResponse(call: Call, response: Response) {

                val result = response.body?.string() ?: ""

                val gson = Gson()
                val analysis = gson.fromJson(result, AnalysisResponse::class.java)

                val elapsedMs = SystemClock.elapsedRealtime() - requestStart
                callback(analysis, elapsedMs)
            }
        })
    }

    fun messageAnalysisLv2(
        text: String,
        callback: (AnalysisResponse, Long) -> Unit,
        onError: (String) -> Unit = {}
    ) {

        val json = JSONObject()
        json.put("message", text)

        val body = json.toString().toRequestBody(
            "application/json".toMediaType()
        )

        // SERVER_URL_LV2:これをAPI(URL)に置き換える
        // SAMPLE_UUID:これを対応しているuuidに置き換える
        val request = Request.Builder()
            .url("https://hackutokyo2026.yoimiya.net/lv2")
            .addHeader("user_uuid", uuid)
            .post(body)
            .build()

        val requestStart = SystemClock.elapsedRealtime()
        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                onError(e.message ?: "通信エラー")
            }

            override fun onResponse(call: Call, response: Response) {

                val result = response.body?.string() ?: ""

                val gson = Gson()
                val analysis = gson.fromJson(result, AnalysisResponse::class.java)

                val elapsedMs = SystemClock.elapsedRealtime() - requestStart
                callback(analysis, elapsedMs)
            }
        })
    }

    fun messageAnalysisLv3(
        text: String,
        callback: (AnalysisResponse, Long) -> Unit,
        onError: (String) -> Unit = {}
    ) {

        val json = JSONObject()
        json.put("message", text)

        val body = json.toString().toRequestBody(
            "application/json".toMediaType()
        )

        // SERVER_URL_LV3:これをAPI(URL)に置き換える
        // SAMPLE_UUID:これを対応しているuuidに置き換える
        val request = Request.Builder()
            .url("https://hackutokyo2026.yoimiya.net/lv3")
            .addHeader("user_uuid", uuid)
            .post(body)
            .build()

        val requestStart = SystemClock.elapsedRealtime()
        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                onError(e.message ?: "通信エラー")
            }

            override fun onResponse(call: Call, response: Response) {

                val result = response.body?.string() ?: ""

                val gson = Gson()
                val analysis = gson.fromJson(result, AnalysisResponse::class.java)

                val elapsedMs = SystemClock.elapsedRealtime() - requestStart
                callback(analysis, elapsedMs)
            }
        })
    }
}