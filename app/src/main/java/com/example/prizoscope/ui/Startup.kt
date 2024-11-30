package com.example.prizoscope.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.prizoscope.databinding.ActivityStartupBinding
import com.example.prizoscope.ui.auth.Login
import com.example.prizoscope.ui.auth.Signup

class StartupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStartupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStartupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginButton.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
        }

        binding.signupButton.setOnClickListener {
            startActivity(Intent(this, Signup::class.java))
        }
    }
}
