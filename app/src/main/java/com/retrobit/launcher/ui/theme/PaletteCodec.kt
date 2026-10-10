package com.retrobit.launcher.ui.theme

import android.graphics.Color as AndroidColor
import org.json.JSONObject

/** JSON palette schema: background, surface, subtle, inkPrimary, inkSecondary, inkMuted, border. */
object PaletteCodec {
    fun parse(json: String): RetroBitColors? = runCatching {
        val value = JSONObject(json)
        RetroBitColors(
            background = value.color("background"),
            surface = value.color("surface"),
            subtle = value.color("subtle"),
            inkPrimary = value.color("inkPrimary"),
            inkSecondary = value.color("inkSecondary"),
            inkMuted = value.color("inkMuted"),
            border = value.color("border")
        )
    }.getOrNull()

    private fun JSONObject.color(key: String): androidx.compose.ui.graphics.Color {
        val raw = getString(key)
        val argb = AndroidColor.parseColor(raw)
        return androidx.compose.ui.graphics.Color(
            red = AndroidColor.red(argb) / 255f,
            green = AndroidColor.green(argb) / 255f,
            blue = AndroidColor.blue(argb) / 255f,
            alpha = AndroidColor.alpha(argb) / 255f
        )
    }
}
