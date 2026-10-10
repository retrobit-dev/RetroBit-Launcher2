package com.retrobit.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.retrobit.launcher.data.AppRepository
import com.retrobit.launcher.ui.MainLauncherScreen
import com.retrobit.launcher.ui.theme.RetroBitLauncherTheme
import com.retrobit.launcher.ui.theme.RetroBitThemeMode

class MainActivity : ComponentActivity() {

    private lateinit var appRepository: AppRepository
    private var packageReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appRepository = AppRepository(this)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Di launcher, tombol back mengembalikan list ke atas / tidak keluar
            }
        })

        setContent {
            var currentTheme by remember { mutableStateOf(RetroBitThemeMode.LIGHT_EINK) }

            RetroBitLauncherTheme(themeMode = currentTheme) {
                MainLauncherScreen(
                    repository = appRepository,
                    currentTheme = currentTheme,
                    onThemeChange = { currentTheme = it }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        packageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                // Deteksi otomatis saat user pasang atau hapus aplikasi
            }
        }
        registerReceiver(packageReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        packageReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {}
        }
    }
}
