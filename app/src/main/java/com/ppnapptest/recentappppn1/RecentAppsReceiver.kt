package com.ppnapptest.recentappppn1

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast

class RecentAppsReceiver : BroadcastReceiver() {

    // Переменная для хранения времени последнего уведомления
    private var lastNotificationTime: Long = 0

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == "android.intent.action.SEND") {
            val message = intent.getStringExtra("run_function")

            // Текущее время
            val currentTime = System.currentTimeMillis()

            // Проверяем, прошло ли 1.5 секунды с момента последнего уведомления
            if (currentTime - lastNotificationTime >= 1500) {
                lastNotificationTime = currentTime

                // Используем NotificationHelper для отправки уведомления
                val notificationHelper = NotificationHelper(context)
                notificationHelper.showNotification("Поймал", "Поймал: $message")

                // Показать Toast
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Поймал: $message", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
