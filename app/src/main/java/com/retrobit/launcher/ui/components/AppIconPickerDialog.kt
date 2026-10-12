package com.retrobit.launcher.ui.components

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.retrobit.launcher.data.AppModel
import com.retrobit.launcher.data.IconPackManager
import com.retrobit.launcher.data.IconPackManager.PackIconResource
import com.retrobit.launcher.ui.theme.LocalRetroBitColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class AppIconPackOption(
    val packageName: String,
    val label: String,
    val icon: Drawable?
)

@Composable
fun AppIconPickerDialog(
    app: AppModel,
    activeIconPackPackage: String?,
    activeIconResourceType: String?,
    activeIconResourceName: String?,
    hasCustomIcon: Boolean,
    onChooseImage: () -> Unit,
    onSelectIconPackIcon: (packPackage: String, resourceType: String, resourceName: String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalRetroBitColors.current
    val context = LocalContext.current
    val iconPackManager = remember(context) { IconPackManager(context) }
    var packOptions by remember(app.packageName, app.componentName) {
        mutableStateOf<List<AppIconPackOption>>(emptyList())
    }
    var loadingPacks by remember(app.packageName, app.componentName) { mutableStateOf(true) }
    var selectedPackPackage by remember(app.packageName, app.componentName) { mutableStateOf<String?>(null) }
    var packIcons by remember(selectedPackPackage) { mutableStateOf<List<PackIconResource>>(emptyList()) }
    var loadingIcons by remember(selectedPackPackage) { mutableStateOf(false) }
    var searchQuery by remember(selectedPackPackage) { mutableStateOf("") }

    LaunchedEffect(app.packageName, app.componentName) {
        loadingPacks = true
        packOptions = withContext(Dispatchers.IO) {
            runCatching {
                iconPackManager.installedPacks().map { pack ->
                    AppIconPackOption(
                        packageName = pack.packageName,
                        label = pack.label,
                        icon = iconPackManager.packApplicationIcon(pack.packageName)
                    )
                }
            }.getOrDefault(emptyList())
        }
        loadingPacks = false
    }

    LaunchedEffect(selectedPackPackage) {
        val packPackage = selectedPackPackage ?: return@LaunchedEffect
        loadingIcons = true
        packIcons = withContext(Dispatchers.IO) {
            runCatching { iconPackManager.iconResources(packPackage) }.getOrDefault(emptyList())
        }
        loadingIcons = false
    }

    val currentBitmap = remember(app.icon) { app.icon?.let { drawableToGrayscaleBitmap(it, 72, 72) } }
    val currentIconDescription = when {
        activeIconPackPackage != null && activeIconResourceName != null ->
            "$activeIconPackPackage / $activeIconResourceName"
        activeIconPackPackage != null -> activeIconPackPackage
        hasCustomIcon -> "CUSTOM IMAGE"
        else -> "GLOBAL / SYSTEM DEFAULT"
    }
    val selectedPack = packOptions.firstOrNull { it.packageName == selectedPackPackage }
    val visiblePackIcons = packIcons.filter { it.resourceName.contains(searchQuery.trim(), ignoreCase = true) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState())
                .background(colors.background)
                .border(2.dp, colors.border)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("CUSTOM APP ICON", color = colors.inkPrimary, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    Text(app.displayLabel, color = colors.inkSecondary, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (selectedPack != null) {
                    Text(
                        "[‹ PACKS]",
                        color = colors.inkPrimary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { selectedPackPackage = null }.padding(5.dp)
                    )
                } else {
                    Text("[ESC]", color = colors.inkPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onDismiss).padding(5.dp))
                }
            }
            Spacer(Modifier.height(7.dp))
            Text(
                "Choose an image or any icon asset from an installed icon pack. This override applies only to ${app.displayLabel}.",
                color = colors.inkMuted,
                fontSize = 8.sp
            )
            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth().background(colors.surface).border(1.dp, colors.border).padding(7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (currentBitmap != null) {
                    Image(currentBitmap.asImageBitmap(), contentDescription = "Current ${app.displayLabel} icon", modifier = Modifier.size(40.dp))
                } else {
                    Box(Modifier.size(40.dp).border(1.dp, colors.border), contentAlignment = Alignment.Center) {
                        Text(app.displayLabel.take(2).uppercase(), color = colors.inkPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text("CURRENT ICON", color = colors.inkSecondary, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    Text(currentIconDescription, color = colors.inkPrimary, fontSize = 8.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }

            Spacer(Modifier.height(8.dp))
            CyberActionButton(label = "CHOOSE IMAGE FROM DEVICE", onClick = onChooseImage)
            Spacer(Modifier.height(10.dp))

            if (selectedPack == null) {
                Text("INSTALLED ICON PACKS // BROWSE ANY ICON", color = colors.inkSecondary, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(4.dp))
                when {
                    loadingPacks -> Text("Scanning installed compatible icon packs…", color = colors.inkMuted, fontSize = 8.sp)
                    packOptions.isEmpty() -> Text("No compatible installed icon packs were found.", color = colors.inkMuted, fontSize = 8.sp)
                    else -> packOptions.forEach { option ->
                        AppIconPackRow(
                            option = option,
                            selected = activeIconPackPackage == option.packageName,
                            onClick = { selectedPackPackage = option.packageName }
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
            } else {
                Text("${selectedPack.label.uppercase()} // CHOOSE ANY ICON", color = colors.inkSecondary, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(5.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    textStyle = TextStyle(color = colors.inkPrimary, fontSize = 10.sp),
                    cursorBrush = SolidColor(colors.inkPrimary),
                    modifier = Modifier.fillMaxWidth().background(colors.surface).border(1.dp, colors.border).padding(9.dp),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isBlank()) Text("Search icon names…", color = colors.inkMuted, fontSize = 9.sp)
                            inner()
                        }
                    }
                )
                Spacer(Modifier.height(6.dp))
                when {
                    loadingIcons -> Text("Loading icons from ${selectedPack.label}…", color = colors.inkMuted, fontSize = 8.sp)
                    packIcons.isEmpty() -> Text("No drawable icon resources were found in this pack.", color = colors.inkMuted, fontSize = 8.sp)
                    visiblePackIcons.isEmpty() -> Text("No icon names match that search.", color = colors.inkMuted, fontSize = 8.sp)
                    else -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 66.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        items(
                            items = visiblePackIcons,
                            key = { "${it.resourceType}/${it.resourceName}" }
                        ) { resource ->
                            AppPackIconTile(
                                iconPackPackage = selectedPack.packageName,
                                resource = resource,
                                iconPackManager = iconPackManager,
                                selected = activeIconPackPackage == selectedPack.packageName &&
                                    activeIconResourceType == resource.resourceType &&
                                    activeIconResourceName == resource.resourceName,
                                onClick = {
                                    onSelectIconPackIcon(
                                        selectedPack.packageName,
                                        resource.resourceType,
                                        resource.resourceName
                                    )
                                }
                            )
                        }
                    }
                }
            }

            if (hasCustomIcon) {
                Spacer(Modifier.height(8.dp))
                CyberActionButton(label = "RESET TO GLOBAL / SYSTEM ICON", onClick = onReset)
            }
        }
    }
}

@Composable
private fun AppIconPackRow(
    option: AppIconPackOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalRetroBitColors.current
    val bitmap = remember(option.icon) { option.icon?.let { drawableToGrayscaleBitmap(it, 72, 72) } }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) colors.surface else colors.background)
            .border(1.dp, colors.border)
            .clickable(onClick = onClick)
            .padding(7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (bitmap != null) {
            Image(bitmap.asImageBitmap(), contentDescription = "${option.label} icon pack", modifier = Modifier.size(34.dp))
        } else {
            Box(Modifier.size(34.dp).border(1.dp, colors.border), contentAlignment = Alignment.Center) {
                Text("—", color = colors.inkMuted, fontSize = 10.sp)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                (if (selected) "● " else "") + option.label,
                color = colors.inkPrimary,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text("Browse all drawable icons in this pack", color = colors.inkMuted, fontSize = 7.sp)
        }
    }
}

@Composable
private fun AppPackIconTile(
    iconPackPackage: String,
    resource: PackIconResource,
    iconPackManager: IconPackManager,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalRetroBitColors.current
    var bitmap by remember(iconPackPackage, resource) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(iconPackPackage, resource.resourceType, resource.resourceName) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                iconPackManager.drawableForResource(iconPackPackage, resource.resourceType, resource.resourceName)
                    ?.let { drawableToGrayscaleBitmap(it, 96, 96) }
            }.getOrNull()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
            .background(if (selected) colors.surface else colors.background)
            .border(1.dp, colors.border)
            .clickable(onClick = onClick)
            .padding(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (bitmap != null) {
            Image(bitmap!!.asImageBitmap(), contentDescription = resource.resourceName, modifier = Modifier.size(40.dp))
        } else {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Text("…", color = colors.inkMuted, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            resource.resourceName,
            color = colors.inkPrimary,
            fontSize = 7.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
