package com.example.mvm.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mvm.R
import com.example.mvm.models.Task
import android.widget.Button


class TaskAdapter(
    private val tasks: List<Task>,
    private val onTaskCompleted: ((Task) -> Unit)? = null,
    private val showCompleteButton: Boolean = true // Новое свойство
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.taskTitleTextView)
        val descriptionTextView: TextView = itemView.findViewById(R.id.taskDescriptionTextView)
        val typeTextView: TextView = itemView.findViewById(R.id.taskTypeTextView)
        val completeButton: Button = itemView.findViewById(R.id.completeTaskButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.titleTextView.text = task.title
        holder.descriptionTextView.text = task.description
        holder.typeTextView.text = if (task.isHabit) "Привычка" else "Задача"

        // Логика отображения кнопки
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


