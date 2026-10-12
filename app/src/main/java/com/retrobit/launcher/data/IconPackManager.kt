package com.retrobit.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import org.xmlpull.v1.XmlPullParser
import java.util.zip.ZipFile

class IconPackManager(private val context: Context) {
    data class InstalledIconPack(val packageName: String, val label: String)
    data class PackIconResource(val resourceType: String, val resourceName: String)

    private val drawableMaps = mutableMapOf<String, Map<String, String>>()
    private val iconResourcesCache = mutableMapOf<String, List<PackIconResource>>()

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

    fun packApplicationIcon(iconPackPackage: String): Drawable? = runCatching {
        context.packageManager.getApplicationIcon(iconPackPackage)
    }.getOrNull()

    fun iconFor(packageName: String, className: String, iconPackPackage: String?): Drawable? {
        if (iconPackPackage.isNullOrBlank()) return null
        return try {
            val resources = context.packageManager.getResourcesForApplication(iconPackPackage)
            val map = synchronized(drawableMaps) {
                drawableMaps.getOrPut(iconPackPackage) { readAppFilter(resources, iconPackPackage) }
            }
            val flattened = ComponentName(packageName, className).flattenToString()
            val shortFlattened = ComponentName(packageName, className).flattenToShortString()
            val drawableName = map[flattened] ?: map[shortFlattened] ?: map["$packageName/$className"] ?: return null
            drawableFor(resources, iconPackPackage, "drawable", drawableName)
                ?: drawableFor(resources, iconPackPackage, "mipmap", drawableName)
        } catch (_: Exception) {
            null
        }
    }

    /** Lists icon drawable resources even when the pack's appfilter does not map them to this app. */
    fun iconResources(iconPackPackage: String): List<PackIconResource> {
        if (iconPackPackage.isBlank()) return emptyList()
        synchronized(iconResourcesCache) {
            iconResourcesCache[iconPackPackage]?.let { return it }
        }

        val discovered = linkedSetOf<PackIconResource>()
        try {
            val packageManager = context.packageManager
            val resources = packageManager.getResourcesForApplication(iconPackPackage)
            val appFilter = synchronized(drawableMaps) {
                drawableMaps.getOrPut(iconPackPackage) { readAppFilter(resources, iconPackPackage) }
            }
            appFilter.values.forEach { name ->
                addIfResourceExists(resources, iconPackPackage, "drawable", name, discovered)
                addIfResourceExists(resources, iconPackPackage, "mipmap", name, discovered)
            }

            val appInfo = packageManager.getApplicationInfo(iconPackPackage, 0)
            val apkPaths = mutableListOf<String>()
            appInfo.sourceDir?.takeIf { it.isNotBlank() }?.let { apkPaths.add(it) }
            appInfo.splitSourceDirs?.forEach { path -> if (path.isNotBlank()) apkPaths.add(path) }
            apkPaths.distinct().forEach { apkPath ->
                runCatching {
                    ZipFile(apkPath).use { archive ->
                        val entries = archive.entries()
                        while (entries.hasMoreElements()) {
                            val entry = entries.nextElement()
                            val resource = resourceFromArchivePath(entry.name) ?: continue
                            addIfResourceExists(
                                resources,
                                iconPackPackage,
                                resource.resourceType,
                                resource.resourceName,
                                discovered
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // appfilter-only candidates gathered above are still returned when archive inspection fails.
        }

        val result = discovered.sortedWith(compareBy<PackIconResource> { it.resourceName.lowercase() }.thenBy { it.resourceType })
        synchronized(iconResourcesCache) { iconResourcesCache[iconPackPackage] = result }
        return result
    }

    fun drawableForResource(iconPackPackage: String, resourceType: String, resourceName: String): Drawable? {
        if (iconPackPackage.isBlank() || resourceName.isBlank()) return null
        return try {
            val resources = context.packageManager.getResourcesForApplication(iconPackPackage)
            drawableFor(resources, iconPackPackage, resourceType, resourceName)
        } catch (_: Exception) {
            null
        }
    }

    private fun addIfResourceExists(
        resources: Resources,
        packageName: String,
        type: String,
        name: String,
        destination: MutableSet<PackIconResource>
    ) {
        if (name.isBlank()) return
        if (resources.getIdentifier(name, type, packageName) != 0) {
            destination += PackIconResource(type, name)
        }
    }

    private fun resourceFromArchivePath(path: String): PackIconResource? {
        if (!path.startsWith("res/")) return null
        val slash = path.indexOf('/', startIndex = 4)
        if (slash < 0 || slash == path.lastIndex) return null
        val directory = path.substring(4, slash).substringBefore('-')
        if (directory != "drawable" && directory != "mipmap") return null
        val fileName = path.substringAfterLast('/')
        val nameWithNinePatch = fileName.substringBeforeLast('.', fileName)
        val name = nameWithNinePatch.removeSuffix(".9")
        if (name.isBlank()) return null
        return PackIconResource(directory, name)
    }

    private fun drawableFor(resources: Resources, packageName: String, type: String, name: String): Drawable? {
        val id = resources.getIdentifier(name, type, packageName)
        return if (id == 0) null else runCatching { resources.getDrawable(id, context.theme) }.getOrNull()
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
