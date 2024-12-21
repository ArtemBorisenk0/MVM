package com.example.mvm.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mvm.adapters.TaskAdapter
import com.example.mvm.databinding.DialogAddTaskBinding
import com.example.mvm.databinding.FragmentHomeBinding
import com.example.mvm.models.Task
import com.example.mvm.models.HomeViewModel
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var taskAdapter: TaskAdapter
    private val taskList = mutableListOf<Task>()
    private val db = FirebaseFirestore.getInstance()




    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?


    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Инициализация RecyclerView
        setupRecyclerView()

        // Кнопка для добавления задачи/привычки
        binding.addTaskButton.setOnClickListener {
            showAddTaskDialog()
        }

        // Загрузка задач из Firebase
        loadTasksFromFirebase()

        return root
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            taskList,
            onTaskCompleted = { task -> completeTask(task) },
            showCompleteButton = true // Отображаем кнопку
        )
        binding.tasksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.tasksRecyclerView.adapter = taskAdapter
    }





    private fun showAddTaskDialog() {
        val dialogBinding = DialogAddTaskBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Добавить задачу или привычку")
            .setView(dialogBinding.root)
            .setPositiveButton("Сохранить") { _, _ ->
                val title = dialogBinding.taskTitleEditText.text.toString()
                val description = dialogBinding.taskDescriptionEditText.text.toString()
                val isHabit = dialogBinding.taskTypeSwitch.isChecked // True = привычка, False = задача

                if (title.isNotEmpty()) {
                    val task = Task(title, description, isHabit)
                    saveTaskToFirebase(task)
                } else {
                    Toast.makeText(requireContext(), "Название задачи обязательно", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .create()

        dialog.show()
    }


    private fun saveTaskToFirebase(task: Task) {
        db.collection("tasks")
            .add(task)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Задача добавлена", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Ошибка сохранения", Toast.LENGTH_SHORT).show()
            }
    }



    private fun loadTasksFromFirebase() {
        db.collection("tasks")
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    Toast.makeText(requireContext(), "Ошибка загрузки задач", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (querySnapshot != null) {
                    taskList.clear() // Очищаем локальный список
                    for (document in querySnapshot.documents) {
                        val task = document.toObject(Task::class.java)
                        if (task != null) {
                            taskList.add(task) // Добавляем задачу в локальный список
                        }
                    }
                    taskAdapter.notifyDataSetChanged() // Обновляем адаптер
                }
            }
    }

    private fun showEditTaskDialog(task: Task) {
        val dialogBinding = DialogAddTaskBinding.inflate(layoutInflater)
        dialogBinding.taskTitleEditText.setText(task.title)
        dialogBinding.taskDescriptionEditText.setText(task.description)
        dialogBinding.taskTypeSwitch.isChecked = task.isHabit

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Редактировать задачу")
            .setView(dialogBinding.root)
            .setPositiveButton("Сохранить") { _, _ ->
                val updatedTitle = dialogBinding.taskTitleEditText.text.toString()
                val updatedDescription = dialogBinding.taskDescriptionEditText.text.toString()
                val updatedIsHabit = dialogBinding.taskTypeSwitch.isChecked

                if (updatedTitle.isNotEmpty()) {
                    val updatedTask = task.copy(
                        title = updatedTitle,
                        description = updatedDescription,
                        isHabit = updatedIsHabit
                    )
                    updateTaskInFirebase(task, updatedTask)
                } else {
                    Toast.makeText(requireContext(), "Название задачи обязательно", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .setNeutralButton("Удалить") { _, _ ->
                deleteTask(task)
            }
            .create()

        dialog.show()
    }



    private fun deleteTask(task: Task) {
        db.collection("tasks")
            .whereEqualTo("title", task.title) // Найти документ по заголовку (лучше использовать ID, если доступен)
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    db.collection("tasks").document(document.id).delete()
                        .addOnSuccessListener {
                            taskList.remove(task)
                            taskAdapter.notifyDataSetChanged()
                            Toast.makeText(requireContext(), "Задача удалена", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Ошибка удаления задачи", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateTaskInFirebase(oldTask: Task, updatedTask: Task) {
        db.collection("tasks")
            .whereEqualTo("title", oldTask.title) // Найти документ по заголовку
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    db.collection("tasks").document(document.id).set(updatedTask)
                        .addOnSuccessListener {
                            val index = taskList.indexOf(oldTask)
                            if (index != -1) {
                                taskList[index] = updatedTask
                                taskAdapter.notifyItemChanged(index)
                            }
                            Toast.makeText(requireContext(), "Задача обновлена", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Ошибка обновления задачи", Toast.LENGTH_SHORT).show()
            }
    }

    private fun completeTask(task: Task) {
        val completedTask = task.copy(completedDate = System.currentTimeMillis())

        db.collection("archive")
            .add(completedTask) // Сохраняем выполненный элемент в архив
            .addOnSuccessListener {
                if (!task.isHabit) { // Только задачи удаляются
                    db.collection("tasks")
                        .whereEqualTo("title", task.title)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            for (document in querySnapshot.documents) {
                                db.collection("tasks").document(document.id).delete()
                                    .addOnSuccessListener {
                                        taskList.remove(task)
                                        taskAdapter.notifyDataSetChanged()
                                        Toast.makeText(requireContext(), "Задача выполнена и сохранена в архив", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                } else {
                    // Привычки остаются в списке, но сохраняются в архив
                    Toast.makeText(requireContext(), "Привычка сохранена в архив как выполненная", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Ошибка сохранения в архив", Toast.LENGTH_SHORT).show()
            }
    }





    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
