package com.example.roboticsortingsystem.bluetooth

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

const val RUNTIME_PERMISSION_REQUEST_CODE = 2

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

// Extends the Activity class to request relevant permissions if needed
fun Activity.requestRelevantRuntimePermissions() {
    if (hasRequiredRuntimePermissions()) { return } // No need to ask for permissions if they're already there
    when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> { // Need location permission for Android 9-11
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                RUNTIME_PERMISSION_REQUEST_CODE
            )
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> { // Android 12+: need BLUETOOTH_SCAN and BLUETOOTH_CONNECT permissions
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT),
                RUNTIME_PERMISSION_REQUEST_CODE
            )
        }
    }
}

// Provides permissions status to UI
object PermissionState {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+ BLE permissions
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    } else { // Android 9-11 BLE permissions
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
}