package com.example.prizoscope.data.repository

import android.content.Context
import com.example.prizoscope.utils.DatabaseHelper

class UserRepository(private val context: Context) {

    private val dbHelper = DatabaseHelper(context)

    fun registerUser(username: String, password: String): Boolean {
        return dbHelper.addUser(username, password)
    }

    fun loginUser(username: String, password: String): Boolean {
        return dbHelper.validateUser(username, password)
    }

    fun getUserDetails(username: String): Map<String, String>? {
        return dbHelper.getUserDetails(username)
    }
}
