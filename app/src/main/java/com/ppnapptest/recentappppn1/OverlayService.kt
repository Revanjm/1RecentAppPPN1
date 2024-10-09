package com.ppnapptest.recentappppn1

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import android.text.TextUtils

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var receiver: BroadcastReceiver
    private lateinit var vibrator: Vibrator

    override fun onCreate() {
        super.onCreate()

        // Инициализация WindowManager и добавление оверлея
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)

        // Инициализация вибратора
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

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
        // Очищаем предыдущие строки
        (overlayView as LinearLayout).removeAllViews()

        // Разбиваем строку на несколько строк
        val formattedData = data.split("\n")

        // Для каждой строки создаём отдельный TextView
        formattedData.forEach { item ->
            val textView = TextView(this)
            textView.text = item
            textView.textSize = 10f
            textView.setTextColor(Color.WHITE)
            textView.maxLines = 1
            textView.ellipsize = TextUtils.TruncateAt.END
            textView.setBackgroundResource(R.drawable.text_border) // Граница для каждой строки
            textView.setPadding(5, 5, 5, 5)

            // Устанавливаем действие при клике
            textView.setOnClickListener {
                // Виброотклик
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrator.vibrate(100)
                }

                // Выполняем команду
                val command = item.substringAfter(" ")
                try {
                    val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "am start $command"))
                    process.waitFor()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Добавляем TextView в родительский LinearLayout
            (overlayView as LinearLayout).addView(textView)
        }
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
