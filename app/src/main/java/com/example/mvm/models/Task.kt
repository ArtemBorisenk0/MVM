package com.example.mvm.models

data class Task(
    val title: String = "",
    val description: String = "-",
    val isHabit: Boolean = false // True = привычка, False = задача
)
