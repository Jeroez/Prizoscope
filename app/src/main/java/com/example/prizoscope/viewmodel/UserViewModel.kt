package com.example.prizoscope.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.prizoscope.data.repository.UserRepository

class UserViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val _isLoggedIn = MutableLiveData<Boolean>()
    val isLoggedIn: LiveData<Boolean> = _isLoggedIn

    fun login(username: String, password: String) {
        val result = userRepository.authenticate(username, password)
        _isLoggedIn.value = result
    }

    fun signUp(username: String, password: String) {
        userRepository.addUser(username, password)
    }
}
