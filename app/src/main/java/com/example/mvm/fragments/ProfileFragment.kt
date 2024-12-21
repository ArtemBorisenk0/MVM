package com.example.mvm.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mvm.R
import com.example.mvm.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserProfile()

        binding.accountButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_accountFragment)
        }
    }

    private fun loadUserProfile() {
        val user = auth.currentUser
        if (user != null) {
            // Получаем дополнительные данные из Firestore
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    val nickname = document.getString("nickname") ?: "Безымянный пользователь"
                    binding.usernameTextView.text = nickname
                }
                .addOnFailureListener {
                    binding.usernameTextView.text = "Ошибка загрузки профиля"
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
