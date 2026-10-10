package com.retrobit.launcher.data

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

/** Device lock requires the user to explicitly activate this app as a Device Admin. */
object DeviceLockController {
    private fun receiver(context: Context) =
        ComponentName(context, RetroBitDeviceAdminReceiver::class.java)

    fun isAdminActive(context: Context): Boolean = try {
        val manager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        manager.isAdminActive(receiver(context))
    } catch (_: Exception) {
        false
    }

    fun activationIntent(context: Context): Intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, receiver(context))
        .putExtra(
            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            "RetroBit uses Device Admin only to lock the phone when you use the double-tap gesture. It cannot wipe or manage your device."
        )

    fun removalIntent(context: Context): Intent = Intent(Settings.ACTION_SECURITY_SETTINGS)

    fun lockNow(context: Context): Boolean = try {
        val manager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val component = receiver(context)
        if (!manager.isAdminActive(component)) false else {
            manager.lockNow()
            true
        }
    } catch (_: Exception) {
        false
    }
}

class RetroBitDeviceAdminReceiver : DeviceAdminReceiver()
