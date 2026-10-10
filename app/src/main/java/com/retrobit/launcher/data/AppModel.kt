package com.retrobit.launcher.data

import android.graphics.drawable.Drawable

/**
 * Representasi data aplikasi pada RetroBit Launcher.
 */
data class AppModel(
    val packageName: String,
    val componentName: String,
    val originalLabel: String,
    val customLabel: String? = null,
    val icon: Drawable? = null,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val isDetoxRestricted: Boolean = false,
    val categoryTag: String = "NODE",
    val screentimeText: String = "5m",
    val notificationPreview: String? = null,
    val subtext: String = ""
) {
    val displayLabel: String
        get() = customLabel ?: originalLabel
}
