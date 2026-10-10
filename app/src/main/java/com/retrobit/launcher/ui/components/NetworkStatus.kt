package com.retrobit.launcher.ui.components

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberNetworkStatus(): String {
    val context = LocalContext.current
    var status by remember(context) { mutableStateOf(readNetworkStatus(context)) }

    DisposableEffect(context) {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                status = readNetworkStatus(context)
            }

            override fun onLost(network: Network) {
                status = readNetworkStatus(context)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                status = readNetworkStatus(context)
            }
        }
        try {
            manager.registerDefaultNetworkCallback(callback)
            status = readNetworkStatus(context)
        } catch (_: Exception) {
            status = "NETWORK STATUS UNAVAILABLE"
        }
        onDispose {
            try {
                manager.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
                // The callback may not have been registered on older or restricted devices.
            }
        }
    }
    return status
}

@Suppress("DEPRECATION")
private fun readNetworkStatus(context: Context): String {
    return try {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: return "OFFLINE"
        val capabilities = manager.getNetworkCapabilities(network) ?: return "OFFLINE"
        when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val rssi = wifi?.connectionInfo?.rssi ?: NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED
                if (rssi in -100..0) "WIFI // ${rssi}dBm" else "WIFI // CONNECTED"
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                val strength = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) capabilities.signalStrength else NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED
                if (strength != NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED) "CELLULAR // ${strength}dBm" else "CELLULAR // CONNECTED"
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET // CONNECTED"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN // CONNECTED"
            else -> "NETWORK // CONNECTED"
        }
    } catch (_: SecurityException) {
        "NETWORK // CONNECTED"
    } catch (_: Exception) {
        "NETWORK STATUS UNAVAILABLE"
    }
}
