package com.ppnapptest.recentappppn1

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecentAppsRepository(private val context: Context) {

    private val _recentApps = MutableLiveData<List<String>>()
    val recentApps: LiveData<List<String>> = _recentApps

    suspend fun updateRecentApps() {
        withContext(Dispatchers.IO) {
            try {
                // Запускаем скрипт для получения списка последних приложений
                val result = Shell.su("/system/bin/sh /storage/emulated/0/.recentappppn1/.sh/updatMainRA.sh").exec()

                if (result.isSuccess) {
                    val output = result.out.joinToString("\n")
                    val recentAppsList = output.split("/=/").filter { it.isNotEmpty() }

                    Log.d("RecentAppsRepository", "Recent apps list: $recentAppsList")

                    withContext(Dispatchers.Main) {
                        _recentApps.value = recentAppsList
                    }
                } else {
                    Log.e("RecentAppsRepository", "Error executing script: ${result.err}")
                }
            } catch (e: Exception) {
                Log.e("RecentAppsRepository", "Error updating recent apps: ${e.message}")
            }
        }
    }
}
