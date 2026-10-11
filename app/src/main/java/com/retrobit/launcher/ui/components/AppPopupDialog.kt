package com.retrobit.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.retrobit.launcher.data.AppModel
import com.retrobit.launcher.ui.theme.LocalRetroBitColors

@Composable
fun AppPopupDialog(
    app: AppModel,
    onDismiss: () -> Unit,
    onLaunch: () -> Unit,
    isPinnedToHome: Boolean,
    onToggleHomeShortcut: () -> Unit,
    onRename: () -> Unit,
    onChangeIcon: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleHide: () -> Unit,
    onToggleDetox: () -> Unit,
    onOpenDetails: () -> Unit,
    onUninstall: () -> Unit
) {
    val colors = LocalRetroBitColors.current

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.background)
                .border(2.dp, colors.border)
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "[NODE] ${app.displayLabel.uppercase()}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.inkPrimary
                )
                Text(
                    text = "[ESC]",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.inkPrimary,
                    modifier = Modifier.clickable { onDismiss() }
                )
            }

            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .border(1.dp, colors.border)
                    .padding(8.dp)
            ) {
                Text(text = "PKG: ${app.packageName}", fontSize = 8.sp, color = colors.inkSecondary)
                Text(text = "ALIAS: ${app.customLabel ?: "NONE (DEFAULT)"}", fontSize = 8.sp, color = colors.inkSecondary)
                Text(text = "CATEGORY: #${app.categoryTag} // USAGE: ${app.screentimeText}", fontSize = 8.sp, color = colors.inkSecondary)
                Text(
                    text = "STATUS: ${if (app.isHidden) "HIDDEN (VAULT)" else "VISIBLE"} // DETOX: ${if (app.isDetoxRestricted) "PAUSE ENABLED" else "OFF"}",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.inkPrimary
                )
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = "// DECK ACTIONS",
                fontSize = 8.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.inkSecondary
            )
            Spacer(Modifier.height(4.dp))

            CyberActionButton(label = "▶ EXECUTE APPLICATION", onClick = onLaunch)
            Spacer(Modifier.height(4.dp))
            CyberActionButton(
                label = if (isPinnedToHome) "▣ REMOVE FROM HOME SCREEN" else "▣ ADD TO HOME SCREEN",
                onClick = onToggleHomeShortcut
            )
            Spacer(Modifier.height(4.dp))
            CyberActionButton(label = "✎ RENAME / CUSTOM ALIAS", onClick = onRename)
            Spacer(Modifier.height(4.dp))
            CyberActionButton(label = "◈ CHANGE APP ICON // IMAGE OR PACK", onClick = onChangeIcon)
            Spacer(Modifier.height(4.dp))
            CyberActionButton(
                label = if (app.isDetoxRestricted) "⏳ REMOVE MINDFUL PAUSE" else "⏳ ADD 5s MINDFUL PAUSE (DETOX)",
                onClick = onToggleDetox
            )
            Spacer(Modifier.height(4.dp))
            CyberActionButton(
                label = if (app.isHidden) "👁 UNHIDE APPLICATION" else "👁 HIDE APPLICATION (DETOX)",
                onClick = onToggleHide
            )
            Spacer(Modifier.height(4.dp))
            CyberActionButton(
                label = if (app.isFavorite) "★ REMOVE FROM FAVORITES" else "★ PIN TO FAVORITES",
                onClick = onToggleFavorite
            )
            Spacer(Modifier.height(4.dp))
            CyberActionButton(label = "⚙ SYSTEM APP INFO", onClick = onOpenDetails)
            Spacer(Modifier.height(4.dp))
            CyberActionButton(label = "☒ UNINSTALL PACKAGE", onClick = onUninstall)
        }
    }
}

@Composable
fun CyberActionButton(
    label: String,
    onClick: () -> Unit
) {
    val colors = LocalRetroBitColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.border)
            .background(colors.surface)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = colors.inkPrimary
        )
    }
}
