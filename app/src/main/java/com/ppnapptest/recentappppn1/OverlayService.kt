package com.ppnapptest.recentappppn1

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var overlayTextView: TextView
    private lateinit var receiver: BroadcastReceiver

    override fun onCreate() {
        super.onCreate()

        // Инициализация WindowManager и добавление оверлея
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)
        overlayTextView = overlayView.findViewById(R.id.overlay_text)

        // Параметры для оверлея
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, // Для API 26+
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        // Размещение оверлея в правом верхнем углу
        params.gravity = Gravity.TOP or Gravity.END

        // Добавляем оверлей на экран
        windowManager.addView(overlayView, params)

        // Инициализация и регистрация ресивера для получения данных
        initializeBroadcastReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Получаем данные из интента
        val recentAppsData = intent?.getStringExtra("run_function")
        recentAppsData?.let {
            // Обновляем текст оверлея
            updateOverlayText(it)
        }

        return START_STICKY
    }

    private fun initializeBroadcastReceiver() {
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.d("OverlayService", "Intent received")
                val recentAppCl = intent.getStringExtra("run_function")
                recentAppCl?.let {
                    updateOverlayText(it)
                } ?: Log.d("OverlayService", "run_function is null")
            }
        }

        val intentFilter = IntentFilter().apply {
            addAction("com.ppnapptest.recentappppn1.CUSTOM_INTENT")
            addAction(Intent.ACTION_SEND)
        }

        // Проверка версии SDK и регистрация ресивера с правильным флагом
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, intentFilter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, intentFilter)
        }
    }

    private fun updateOverlayText(data: String) {
        // Разбиваем строку на строки для отображения построчно
        val formattedData = data.split("\n").joinToString("\n")
        overlayTextView.text = formattedData
    }

    override fun onDestroy() {
        super.onDestroy()
        // Удаляем оверлей при уничтожении сервиса
        if (this::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
        // Отменяем регистрацию ресивера
        unregisterReceiver(receiver)
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Сервис не поддерживает привязку
        return null
    }
}
