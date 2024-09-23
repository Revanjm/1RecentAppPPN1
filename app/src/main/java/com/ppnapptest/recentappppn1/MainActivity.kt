package com.ppnapptest.recentappppn1

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
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

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 1
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 2
    private lateinit var viewModel: MainViewModel
    private lateinit var recentAppsTextView: TextView
    private lateinit var serviceStatusTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recentAppsTextView = findViewById(R.id.recentAppsTextView)
        serviceStatusTextView = findViewById(R.id.serviceStatusTextView)

        viewModel = ViewModelProvider(this, MainViewModelFactory(RecentAppsRepository(this)))[MainViewModel::class.java]

        viewModel.recentApps.observe(this) { data ->
            Log.d("MainActivity", "Recent apps: $data")
            recentAppsTextView.text = if (data.isNotEmpty()) {
                data.filterNot { it == "NULL" || it.contains("/") }.filterNotNull().filter { it.isNotEmpty() }.joinToString("\n")
            } else {
                "Список последних приложений пуст"
            }
        }

        val startServiceButton: Button = findViewById(R.id.startServiceButton)
        val stopServiceButton: Button = findViewById(R.id.stopServiceButton)

        startServiceButton.setOnClickListener {
            vibrate()
            if (!isServiceRunning(RecentAppsService::class.java)) {
                startService(Intent(this, RecentAppsService::class.java))
                updateServiceStatus(true) // Обновляем статус сервиса как запущен
            }
            viewModel.updateRecentApps()
        }

        stopServiceButton.setOnClickListener {
            vibrate()
            stopService(Intent(this, RecentAppsService::class.java))
            updateServiceStatus(false) // Обновляем статус сервиса как остановлен
        }

        lifecycleScope.launch {
            requestRootAccess()
        }

        checkStoragePermissions()
        checkNotificationPermission()

        startAutoUpdate()

        // Проверяем статус сервиса при запуске приложения
        updateServiceStatus(isServiceRunning(RecentAppsService::class.java))
    }

    private fun startAutoUpdate() {
        lifecycleScope.launch {
            while (true) {
                delay(900)
                viewModel.updateRecentApps()
            }
        }
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in activityManager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun updateServiceStatus(isRunning: Boolean) {
        if (isRunning) {
            serviceStatusTextView.text = "Running"
            serviceStatusTextView.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            serviceStatusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.black))
        } else {
            serviceStatusTextView.text = "Stopped"
            serviceStatusTextView.setBackgroundColor(ContextCompat.getColor(this, android.R.color.black))
            serviceStatusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        }
    }

    private fun vibrate() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(100)
        }
    }

    private fun requestRootAccess() {
        Shell.getShell { shell ->
            if (!shell.isRoot) {
                Log.e("MainActivity", "Root права не получены")
                Toast.makeText(this, "Для работы приложения необходимы root права", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Snackbar.make(findViewById(android.R.id.content), "Для работы приложения необходим доступ ко всем файлам", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Предоставить") {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        startActivity(intent)
                    }
                    .show()
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(findViewById(android.R.id.content), "Для работы приложения необходим доступ к файлам", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Предоставить") {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                            PERMISSION_REQUEST_CODE
                        )
                    }
                    .show()
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST_CODE)
            }
        }
    }
}
