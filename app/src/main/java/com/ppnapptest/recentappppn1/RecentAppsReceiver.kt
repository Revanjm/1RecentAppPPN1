package com.ppnapptest.recentappppn1

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
            val currentTime = System.currentTimeMillis()

            if (currentTime - lastNotificationTime >= 1500) {
                lastNotificationTime = currentTime
                val notificationHelper = NotificationHelper(context)
                notificationHelper.showNotification("Поймал", "Поймал: $message")
            }
        }
    }
}
