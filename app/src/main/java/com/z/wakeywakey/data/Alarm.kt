package com.z.wakeywakey.data

data class Alarm(
    val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val isEnabled: Boolean = true,
    val repeatDays: String = "",
    val ringtonePath: String = "",
    val mathQuestionCount: Int = 5,
    val isVibrate: Boolean = true
)
