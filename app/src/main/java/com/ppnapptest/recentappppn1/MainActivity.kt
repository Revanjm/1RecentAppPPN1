package com.ppnapptest.recentappppn1

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.topjohnwu.superuser.Shell
import java.io.File
import java.io.IOException
import android.view.View

class MainActivity : AppCompatActivity() {
    private val PERMISSION_REQUEST_CODE = 1
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 2
    private val USAGE_STATS_PERMISSION_CODE = 3
    private lateinit var viewModel: MainViewModel
    private lateinit var recentAppsTextView: TextView
    private lateinit var receiver: BroadcastReceiver

    // Предыдущее состояние строк
    private var previousData: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Отключаем аппаратное ускорение
        window.setFlags(0, 0)

        recentAppsTextView = findViewById(R.id.recentAppsTextView)
        viewModel = ViewModelProvider(this, MainViewModelFactory(RecentAppsRepository(this)))[MainViewModel::class.java]
        viewModel.recentApps.observe(this) { data ->
            Log.d("MainActivity", "Recent apps: $data")
            recentAppsTextView.text = data.joinToString(separator = "\n") { app ->
                app.takeUnless { it == "NULL" || it.contains("/") } ?: "Invalid app data"
            }.ifEmpty { "Список последних приложений пуст" }
        }

        initializeBroadcastReceiver()
        checkPermissions()
        setupAppFolders()

        val startServiceButton: Button = findViewById(R.id.startServiceButton)
        val stopServiceButton: Button = findViewById(R.id.stopServiceButton)
        val serviceStatusTextView: TextView = findViewById(R.id.serviceStatusTextView)

        // Обновляем UI в зависимости от состояния сервиса
        var isServiceRunning = isServiceRunning(this, RecentAppsService::class.java)
        updateServiceStatusUI(isServiceRunning, serviceStatusTextView)

        // Кнопка запуска сервиса
        startServiceButton.setOnClickListener {
            if (!isServiceRunning) {
                val intent = Intent(this, RecentAppsService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
                Toast.makeText(this, "Сервис запущен", Toast.LENGTH_SHORT).show() // Toast для старта сервиса
                updateServiceStatusUI(true, serviceStatusTextView)
                isServiceRunning = true
            }
        }

        // Кнопка остановки сервиса
        stopServiceButton.setOnClickListener {
            if (isServiceRunning) {
                val intent = Intent(this, RecentAppsService::class.java)
                stopService(intent)
                Toast.makeText(this, "Сервис остановлен", Toast.LENGTH_SHORT).show() // Toast для остановки сервиса
                updateServiceStatusUI(false, serviceStatusTextView)
                isServiceRunning = false
            }
        }
    }

