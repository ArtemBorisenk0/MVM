
package com.example.mvm.models
import com.google.firebase.firestore.PropertyName

data class Task(
    val title: String = "",
    val description: String = "-",
    @PropertyName("habit") val isHabit: Boolean = false,
    val completedDate: Long? = null, // Время выполнения в миллисекундах (timestamp)
    val value: Int = 1 // поле для ценности задачи
)
