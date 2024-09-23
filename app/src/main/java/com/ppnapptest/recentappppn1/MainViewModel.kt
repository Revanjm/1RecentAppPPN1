package com.ppnapptest.recentappppn1

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainViewModel(private val repository: RecentAppsRepository) : ViewModel() {
    val recentApps: LiveData<List<String>> = repository.recentApps

    fun updateRecentApps() {
        viewModelScope.launch {
            repository.updateRecentApps()
        }
    }

    // Новый метод для обновления при нажатии
    fun forceUpdateRecentApps() {
        viewModelScope.launch {
            repository.updateRecentApps()
        }
    }
}
