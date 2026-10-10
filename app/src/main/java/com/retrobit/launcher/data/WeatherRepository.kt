package com.retrobit.launcher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

 data class WeatherSnapshot(
    val location: String,
    val temperatureC: Double,
    val humidityPercent: Int?,
    val windKph: Double?,
    val description: String
)

/** Open-Meteo client; city search is used so the launcher does not need a paid API key. */
class WeatherRepository {
    suspend fun currentWeather(city: String): Result<WeatherSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            require(city.isNotBlank()) { "Enter a city name first." }
            val encodedCity = URLEncoder.encode(city.trim(), Charsets.UTF_8.name())
            val geocoding = getJson(
                "https://geocoding-api.open-meteo.com/v1/search?name=$encodedCity&count=1&language=en&format=json"
            )
            val results = geocoding.optJSONArray("results")
                ?: error("Location not found. Check the city name.")
            if (results.length() == 0) error("Location not found. Check the city name.")
            val place = results.getJSONObject(0)
            val latitude = place.getDouble("latitude")
            val longitude = place.getDouble("longitude")
            val displayName = listOfNotNull(
                place.optString("name").takeIf { it.isNotBlank() },
                place.optString("admin1").takeIf { it.isNotBlank() },
                place.optString("country").takeIf { it.isNotBlank() }
            ).distinct().joinToString(", ")

            val forecast = getJson(
                "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m&temperature_unit=celsius&wind_speed_unit=kmh"
            )
            val current = forecast.optJSONObject("current") ?: error("Weather data is unavailable.")
            WeatherSnapshot(
                location = displayName.ifBlank { city.trim() },
                temperatureC = current.optDouble("temperature_2m", Double.NaN),
                humidityPercent = current.optInt("relative_humidity_2m", -1).takeIf { it >= 0 },
                windKph = current.optDouble("wind_speed_10m", Double.NaN).takeIf { it.isFinite() },
                description = describeWeather(current.optInt("weather_code", -1))
            )
        }
    }

    private fun getJson(url: String): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.setRequestProperty("Accept", "application/json")
        return try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) error("Weather service returned HTTP $code.")
            JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun describeWeather(code: Int): String = when (code) {
        0 -> "Clear sky"
        1 -> "Mainly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51, 53, 55 -> "Drizzle"
        56, 57 -> "Freezing drizzle"
        61, 63, 65 -> "Rain"
        66, 67 -> "Freezing rain"
        71, 73, 75, 77 -> "Snow"
        80, 81, 82 -> "Rain showers"
        85, 86 -> "Snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm with hail"
        else -> "Weather unavailable"
    }
}
