package com.retrobit.launcher.ui.components

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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import com.retrobit.launcher.data.AppModel
import com.retrobit.launcher.data.IconPackManager
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
    hasCustomIcon: Boolean,
    onChooseImage: () -> Unit,
    onSelectIconPack: (String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalRetroBitColors.current
    val context = LocalContext.current
    val iconPackManager = remember(context) { IconPackManager(context) }
    var packOptions by remember(app.packageName, app.componentName) { mutableStateOf<List<AppIconPackOption>>(emptyList()) }
    var loadingPacks by remember(app.packageName, app.componentName) { mutableStateOf(true) }

    LaunchedEffect(app.packageName, app.componentName) {
        loadingPacks = true
        packOptions = withContext(Dispatchers.IO) {
            runCatching {
                iconPackManager.installedPacks().map { pack ->
                    AppIconPackOption(
                        packageName = pack.packageName,
                        label = pack.label,
                        icon = iconPackManager.iconFor(app.packageName, app.componentName, pack.packageName)
                    )
                }
            }.getOrDefault(emptyList())
        }
        loadingPacks = false
    }

    val currentBitmap = remember(app.icon) { app.icon?.let { drawableToGrayscaleBitmap(it, 72, 72) } }

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
                Text("[ESC]", color = colors.inkPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onDismiss).padding(5.dp))
            }
            Spacer(Modifier.height(7.dp))
            Text(
                "This icon override is shared by the app drawer and Home shortcut.",
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
                    Text(
                        activeIconPackPackage ?: if (hasCustomIcon) "CUSTOM IMAGE" else "GLOBAL / SYSTEM DEFAULT",
                        color = colors.inkPrimary,
                        fontSize = 8.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            CyberActionButton(label = "CHOOSE IMAGE FROM DEVICE", onClick = onChooseImage)
            Spacer(Modifier.height(10.dp))
            Text("INSTALLED ICON PACKS // APP-SPECIFIC MATCHES", color = colors.inkSecondary, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(4.dp))

            when {
                loadingPacks -> Text("Scanning installed compatible icon packs…", color = colors.inkMuted, fontSize = 8.sp)
                packOptions.isEmpty() -> Text("No compatible installed icon packs were found.", color = colors.inkMuted, fontSize = 8.sp)
                else -> packOptions.forEach { option ->
                    AppIconPackRow(
                        option = option,
                        selected = activeIconPackPackage == option.packageName,
                        onSelect = { if (option.icon != null) onSelectIconPack(option.packageName) }
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }

            if (hasCustomIcon) {
                Spacer(Modifier.height(4.dp))
                CyberActionButton(label = "RESET TO GLOBAL / SYSTEM ICON", onClick = onReset)
            }
        }
    }
}

@Composable
private fun AppIconPackRow(
    option: AppIconPackOption,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val colors = LocalRetroBitColors.current
    val bitmap = remember(option.icon) { option.icon?.let { drawableToGrayscaleBitmap(it, 72, 72) } }
    val available = option.icon != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) colors.surface else colors.background)
            .border(1.dp, colors.border)
            .then(if (available) Modifier.clickable(onClick = onSelect) else Modifier)
            .padding(7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (bitmap != null) {
            Image(bitmap.asImageBitmap(), contentDescription = "${option.label} icon", modifier = Modifier.size(34.dp))
        } else {
            Box(Modifier.size(34.dp).border(1.dp, colors.border), contentAlignment = Alignment.Center) {
                Text("—", color = colors.inkMuted, fontSize = 10.sp)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                (if (selected) "● " else "") + option.label,
                color = if (available) colors.inkPrimary else colors.inkMuted,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (available) "Tap to use this pack's icon for this app" else "No icon mapping for this app",
                color = colors.inkMuted,
                fontSize = 7.sp
            )
        }
    }
}
