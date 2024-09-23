package com.ppnapptest.recentappppn1

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.*

@SuppressLint("ForegroundServiceType")
class RecentAppsService : Service() {

    private lateinit var repository: RecentAppsRepository
    private var job: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        repository = RecentAppsRepository(this)

        Toast.makeText(this, "Сервис RecentAppsService запущен", Toast.LENGTH_SHORT).show()

        val notificationHelper = NotificationHelper(this)
        val notification = notificationHelper.createNotification()
        startForeground(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startUpdatingRecentApps()
        return START_STICKY
    }

    private fun startUpdatingRecentApps() {
        job = serviceScope.launch(Dispatchers.IO) {
            try {
                // Запрос root прав через явный вызов su
                if (!Shell.rootAccess()) {
                    Log.e("RecentAppsService", "Root права не получены")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@RecentAppsService, "Нет root прав", Toast.LENGTH_SHORT).show()
                    }
                    stopSelf()
                    return@launch
                }

                while (isActive) {
                    val result = Shell.su("/system/bin/sh /storage/emulated/0/.recentappppn1/.sh/updatMainRA.sh").exec()

                    if (result.isSuccess) {
                        Log.d("RecentAppsService", "Скрипт успешно выполнен")
                        repository.updateRecentApps()
                        delay(3000L)
                    } else {
                        Log.e("RecentAppsService", "Ошибка выполнения скрипта: ${result.code} - ${result.err}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@RecentAppsService, "Ошибка выполнения скрипта: ${result.err}", Toast.LENGTH_SHORT).show()
                        }
                        delay(500L)
                    }
                }
            } catch (e: Exception) {
                Log.e("RecentAppsService", "Ошибка: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RecentAppsService, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        serviceScope.cancel()
        Toast.makeText(this, "Сервис RecentAppsService завершён", Toast.LENGTH_SHORT).show()
    }
}
