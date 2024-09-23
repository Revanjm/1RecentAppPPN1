package com.ppnapptest.recentappppn1

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecentAppsRepository(private val context: Context) {

    private val _recentApps = MutableLiveData<List<String>>()
    val recentApps: LiveData<List<String>> = _recentApps

    suspend fun updateRecentApps() {
        withContext(Dispatchers.IO) {
            try {
                val dbHelper = RecentAppsDbHelper(context)
                val recentAppsList = dbHelper.getRecentApps()

                if (recentAppsList == null) {
                    Log.e("RecentAppsRepository", "Failed to retrieve recent apps. List is null.")
                } else {
                    Log.d("RecentAppsRepository", "Recent apps list: $recentAppsList")
                    withContext(Dispatchers.Main) {
                        _recentApps.value = recentAppsList
                    }
                }
            } catch (e: Exception) {
                Log.e("RecentAppsRepository", "Error updating recent apps: ${e.message}")
            }
        }
    }
}
