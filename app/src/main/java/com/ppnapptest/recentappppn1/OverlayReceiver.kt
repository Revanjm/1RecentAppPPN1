package com.ppnapptest.recentappppn1

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class OverlayReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("OverlayReceiver", "Intent received")

        // Проверяем, что action - это SEND
        if (intent.action == "android.intent.action.SEND") {
            // Получаем данные из intent
            val recentApps = intent.getStringExtra("run_function")

            if (recentApps != null && recentApps.isNotEmpty()) {
                Log.d("OverlayReceiver", "Received recent apps: $recentApps")

                // Разбиваем приложения на строки (по пробелам или другим разделителям)
                val recentAppsList = recentApps.split("\n").joinToString("\n")

                // Отправляем данные в OverlayService для обновления
                val overlayIntent = Intent(context, OverlayService::class.java).apply {
                    putExtra("run_function", recentAppsList) // Передаем данные через ключ "run_function"
                }
                context.startService(overlayIntent)

                Log.d("OverlayReceiver", "Started OverlayService with recent apps list")
            } else {
                Log.d("OverlayReceiver", "No recent apps data found in the intent")
            }
        } else {
            Log.d("OverlayReceiver", "Unexpected intent action: ${intent.action}")
        }
    }
}