    private fun initializeBroadcastReceiver() {
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.d("MainActivity", "Intent received")
                val recentAppCl = intent.getStringExtra("run_function")
                if (recentAppCl != null) {
                    updateUI(recentAppCl)
                } else {
                    Log.d("MainActivity", "run_function is null")
                }
            }
        }

        // Регистрируем ресивер для действия как внешних, так и внутренних интентов
        val intentFilter = IntentFilter().apply {
            addAction("com.ppnapptest.recentappppn1.CUSTOM_INTENT") // кастомный интент для внутреннего использования
            addAction(Intent.ACTION_SEND) // системный интент для внешних приложений
        }

        // Проверка на версию Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, intentFilter)
        }
    }

    private fun updateUI(newData: String) {
        // Разделяем строку по разделителю (например, если это список приложений через \n)
        val currentData = newData.split("\n")

        // Загружаем анимации
        val slideVerticalAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_vertical)  // Анимация перемещения по вертикали
        val slideOutLeftAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_out_left)  // Анимация для старых элементов
        val slideInRightAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)  // Анимация для новых элементов

        // Найдем строки таблицы (правая колонка)
        val textView1 = findViewById<TextView>(R.id.text_view_1)
        val textView2 = findViewById<TextView>(R.id.text_view_2)
        val textView3 = findViewById<TextView>(R.id.text_view_3)
        val textView4 = findViewById<TextView>(R.id.text_view_4)
        val textView5 = findViewById<TextView>(R.id.text_view_5)
        val textView6 = findViewById<TextView>(R.id.text_view_6)
        val textView7 = findViewById<TextView>(R.id.text_view_7)
        val textView8 = findViewById<TextView>(R.id.text_view_8)
        val textView9 = findViewById<TextView>(R.id.text_view_9)
        val textView10 = findViewById<TextView>(R.id.text_view_10)

        val textViews = listOf(textView1, textView2, textView3, textView4, textView5, textView6, textView7, textView8, textView9, textView10)

        // Проходим по предыдущим данным и применяем анимации
        previousData.forEachIndexed { index, previousItem ->
            if (currentData.contains(previousItem)) {
                // Строка осталась, применяем анимацию перемещения по вертикали
                textViews.getOrNull(index)?.startAnimation(slideVerticalAnimation)
            } else {
                // Строка не пришла в новом обновлении, уезжает влево
                textViews.getOrNull(index)?.startAnimation(slideOutLeftAnimation)
            }
        }

        // Обновляем текст строк и показываем их снова (с учётом новых данных)
        textViews.forEachIndexed { index, textView ->
            val text = currentData.getOrNull(index) ?: ""

            // Если элемент новый, применяем анимацию появления справа
            if (previousData.size <= index || previousData[index] != text) {
                textView.startAnimation(slideInRightAnimation)
            }
            textView.text = text
        }

        // Сохраняем текущее состояние как предыдущее
        previousData = currentData

        Log.d("MainActivity", "UI обновлено с данными: $newData")
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }


    private fun checkPermissions() {
        // Проверка рут-доступа
        if (Shell.rootAccess()) {
            Log.d("MainActivity", "Root доступ получен")
        } else {
            Shell.su("su").submit { result ->
                if (!result.isSuccess) {
                    finish()
                }
            }
        }

        val permissionsToRequest = mutableListOf<String>()
        val permissions = arrayOf(
            Manifest.permission.MANAGE_EXTERNAL_STORAGE,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECEIVE_BOOT_COMPLETED,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.SYSTEM_ALERT_WINDOW,
            Manifest.permission.ACCESS_NOTIFICATION_POLICY
        )

        permissions.forEach { perm ->
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(perm)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !hasUsageStatsPermission()) {
            requestUsageStatsPermission()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOpsManager.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsageStatsPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        startActivityForResult(intent, USAGE_STATS_PERMISSION_CODE)
    }

    private fun hasNotificationPermission(): Boolean {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.areNotificationsEnabled()
    }

    private fun setupAppFolders() {
        val dbFolder = File(Environment.getExternalStorageDirectory().absolutePath + "/.recentappppn1/.db/")
        val shFolder = File(Environment.getExternalStorageDirectory().absolutePath + "/.recentappppn1/.sh/")

        if (!dbFolder.exists()) dbFolder.mkdirs()
        if (!shFolder.exists()) shFolder.mkdirs()

        copyAssetFolder("db", dbFolder.absolutePath)
        copyAssetFolder("sh", shFolder.absolutePath)
    }

    private fun copyAssetFolder(assetFolder: String, destinationPath: String) {
        try {
            val assets = assets.list(assetFolder) ?: return
            for (asset in assets) {
                val file = File("$destinationPath/$asset")
                if (!file.exists()) {
                    val assetManager = this.assets
                    val inStream = assetManager.open("$assetFolder/$asset")
                    val outStream = file.outputStream()
                    inStream.copyTo(outStream)
                    outStream.close()
                    inStream.close()
                }
            }
        } catch (e: IOException) {
            Log.e("MainActivity", "Error copying assets: ${e.message}")
        }
    }

    private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun updateServiceStatusUI(isRunning: Boolean, statusTextView: TextView) {
        if (isRunning) {
            statusTextView.text = "Running"
            statusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
            statusTextView.setBackgroundColor(ContextCompat.getColor(this, android.R.color.black))
        } else {
            statusTextView.text = "Stopped"
            statusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            statusTextView.setBackgroundColor(ContextCompat.getColor(this, android.R.color.black))
        }
    }
}
