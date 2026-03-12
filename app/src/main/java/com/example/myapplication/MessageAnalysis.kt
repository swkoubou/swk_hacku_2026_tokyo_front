package com.example.myapplication

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import com.google.gson.Gson

class MessageAnalysis {
    private val client = OkHttpClient()

    //----------------------------------------------------------
    // 関数名:messageAnalysisLv1
    // 処理:受け取ったメッセージをlv1 APIへ送信し、そのレスポンスを受け取る
    //----------------------------------------------------------
    fun messageAnalysisLv1(text: String, callback: (AnalysisResponse) -> Unit) {

        val json = JSONObject()
        json.put("message", text)

        val body = json.toString().toRequestBody(
            "application/json".toMediaType()
        )

        // SERVER_URL_LV1:これをAPI(URL)に置き換える
        // SAMPLE_UUID:これを対応しているuuidに置き換える
        val request = Request.Builder()
            .url(BuildConfig.SERVER_URL_LV1)
            .addHeader("user_uuid", BuildConfig.SAMPLE_UUID)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {

                val result = response.body?.string() ?: ""

                val gson = Gson()
                val analysis = gson.fromJson(result, AnalysisResponse::class.java)

                callback(analysis)
            }
        })
    }

    fun messageAnalysisLv2(text: String, callback: (AnalysisResponse) -> Unit) {

        val dummy = AnalysisResponse(
            lv = 2,
            message = text,
            startDate = "2026-03-12",
            startTime = "10:00",
            endDate = "2026-03-12",
            eventName = "ダミーイベントLv2"
        )

        callback(dummy)
    }

    fun messageAnalysisLv3(text: String, callback: (AnalysisResponse) -> Unit) {

        val dummy = AnalysisResponse(
            lv = 3,
            message = text,
            startDate = "2026-03-12",
            startTime = "15:00",
            endDate = "2026-03-12",
            eventName = "ダミーイベントLv3"
        )

        callback(dummy)
    }
}