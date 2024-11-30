package com.example.prizoscope.ui.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.example.prizoscope.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}
