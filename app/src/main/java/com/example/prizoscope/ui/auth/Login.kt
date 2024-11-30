package com.example.prizoscope.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.prizoscope.R
import com.example.prizoscope.data.repository.UserRepository
import kotlinx.android.synthetic.main.activity_login.*

class Login: AppCompatActivity() {

    private lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        userRepository = UserRepository(this)

        btnLogin.setOnClickListener {
            val username = edtUsername.text.toString()
            val password = edtPassword.text.toString()

            if (userRepository.loginUser(username, password)) {
                // Navigate to the main activity
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                // Show error message
                Toast.makeText(this, "Invalid credentials!", Toast.LENGTH_SHORT).show()
            }
        }

        btnSignup.setOnClickListener {
            // Navigate to SignupActivity
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }
}
