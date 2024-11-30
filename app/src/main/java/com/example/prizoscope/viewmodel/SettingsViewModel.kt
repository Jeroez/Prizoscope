package com.example.prizoscope.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prizoscope.utils.PreferencesManager
import kotlinx.coroutines.launch

class SettingsViewModel(private val preferencesManager: PreferencesManager) : ViewModel() {
    fun setDarkModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setDarkMode(enabled)
        }
    }

    fun isDarkModeEnabled(): Boolean {
        return preferencesManager.isDarkModeEnabled()
    }
}
