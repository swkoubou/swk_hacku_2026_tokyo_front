package com.example.myapplication

import com.google.gson.annotations.SerializedName

data class AnalysisResponse(

    val lv: Int,
    val message: String,

    @SerializedName("start_date")
    val startDate: String,

    @SerializedName("start_time")
    val startTime: String,

    @SerializedName("end_date")
    val endDate: String,

    @SerializedName("event_name")
    val eventName: String
)