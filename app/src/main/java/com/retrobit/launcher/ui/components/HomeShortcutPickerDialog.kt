package com.retrobit.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.retrobit.launcher.data.AppModel
import com.retrobit.launcher.ui.FuzzySearch
import com.retrobit.launcher.ui.theme.LocalRetroBitColors

@Composable
fun HomeShortcutPickerDialog(
    apps: List<AppModel>,
    pinnedPackages: Set<String>,
    onSelect: (AppModel) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalRetroBitColors.current
    var query by remember { mutableStateOf("") }
    val visibleApps = remember(apps, query) {
        val eligible = apps.filterNot { it.isHidden }
        if (query.isBlank()) eligible
        else eligible.mapNotNull { app ->
            val score = FuzzySearch.score(query, app.displayLabel)
                ?: FuzzySearch.score(query, app.packageName)
            score?.let { app to it }
        }.sortedByDescending { it.second }.map { it.first }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.78f)
                .background(colors.background).border(2.dp, colors.border).padding(12.dp)
        ) {
            Text("ADD HOME SHORTCUT", color = colors.inkPrimary, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(color = colors.inkPrimary, fontSize = 10.sp),
                cursorBrush = SolidColor(colors.inkPrimary),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp)
                    .background(colors.surface).border(1.dp, colors.border).padding(8.dp),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text("Search installed apps…", color = colors.inkMuted, fontSize = 9.sp)
                    inner()
                }
            )
            if (visibleApps.isEmpty()) {
                Text("No matching apps found.", color = colors.inkMuted, fontSize = 9.sp, modifier = Modifier.padding(8.dp))
            } else {
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    items(visibleApps, key = { it.packageName }) { app ->
                        val pinned = app.packageName in pinnedPackages
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                .background(colors.surface).border(1.dp, colors.border)
                                .clickable(enabled = !pinned) { onSelect(app) }
                                .padding(horizontal = 8.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(app.displayLabel, color = colors.inkPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(app.packageName, color = colors.inkMuted, fontSize = 7.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(if (pinned) "PINNED" else "+ ADD", color = if (pinned) colors.inkMuted else colors.inkPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Text("CLOSE", color = colors.inkSecondary, fontSize = 8.sp, modifier = Modifier.align(Alignment.End).clickable(onClick = onDismiss).padding(8.dp))
        }
    }
}
