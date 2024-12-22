package com.example.mvm.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.example.mvm.R
import com.example.mvm.models.Task

class TaskAdapter(
    private val tasks: List<Task>,
    private val onTaskCompleted: ((Task) -> Unit)? = null,
    private val onTaskClicked: ((Task) -> Unit)? = null, // Новый обработчик
    private val showCompleteButton: Boolean = true
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.taskTitleTextView)
        val descriptionTextView: TextView = itemView.findViewById(R.id.taskDescriptionTextView)
        val typeTextView: TextView = itemView.findViewById(R.id.taskTypeTextView)
        val valueTextView: TextView = itemView.findViewById(R.id.taskValueTextView)
        val completeButton: Button = itemView.findViewById(R.id.completeTaskButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]

        // Устанавливаем данные задачи
        holder.titleTextView.text = task.title
        holder.descriptionTextView.text = task.description
        holder.typeTextView.text = if (task.isHabit) "Привычка" else "Задача"
        holder.valueTextView.text = "Ценность: ${task.value}"

        // Обработка клика по задаче
        holder.itemView.setOnClickListener {
            onTaskClicked?.invoke(task) // Передаём задачу в обработчик
        }

        // Логика отображения кнопки "Выполнить"
        if (showCompleteButton) {
            holder.completeButton.visibility = View.VISIBLE
            holder.completeButton.setOnClickListener {
                onTaskCompleted?.invoke(task)
            }
        } else {
            holder.completeButton.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = tasks.size
}

