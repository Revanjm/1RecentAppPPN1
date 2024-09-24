package com.ppnapptest.recentappppn1

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.util.Log

class MainViewModel(private val repository: RecentAppsRepository) : ViewModel() {

    val recentApps: LiveData<List<String>> = repository.recentApps

    fun updateRecentApps() {
        viewModelScope.launch {
            try {
                // Вызываем обновление данных через скрипт
                repository.updateRecentApps()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error updating recent apps: ${e.message}")
            }
        }
    }

    // Новый метод для принудительного обновления при нажатии
    fun forceUpdateRecentApps() {
        viewModelScope.launch {
            try {
                // Вызываем обновление данных через скрипт
                repository.updateRecentApps()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error forcing update of recent apps: ${e.message}")
            }
        }
    }
}
