package com.example.prizoscope.data.repository

import android.content.Context
import com.example.prizoscope.utils.DatabaseHelper

class UserRepository(context: Context) {

    private val dbHelper = DatabaseHelper(context)

    fun authenticate(username: String, password: String): Boolean {
        return dbHelper.validateUser(username, password)
    }

    fun addUser(username: String, password: String) {
        dbHelper.addUser(username, password)
    }
    fun registerUser(username: String, password: String): Boolean {
        return dbHelper.addUser(username, password)
    }
}
