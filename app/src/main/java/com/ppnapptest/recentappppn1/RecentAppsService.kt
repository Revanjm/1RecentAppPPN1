package com.ppnapptest.recentappppn1

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.*

class RecentAppsService : Service() {

    private lateinit var repository: RecentAppsRepository
    private var job: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        repository = RecentAppsRepository(this)

        if (Shell.rootAccess()) {
            // Root права получены, уведомления не выводим
            Log.d("RecentAppsService", "Root доступ подтвержден.")
        } else {
            // Если root-доступ не получен, пытаемся его получить через команду su
            val result = Shell.su("su").exec()
            if (!result.isSuccess) {
                Toast.makeText(this, "Не удалось получить Root доступ, сервис остановлен", Toast.LENGTH_SHORT).show()
                stopSelf()
                return
            }
            Toast.makeText(this, "Root доступ получен", Toast.LENGTH_SHORT).show()
        }

        Toast.makeText(this, "Сервис RecentAppsService запущен", Toast.LENGTH_SHORT).show()

        val notificationHelper = NotificationHelper(this)
        val notification = notificationHelper.createNotification()
        startForeground(1, notification)

        // Запуск процесса наблюдения за выполнением скрипта
        observeAndRestartScript()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun observeAndRestartScript() {
        job = serviceScope.launch(Dispatchers.IO) {
            try {
                while (isActive) {
                    val isScriptRunning = Shell.su("pgrep -f updatMainRA.sh").exec().isSuccess

                    if (!isScriptRunning) {
                        Log.d("RecentAppsService", "Скрипт не выполняется, перезапускаем...")
                        startScript()
                    }

                    delay(500L) // Скрипт выполняется с интервалом не меньше 0.5 секунды
                }
            } catch (e: Exception) {
                Log.e("RecentAppsService", "Ошибка наблюдения за скриптом: ${e.message}")
            }
        }
    }

    private fun startScript() {
        try {
            val result = Shell.su("/system/bin/sh /storage/emulated/0/.recentappppn1/.sh/updatMainRA.sh").exec()

            if (result.isSuccess) {
                Log.d("RecentAppsService", "Скрипт успешно запущен")

                // Запуск suspend функции в корутине
                serviceScope.launch {
                    repository.updateRecentApps() // Без аргументов
                }
            } else {
                Log.e("RecentAppsService", "Ошибка запуска скрипта: ${result.err}")
                Toast.makeText(this, "Ошибка запуска скрипта", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("RecentAppsService", "Ошибка при запуске скрипта: ${e.message}")
            Toast.makeText(this, "Ошибка при запуске скрипта", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        serviceScope.cancel()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(1)
        Toast.makeText(this, "Сервис RecentAppsService завершён", Toast.LENGTH_SHORT).show()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }
}
