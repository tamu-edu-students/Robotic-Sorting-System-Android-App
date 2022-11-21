package com.example.roboticsortingsystem.bluetooth

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

// The following are extensions to the Context class that make it easier to tell if a permission has been given.
// Checks whether a permission has been granted by the user
fun Context.hasPermission(permissionType: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permissionType) ==
            PackageManager.PERMISSION_GRANTED
}

// Checks correct runtime permissions
fun Context.hasRequiredRuntimePermissions(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Permissions for Android 12+
        hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
                hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
    } else { // Permission for Android 9-11
        hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}