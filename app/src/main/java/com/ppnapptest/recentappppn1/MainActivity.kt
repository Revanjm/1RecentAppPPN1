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
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {
    private val PERMISSION_REQUEST_CODE = 1
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 2
    private val USAGE_STATS_PERMISSION_CODE = 3
    private lateinit var viewModel: MainViewModel
    private lateinit var recentAppsTextView: TextView
    private lateinit var receiver: BroadcastReceiver

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

        // Update UI based on service running state
        val isServiceRunning = isServiceRunning(this, RecentAppsService::class.java)
        updateServiceStatusUI(isServiceRunning, serviceStatusTextView)

        // Start service button
        startServiceButton.setOnClickListener {
            if (!isServiceRunning) {
                val intent = Intent(this, RecentAppsService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
                Toast.makeText(this, "Сервис запущен", Toast.LENGTH_SHORT).show()
                updateServiceStatusUI(true, serviceStatusTextView)
            }
        }

        // Stop service button
        stopServiceButton.setOnClickListener {
            if (isServiceRunning) {
                val intent = Intent(this, RecentAppsService::class.java)
                stopService(intent)
                Toast.makeText(this, "Сервис остановлен", Toast.LENGTH_SHORT).show()
                updateServiceStatusUI(false, serviceStatusTextView)
            }
        }
    }

    private fun initializeBroadcastReceiver() {
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                // Обработка полученного интента с функцией run_function
                val recentAppCl = intent.getStringExtra("run_function")
                recentAppCl?.let {
                    Log.d("MainActivity", "Received data: $it")
                    updateUI(it)
                }
            }
        }

        // Регистрируем ресивер для действия android.intent.action.SEND
        val intentFilter = IntentFilter("android.intent.action.SEND")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, intentFilter)
        }
    }

    private fun updateUI(newData: String) {
        // Обновляем текст на экране с новыми данными
        recentAppsTextView.text = newData
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
            // Запрос root-доступа через su
            Toast.makeText(this, "Получение root-доступа...", Toast.LENGTH_LONG).show()
            Shell.su("su").submit { result ->
                if (result.isSuccess) {
                    Log.d("MainActivity", "Root доступ получен после команды su")
                } else {
                    Toast.makeText(this, "Не удалось получить root-доступ", Toast.LENGTH_LONG).show()
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

        // Проверка и запрос разрешений на использование статистики
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (!hasUsageStatsPermission()) {
                requestUsageStatsPermission()
            }
        }

        // Запрос на разрешение отправки уведомлений
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission()) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
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
        if (!shFolder.exists()) dbFolder.mkdirs()

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
            Toast.makeText(this, "Ошибка при копировании файлов: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to check if the service is running
    private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    // Function to update UI based on service running status
    private fun updateServiceStatusUI(isRunning: Boolean, statusTextView: TextView) {
        if (isRunning) {
            statusTextView.text = "Running"
            statusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        } else {
            statusTextView.text = "Stopped"
            statusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        }
    }
}
