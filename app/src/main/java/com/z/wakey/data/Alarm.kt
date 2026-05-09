package com.z.wakey.data

enum class DismissType { MATH, FINGERS }

data class Alarm(
    val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val isEnabled: Boolean = true,
    val repeatDays: String = "",
    val ringtonePath: String = "",
    val mathQuestionCount: Int = 5,
    val isVibrate: Boolean = true,
    val dismissType: DismissType = DismissType.MATH,
    val fingerRoundCount: Int = 5
)
