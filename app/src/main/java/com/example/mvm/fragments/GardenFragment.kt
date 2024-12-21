package com.example.mvm.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mvm.adapters.TaskAdapter
import com.example.mvm.databinding.FragmentGardenBinding
import com.example.mvm.models.Task
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar


class GardenFragment : Fragment() {

    private var _binding: FragmentGardenBinding? = null
    private val binding get() = _binding!!

    private lateinit var taskAdapter: TaskAdapter
    private val completedTasks = mutableListOf<Task>()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGardenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        // Устанавливаем слушатель для CalendarView
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse("$year-${month + 1}-$dayOfMonth")
            loadCompletedTasks(selectedDate.time)
        }
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            completedTasks,
            showCompleteButton = false // Скрываем кнопку
        )
        binding.completedTasksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.completedTasksRecyclerView.adapter = taskAdapter
    }



    private fun getStartOfDay(date: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getEndOfDay(date: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = date
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    private fun loadCompletedTasks(selectedDate: Long) {
        // Определяем начало и конец выбранного дня
        val startOfDay = getStartOfDay(selectedDate)
        val endOfDay = getEndOfDay(selectedDate)

        db.collection("archive")
            .whereGreaterThanOrEqualTo("completedDate", startOfDay)
            .whereLessThanOrEqualTo("completedDate", endOfDay)
            .get()
            .addOnSuccessListener { querySnapshot ->
                completedTasks.clear()
                for (document in querySnapshot.documents) {
                    val task = document.toObject(Task::class.java)
                    if (task != null) {
                        completedTasks.add(task) // Добавляем выполненные привычки и задачи
                    }
                }
                taskAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Ошибка загрузки задач и привычек", Toast.LENGTH_SHORT).show()
            }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
