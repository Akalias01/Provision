package com.example.rezon.ui.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.rezon.ui.theme.RezonThemeOption
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor() : ViewModel() {
    // In a real app, save this to DataStore
    var currentTheme = mutableStateOf(RezonThemeOption.NeonCyber)

    fun setTheme(theme: RezonThemeOption) {
        currentTheme.value = theme
    }
}
