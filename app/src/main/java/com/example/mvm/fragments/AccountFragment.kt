package com.example.mvm.fragments

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.mvm.databinding.FragmentAccountBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Intent


class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserData()

        // Обработка нажатия на галочку
        binding.nicknameEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                saveNickname() // Сохранение никнейма
                hideKeyboard() // Скрытие клавиатуры
                true
            } else {
                false
            }
        }

        // Обработка кнопки "Выйти"
        binding.logoutButton.setOnClickListener {
            logOut()
        }
    }

    private fun logOut() {
        auth.signOut() // Выход из Firebase
        Toast.makeText(requireContext(), "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show()

        // Переход на экран входа
        val intent = requireActivity().intent
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        requireActivity().finish()
        startActivity(intent)
    }

    private fun loadUserData() {
        val user = auth.currentUser
        if (user != null) {
            binding.emailTextView.text = "Email: ${user.email}"

            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    val nickname = document.getString("nickname") ?: ""
                    binding.nicknameEditText.setText(nickname)
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveNickname() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Пользователь не авторизован", Toast.LENGTH_SHORT).show()
            return
        }

        val newNickname = binding.nicknameEditText.text.toString().trim()
        if (newNickname.isEmpty()) {
            Toast.makeText(requireContext(), "Введите никнейм", Toast.LENGTH_SHORT).show()
            return
        }

        val userProfile = mapOf(
            "nickname" to newNickname,
            "email" to user.email
        )

        db.collection("users").document(user.uid)
            .set(userProfile) // Создаёт документ, если он отсутствует
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Никнейм сохранён", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                exception.printStackTrace()
                Toast.makeText(requireContext(), "Ошибка сохранения никнейма: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun hideKeyboard() {
        val inputMethodManager =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(binding.nicknameEditText.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
