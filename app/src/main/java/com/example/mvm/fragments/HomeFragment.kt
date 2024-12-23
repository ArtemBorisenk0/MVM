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
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.util.Calendar
import android.util.Log
import com.example.mvm.R
import com.google.firebase.auth.FirebaseAuth




class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var taskAdapter: TaskAdapter
    private val taskList = mutableListOf<Task>()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View> // добавил только что



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Инициализация BottomSheet
        val bottomSheet = binding.bottomSheet
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)

        // Устанавливаем начальное состояние
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        // Настраиваем минимальную высоту (1/4 экрана)
        bottomSheetBehavior.peekHeight = (resources.displayMetrics.heightPixels * 0.25).toInt()

        // Обработка событий BottomSheet
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        // BottomSheet развернут
                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        // BottomSheet свернут
                    }
                    BottomSheetBehavior.STATE_DRAGGING -> {
                        // BottomSheet в процессе перетаскивания
                    }
                    BottomSheetBehavior.STATE_SETTLING -> {
                        // BottomSheet в процессе установки конечного состояния
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Этот метод можно оставить пустым, если не требуется обработка
            }
        })

        // Инициализация RecyclerView
        setupRecyclerView()

        // Кнопка для добавления задачи/привычки
        binding.addTaskButton.setOnClickListener {
            showAddTaskDialog()
        }

        // Загрузка задач из Firebase
        loadTasksFromFirebase()

        calculateDailyPlan() // Пересчёт плана на день при загрузке

        return root
    }


    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            taskList,
            onTaskCompleted = { task -> completeTask(task) },
            onTaskClicked = { task -> showEditTaskDialog(task) }, // Обработчик клика для редактирования
            showCompleteButton = true
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
                val value = dialogBinding.taskValueSeekBar.progress + 1 // Получаем значение ценности (от 1 до 4)

                if (title.isNotEmpty()) {
                    val task = Task(title, description, isHabit, value = value) // Добавляем ценность в задачу
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
                loadTasksFromFirebase()
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

        // Заполняем текущие данные задачи
        dialogBinding.taskTitleEditText.setText(task.title)
        dialogBinding.taskDescriptionEditText.setText(task.description)
        dialogBinding.taskTypeSwitch.isChecked = task.isHabit
        dialogBinding.taskValueSeekBar.progress = task.value - 1

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Редактировать задачу")
            .setView(dialogBinding.root)
            .setPositiveButton("Сохранить") { _, _ ->
                val updatedTitle = dialogBinding.taskTitleEditText.text.toString()
                val updatedDescription = dialogBinding.taskDescriptionEditText.text.toString()
                val updatedIsHabit = dialogBinding.taskTypeSwitch.isChecked
                val updatedValue = dialogBinding.taskValueSeekBar.progress + 1

                if (updatedTitle.isNotEmpty()) {
                    val updatedTask = task.copy(
                        title = updatedTitle,
                        description = updatedDescription,
                        isHabit = updatedIsHabit,
                        value = updatedValue
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
    private fun getStartOfDayTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getEndOfDayTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }


    private fun updateDailyPlanUI(totalValue: Int, maxPlanValue: Int) {
        val progressBar = binding.dailyPlanProgressBar
        val progressTextView = binding.dailyPlanTextView
        val planStatusImageView = binding.planStatusImageView

        progressBar.max = maxPlanValue
        progressBar.progress = totalValue

        // Обновляем текстовое поле
        progressTextView.text = "$totalValue / $maxPlanValue"

        // Изменяем изображение на основе состояния плана
        if (totalValue >= maxPlanValue && totalValue > 0) {
            planStatusImageView.setImageResource(R.drawable.ic_plan_completed)
            Toast.makeText(requireContext(), "План на день выполнен!", Toast.LENGTH_SHORT).show()
        } else {
            planStatusImageView.setImageResource(R.drawable.ic_plan_not_completed)
        }

        Log.d("DailyPlan", "Progress updated: $totalValue / $maxPlanValue")
    }




    private fun loadDailyPlanValue(callback: (Int) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val dailyPlanValue = document.getLong("dailyPlanValue")?.toInt() ?: 20 // По умолчанию 20
                callback(dailyPlanValue)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Не удалось загрузить план на день", Toast.LENGTH_SHORT).show()
                callback(20) // Устанавливаем значение по умолчанию при ошибке
            }
    }


    private fun calculateDailyPlan() {
        val todayStart = getStartOfDayTimestamp() // Начало дня
        val todayEnd = getEndOfDayTimestamp() // Конец дня

        db.collection("archive")
            .whereGreaterThanOrEqualTo("completedDate", todayStart)
            .whereLessThanOrEqualTo("completedDate", todayEnd)
            .get()
            .addOnSuccessListener { querySnapshot ->
                // Если данных нет, обновляем прогресс как 0
                if (querySnapshot.isEmpty) {
                    Log.d("DailyPlan", "No tasks completed today")
                    loadDailyPlanValue { maxPlanValue ->
                        updateDailyPlanUI(0, maxPlanValue)
                    }
                    return@addOnSuccessListener
                }

                // Если данные есть, рассчитываем сумму ценностей задач
                var totalValue = 0
                for (document in querySnapshot.documents) {
                    val task = document.toObject(Task::class.java)
                    if (task != null) {
                        totalValue += task.value
                        Log.d("DailyPlan", "Task value added: ${task.value}") // Лог для проверки
                    }
                }

                Log.d("DailyPlan", "Total value for today: $totalValue") // Лог общего значения
                loadDailyPlanValue { maxPlanValue ->
                    updateDailyPlanUI(totalValue, maxPlanValue) // Обновляем UI с данными
                }
            }
            .addOnFailureListener { exception ->
                Log.e("DailyPlan", "Error loading tasks: ${exception.message}")
                Toast.makeText(requireContext(), "Ошибка загрузки выполненных задач", Toast.LENGTH_SHORT).show()
                loadDailyPlanValue { maxPlanValue ->
                    updateDailyPlanUI(0, maxPlanValue) // Устанавливаем прогресс в 0 при ошибке
                }
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
                                        calculateDailyPlan() // Пересчёт плана на день
                                    }
                            }
                        }
                } else {
                    // Привычки остаются в списке, но сохраняются в архив
                    Toast.makeText(requireContext(), "Привычка сохранена в архив как выполненная", Toast.LENGTH_SHORT).show()
                    calculateDailyPlan() // Пересчёт плана на день
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
