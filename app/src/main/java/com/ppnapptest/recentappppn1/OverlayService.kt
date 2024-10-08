package com.ppnapptest.recentappppn1

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import android.util.Log

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private lateinit var overlayTextView: TextView
    private var isOverlayActive = false

    override fun onCreate() {
        super.onCreate()

        // Создаём WindowManager для управления оверлеем
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Инициализация оверлея
        addOverlayView()
        isOverlayActive = true
    }

    private fun addOverlayView() {
        Log.d("OverlayService", "Добавление оверлея")
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.overlay_layout, null)

        // Получаем ссылку на текстовое поле
        overlayTextView = overlayView?.findViewById(R.id.overlay_text_view) ?: return

        // Параметры для отображения оверлея
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.RIGHT // Размещаем оверлей в правом верхнем углу

        // Добавляем оверлей в окно
        windowManager?.addView(overlayView, params)
        Log.d("OverlayService", "Оверлей добавлен")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Обновляем данные оверлея, если оверлей активен
        if (isOverlayActive) {
            intent?.getStringArrayListExtra("overlay_data")?.let {
                Log.d("OverlayService", "Данные для обновления оверлея: $it")
                updateOverlay(it)
            }
        }
        return START_STICKY
    }

    private fun updateOverlay(data: List<String>) {
        // Устанавливаем переданный список данных в TextView
        overlayTextView.text = data.joinToString(separator = "\n")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (overlayView != null) {
            windowManager?.removeView(overlayView)
            overlayView = null
        }
        isOverlayActive = false
        Toast.makeText(this, "Оверлей закрыт", Toast.LENGTH_SHORT).show()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
