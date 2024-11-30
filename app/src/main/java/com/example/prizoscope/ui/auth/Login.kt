package com.example.prizoscope.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.prizoscope.R
import com.example.prizoscope.data.repository.UserRepository
import com.example.prizoscope.databinding.ActivityLoginBinding
import com.example.prizoscope.ui.MainActivity

class Login : AppCompatActivity() {

    private lateinit var userRepository: UserRepository
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize View Binding
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userRepository = UserRepository(this)

        binding.btnLogin.setOnClickListener {
            val username = binding.edtUsername.text.toString().trim()
            val password = binding.edtPassword.text.toString().trim()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                if (userRepository.loginUser(username, password)) {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Invalid credentials!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSignup.setOnClickListener {
            startActivity(Intent(this, Signup::class.java))
        }
    }
}
