package com.example.rezon.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rezon.data.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: LibraryRepository
) : ViewModel() {

    val allBooks = repository.allBooks

    fun scanLibrary() {
        viewModelScope.launch {
            repository.scanDeviceForAudiobooks()
        }
    }
}
