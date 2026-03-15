package com.example.myapplication

data class Task(
    val user_uuid: String,
    val task_uuid: String,
    val start_date: String,
    val start_time: String,
    val end_date: String,
    val event_name: String,
    val done: Boolean
)