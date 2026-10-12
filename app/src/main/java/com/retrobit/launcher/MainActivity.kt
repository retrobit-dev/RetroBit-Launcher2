package com.retrobit.launcher

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.retrobit.launcher.data.AppRepository
import com.retrobit.launcher.data.LauncherFeatureStore
import com.retrobit.launcher.ui.MainLauncherScreen
import com.retrobit.launcher.ui.theme.PaletteCodec
import com.retrobit.launcher.ui.theme.RetroBitLauncherTheme
import com.retrobit.launcher.ui.theme.RetroBitThemeMode

class MainActivity : FragmentActivity() {

    private lateinit var appRepository: AppRepository

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
            val featureStore = remember { LauncherFeatureStore(applicationContext) }
            val initialTheme = remember {
                runCatching { RetroBitThemeMode.valueOf(featureStore.themeModeName) }
                    .getOrDefault(RetroBitThemeMode.LIGHT_EINK)
            }
            var currentTheme by remember { mutableStateOf(initialTheme) }
            var launcherTextScale by remember { mutableFloatStateOf(featureStore.launcherTextScale) }
            var customColors by remember {
                mutableStateOf(featureStore.customPaletteJson?.let { PaletteCodec.parse(it) })
            }

            RetroBitLauncherTheme(themeMode = currentTheme, customColors = customColors) {
                val systemDensity = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(systemDensity.density, systemDensity.fontScale * launcherTextScale)
                ) {
                    MainLauncherScreen(
                        repository = appRepository,
                        currentTheme = currentTheme,
                        launcherTextScale = launcherTextScale,
                        onLauncherTextScaleChange = {
                            launcherTextScale = it
                            featureStore.launcherTextScale = it
                        },
                        onThemeChange = {
                            currentTheme = it
                            featureStore.themeModeName = it.name
                            featureStore.customPaletteJson = null
                            customColors = null
                        },
                        onCustomPaletteChange = { customColors = it }
                    )
                }
            }
        }
    }
}
