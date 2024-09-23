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
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 1
    private lateinit var viewModel: MainViewModel
    private lateinit var recentAppsTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recentAppsTextView = findViewById(R.id.recentAppsTextView)

        viewModel = ViewModelProvider(this, MainViewModelFactory(RecentAppsRepository(this)))[MainViewModel::class.java]

        // Наблюдение за recentApps и обновление TextView
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
            }
            viewModel.updateRecentApps() // Принудительное обновление данных при каждом нажатии
        }

        stopServiceButton.setOnClickListener {
            vibrate()
            stopService(Intent(this, RecentAppsService::class.java))
        }

        lifecycleScope.launch {
            requestRootAccess()
        }

        checkStoragePermissions()

        // Автоматическое выполнение каждые 3 секунды
        startAutoUpdate()
    }

    private fun startAutoUpdate() {
        lifecycleScope.launch {
            while (true) {
                delay(50)  // Задержка 3 секунды
                viewModel.updateRecentApps()  // Обновление данных
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
            if (shell.isRoot) {
                copyFilesFromAssetsWithRoot()
            } else {
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
            } else {
                setupAppFolders()
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
            } else {
                setupAppFolders()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupAppFolders()
            } else {
                Snackbar.make(findViewById(android.R.id.content), "Для работы приложения необходим доступ к файлам", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Предоставить") {
                        checkStoragePermissions()
                    }
                    .show()
            }
        }
    }

    private fun copyFilesFromAssetsWithRoot() {
        val dbFolder = "/storage/emulated/0/.recentappppn1/.db/"
        val shFolder = "/storage/emulated/0/.recentappppn1/.sh/"

        createFolderWithRoot(dbFolder)
        createFolderWithRoot(shFolder)

        copyAssetFileWithRoot("db/main.db", "$dbFolder/main.db")

        val shFiles = assets.list("sh") ?: return
        for (shFileName in shFiles) {
            copyAssetFileWithRoot("sh/$shFileName", "$shFolder/$shFileName")
        }
    }

    private fun copyAssetFileWithRoot(assetPath: String, outFilePath: String) {
        try {
            val outFile = File(outFilePath)
            if (!outFile.exists()) {
                val inputStream = assets.open(assetPath)
                val tempFile = File.createTempFile("temp_", null, cacheDir)
                val outputStream = tempFile.outputStream()

                inputStream.copyTo(outputStream)

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                Shell.cmd("cp ${tempFile.absolutePath} $outFilePath").exec()
            }
        } catch (e: IOException) {
            Log.e("MainActivity", "Ошибка при копировании файла: ${e.message}")
            Toast.makeText(this, "Ошибка при копировании файла", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createFolderWithRoot(path: String) {
        Shell.cmd("mkdir -p $path").exec()
    }

    private fun setupAppFolders() {
        createFolderWithRoot("/storage/emulated/0/.recentappppn1/.db/")
        createFolderWithRoot("/storage/emulated/0/.recentappppn1/.sh/")
    }
}