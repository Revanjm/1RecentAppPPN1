package com.ppnapptest.recentappppn1

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper

class RecentAppsReceiver : BroadcastReceiver() {

    private var lastNotificationTime: Long = 0
    private var isReceiverActive = true // Флаг для контроля работы ресивера

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        // Отключение ресивера
        if (action == "android.intent.action.STOP_RECEIVER") {
            isReceiverActive = false
            val notificationHelper = NotificationHelper(context)
            notificationHelper.cancelNotification() // Метод для отмены уведомления
            return
        }

        if (isReceiverActive && action == "android.intent.action.SEND") {
            val message = intent.getStringExtra("run_function")
            if (message != null && message.isNotBlank()) {
                // Update notification with received message
                val notificationHelper = NotificationHelper(context)
                notificationHelper.showNotification("Получено новое приложение", message)
            }
            val currentTime = System.currentTimeMillis()

            // Проверка на null и отправка уведомления
            if (message != null && currentTime - lastNotificationTime >= 1500) {
                lastNotificationTime = currentTime
                val notificationHelper = NotificationHelper(context)
                notificationHelper.showNotification("Поймал", "Поймал: $message")
            }
        }
    }

    fun registerWithIntentFilter(context: Context, fileName: String) {
        val intentFilters = readIntentConfigurations(context, fileName)
        intentFilters.forEach { filter ->
            context.registerReceiver(this, filter)
        }
    }

    private fun readIntentConfigurations(context: Context, fileName: String): List<IntentFilter> {
        val filters = mutableListOf<IntentFilter>()
        context.assets.open(fileName).bufferedReader().useLines { lines ->
            lines.forEach { line ->
                val parts = line.split('=')
                if (parts.size == 3) {
                    val packageName = parts[0].trim()
                    val action = parts[1].trim()
                    val command = parts[2].trim().removeSuffix(";")
                    if (packageName == "*" || packageName == context.packageName) {
                        val filter = IntentFilter(action)
                        filter.addCategory(command)
                        filters.add(filter)
                    }
                }
            }
        }
        return filters
    }
}