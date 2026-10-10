package com.retrobit.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.retrobit.launcher.data.LauncherFeatureStore
import com.retrobit.launcher.ui.theme.LocalRetroBitColors

@Composable
fun WeatherCityDialog(store: LauncherFeatureStore, onDismiss: () -> Unit) {
    val colors = LocalRetroBitColors.current
    var city by remember { mutableStateOf(store.weatherCity) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().background(colors.background).border(2.dp, colors.border).padding(16.dp)
        ) {
            Text("WEATHER LOCATION", color = colors.inkPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            BasicTextField(
                value = city,
                onValueChange = { city = it },
                singleLine = true,
                textStyle = TextStyle(color = colors.inkPrimary, fontSize = 12.sp),
                cursorBrush = SolidColor(colors.inkPrimary),
                modifier = Modifier.fillMaxWidth().background(colors.surface).border(1.dp, colors.border).padding(10.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text("Enter a city name. Weather is provided by Open-Meteo; no API key required.", color = colors.inkSecondary, fontSize = 9.sp)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "CANCEL",
                    color = colors.inkSecondary,
                    modifier = Modifier.clickable(onClick = onDismiss).padding(8.dp)
                )
                Text(
                    "SAVE LOCATION",
                    color = colors.inkPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        if (city.isNotBlank()) store.weatherCity = city.trim()
                        onDismiss()
                    }.padding(8.dp)
                )
            }
        }
    }
}
