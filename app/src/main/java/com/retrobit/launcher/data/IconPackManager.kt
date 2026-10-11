package com.retrobit.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import org.xmlpull.v1.XmlPullParser

class IconPackManager(private val context: Context) {
    data class InstalledIconPack(val packageName: String, val label: String)

    private val drawableMaps = mutableMapOf<String, Map<String, String>>()

    fun installedPacks(): List<InstalledIconPack> {
        val packageManager = context.packageManager
        return SUPPORTED_THEME_ACTIONS
            .flatMap { action ->
                runCatching {
                    packageManager.queryIntentActivities(Intent(action), PackageManager.MATCH_ALL)
                }.getOrDefault(emptyList())
            }
            .mapNotNull { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                InstalledIconPack(
                    packageName = activityInfo.packageName,
                    label = resolveInfo.loadLabel(packageManager)?.toString() ?: activityInfo.packageName
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun iconFor(packageName: String, className: String, iconPackPackage: String?): Drawable? {
        if (iconPackPackage.isNullOrBlank()) return null
        return try {
            val resources = context.packageManager.getResourcesForApplication(iconPackPackage)
            val map = drawableMaps.getOrPut(iconPackPackage) { readAppFilter(resources, iconPackPackage) }
            val flattened = ComponentName(packageName, className).flattenToString()
            val shortFlattened = ComponentName(packageName, className).flattenToShortString()
            val drawableName = map[flattened] ?: map[shortFlattened] ?: map["$packageName/$className"] ?: return null
            val drawableId = resources.getIdentifier(drawableName, "drawable", iconPackPackage)
                .takeIf { it != 0 }
                ?: resources.getIdentifier(drawableName, "mipmap", iconPackPackage)
            if (drawableId == 0) null else resources.getDrawable(drawableId, context.theme)
        } catch (_: Exception) {
            null
        }
    }

    private fun readAppFilter(resources: Resources, packageName: String): Map<String, String> {
        val xmlId = resources.getIdentifier("appfilter", "xml", packageName)
        if (xmlId == 0) return emptyMap()
        val parser = resources.getXml(xmlId)
        val icons = mutableMapOf<String, String>()
        try {
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name.equals("item", ignoreCase = true)) {
                    val component = parser.getAttributeValue(null, "component")
                    val drawable = parser.getAttributeValue(null, "drawable")
                    if (!component.isNullOrBlank() && !drawable.isNullOrBlank()) {
                        val normalized = component
                            .removePrefix("ComponentInfo{")
                            .removeSuffix("}")
                        icons[normalized] = drawable
                        val slash = normalized.indexOf('/')
                        if (slash >= 0 && slash + 1 < normalized.length && normalized[slash + 1] == '.') {
                            icons[normalized.substring(0, slash + 1) + normalized.substring(slash + 2)] = drawable
                        }
                    }
                }
                event = parser.next()
            }
        } catch (_: Exception) {
            return icons
        } finally {
            parser.close()
        }
        return icons
    }

    companion object {
        private val SUPPORTED_THEME_ACTIONS = listOf(
            "org.adw.launcher.THEMES",
            "com.anddoes.launcher.THEME",
            "com.teslacoilsw.launcher.THEME",
            "com.novalauncher.THEME",
            "com.gau.go.launcherex.theme",
            "com.dlto.atom.launcher.THEME"
        )
    }
}
