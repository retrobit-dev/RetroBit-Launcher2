package com.retrobit.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.retrobit.launcher.data.LauncherFeatureStore
import com.retrobit.launcher.ui.theme.LocalRetroBitColors

@Composable
fun TodoDialog(store: LauncherFeatureStore, onDismiss: () -> Unit) {
    val colors = LocalRetroBitColors.current
    var items by remember { mutableStateOf(store.getTodoItems()) }
    var draft by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.78f)
                .background(colors.background).border(2.dp, colors.border).padding(14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("TODO // TASK QUEUE", color = colors.inkPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("[X]", color = colors.inkPrimary, modifier = Modifier.clickable(onClick = onDismiss).padding(4.dp))
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    textStyle = TextStyle(color = colors.inkPrimary, fontSize = 11.sp),
                    cursorBrush = SolidColor(colors.inkPrimary),
                    modifier = Modifier.weight(1f).background(colors.surface).border(1.dp, colors.border).padding(9.dp),
                    decorationBox = { inner ->
                        if (draft.isEmpty()) Text("New task…", color = colors.inkMuted, fontSize = 10.sp)
                        inner()
                    }
                )
                Text(
                    "+ ADD",
                    color = colors.inkPrimary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        store.addTodo(draft)
                        items = store.getTodoItems()
                        draft = ""
                    }.padding(10.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                if (items.isEmpty()) {
                    Text("No tasks yet. Add one above.", color = colors.inkMuted, fontSize = 10.sp)
                }
                items.forEach { item ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp).background(colors.surface).border(1.dp, colors.border).padding(horizontal = 8.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (item.done) "[✓]" else "[ ]",
                            color = colors.inkPrimary,
                            fontSize = 10.sp,
                            modifier = Modifier.clickable {
                                store.toggleTodo(item.id)
                                items = store.getTodoItems()
                            }.padding(end = 8.dp)
                        )
                        Text(
                            item.title,
                            color = if (item.done) colors.inkMuted else colors.inkPrimary,
                            fontSize = 10.sp,
                            textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "[×]",
                            color = colors.inkSecondary,
                            fontSize = 10.sp,
                            modifier = Modifier.clickable {
                                store.removeTodo(item.id)
                                items = store.getTodoItems()
                            }.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
