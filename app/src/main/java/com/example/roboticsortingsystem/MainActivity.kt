package com.example.roboticsortingsystem

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.roboticsortingsystem.ui.theme.RoboticSortingSystemTheme

private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
private const val RUNTIME_PERMISSION_REQUEST_CODE = 2

class MainActivity : ComponentActivity() {

    // Instantiate the Bluetooth adapter and manager
    private val bluetoothAdapter: BluetoothAdapter by lazy { // Lazy: only instatiated when needed
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    // Requests that the user enable Bluetooth if it's not enabled
    private var startBluetoothIntentForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) { // the actual check that Bluetooth is enabled
            showBluetoothDialog() // Sends request to show Bluetooth enable dialog
        }
    }

    private fun showBluetoothDialog() {
        // Need to request permission to connect at runtime in Android 12
        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) { // Enters function body if permission has not been given to enable Bluetooth
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // S = Android 12
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    2
                )
            return
            }
        }
        if(!bluetoothAdapter.isEnabled) { // With this implementation, if the user denies permission, it just pops the prompt up again
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE) // The intent asks the Android OS to enable Bluetooth
            startBluetoothIntentForResult.launch(enableBluetoothIntent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RoboticSortingSystemTheme {
                    RSSApp()
            }
        }
    }

    // Overrides start function to ensure Bluetooth is enabled any time the app is started or returned to
    override fun onStart() {
        super.onStart()
        showBluetoothDialog()
    }

}