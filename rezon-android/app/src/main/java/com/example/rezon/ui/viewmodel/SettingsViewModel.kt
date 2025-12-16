package com.example.rezon.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rezon.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {

    val keepServiceActive = repository.keepServiceActive.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val stopOnClose = repository.stopOnClose.stateIn(viewModelScope, SharingStarted.Lazily, true)
    val showCoverOnLock = repository.showCoverOnLock.stateIn(viewModelScope, SharingStarted.Lazily, true)
    val wifiOnly = repository.wifiOnly.stateIn(viewModelScope, SharingStarted.Lazily, true)

    fun toggleKeepService(value: Boolean) = viewModelScope.launch { repository.setKeepServiceActive(value) }
    fun toggleStopOnClose(value: Boolean) = viewModelScope.launch { repository.setStopOnClose(value) }
    fun toggleShowCover(value: Boolean) = viewModelScope.launch { repository.setShowCoverOnLock(value) }
    fun toggleWifiOnly(value: Boolean) = viewModelScope.launch { repository.setWifiOnly(value) }
}
