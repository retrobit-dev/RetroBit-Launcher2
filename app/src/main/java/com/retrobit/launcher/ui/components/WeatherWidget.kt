package com.retrobit.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrobit.launcher.data.LauncherFeatureStore
import com.retrobit.launcher.data.WeatherRepository
import com.retrobit.launcher.data.WeatherSnapshot
import com.retrobit.launcher.ui.theme.LocalRetroBitColors

@Composable
fun WeatherWidget(
    store: LauncherFeatureStore,
    onConfigure: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalRetroBitColors.current
    val repository = remember { WeatherRepository() }
    var city by remember(store.weatherCity) { mutableStateOf(store.weatherCity) }
    var snapshot by remember { mutableStateOf<WeatherSnapshot?>(null) }
    var message by remember { mutableStateOf("LOADING WEATHER…") }
    var refreshToken by remember { mutableIntStateOf(0) }

    LaunchedEffect(city, refreshToken) {
        message = "LOADING WEATHER…"
        val result = repository.currentWeather(city)
        result.fold(
            onSuccess = {
                snapshot = it
                message = ""
            },
            onFailure = {
                snapshot = null
                message = it.message ?: "WEATHER UNAVAILABLE"
            }
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 2.dp)
            .background(colors.surface)
            .border(1.dp, colors.border)
            .clickable(onClick = onConfigure)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "WEATHER // ${snapshot?.location ?: city.uppercase()}",
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            color = colors.inkSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = snapshot?.let {
                "${it.temperatureC.toInt()}°C  ${it.description.uppercase()}"
            } ?: message,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            color = colors.inkPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            "↻ REFRESH",
            color = colors.inkSecondary,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { refreshToken++ }.padding(start = 6.dp)
        )
    }
}
