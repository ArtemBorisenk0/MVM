package com.example.mvm.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mvm.R
import com.example.mvm.models.Task

class TaskAdapter(
    private val tasks: List<Task>,
    private val onTaskClicked: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.taskTitleTextView)
        val descriptionTextView: TextView = itemView.findViewById(R.id.taskDescriptionTextView)
        val typeTextView: TextView = itemView.findViewById(R.id.taskTypeTextView)
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

        // Нажатие на задачу
        holder.itemView.setOnClickListener { onTaskClicked(task) }
    }

    override fun getItemCount(): Int = tasks.size
}
