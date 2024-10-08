package com.ppnapptest.recentappppn1

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class OverlayReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val overlayData = intent.getStringArrayListExtra("overlay_data")
        Log.d("OverlayReceiver", "Полученные данные для оверлея: $overlayData")

        if (overlayData != null) {
            // Отправляем данные в OverlayService для обновления оверлея
            val serviceIntent = Intent(context, OverlayService::class.java)
            serviceIntent.putStringArrayListExtra("overlay_data", overlayData)
            context.startService(serviceIntent)
        } else {
            Log.d("OverlayReceiver", "Нет данных для оверлея")
        }
    }
}
