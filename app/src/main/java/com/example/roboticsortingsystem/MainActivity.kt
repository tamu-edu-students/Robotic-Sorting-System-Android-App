package com.example.roboticsortingsystem

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.roboticsortingsystem.bluetooth.hasRequiredRuntimePermissions
import com.example.roboticsortingsystem.bluetooth.requestRelevantRuntimePermissions
import com.example.roboticsortingsystem.ui.theme.RoboticSortingSystemTheme

private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
private const val RUNTIME_PERMISSION_REQUEST_CODE = 2
// Sets name of Bluetooth device to automatically connect to
private const val DEVICE_TO_CONNECT = "RoboticSortingSystemTest"

class MainActivity : ComponentActivity() {

    // Instantiate the Bluetooth adapter and manager
    private val bluetoothAdapter: BluetoothAdapter by lazy { // Lazy: only instantiated when needed
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    // Used to indicate whether Bluetooth scan is active
    private var isScanning = false
    // Starts Bluetooth scan (and ensures that the app has permission to do so)
    @SuppressLint("MissingPermission") // Prevents Android Studio from getting upset that there's no permission check here: they're in the Context extensions
    private fun startBleScan() {
        if (!hasRequiredRuntimePermissions()) {
            requestRelevantRuntimePermissions()
        }
        bleScanner.startScan(null, scanSettings, scanCallback)
        isScanning = true

    }
    // Stops Bluetooth scan
    @SuppressLint("MissingPermission")
    private fun stopBleScan() {
        if (!hasRequiredRuntimePermissions()) {
            requestRelevantRuntimePermissions()
        }
            bleScanner.stopScan(scanCallback)
            isScanning = false
    }

    // Instantiates the Bluetooth LE scanner
    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    // Used to configure settings for scanner
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // Ideal for finding a specific sort of device quickly - here, the Raspberry Pi
        .build()

    // Notifies the application when a scan result is available
    private val scanCallback = object: ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
                    with(result.device) {
                        Log.i("ScanCallback", "Found BLE device named ${name ?: "Unnamed"} with address $address") // Returns Unnamed if device does not broadcast a name
                        if (name == DEVICE_TO_CONNECT) {
                            Log.w("ScanCallback", "Match found! Connecting to $name at $address")
                            stopBleScan() // Keeps device from continuing to scan for Bluetooth devices after connection, which wastes resources
                            connectGatt(this@MainActivity, false, gattCallback)
                        }
                    }
            }
    }

    // Callback for gatt connection
    @SuppressLint("MissingPermission")
    private val gattCallback = object: BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w("BluetoothGattCallback", "Successfully connected to $deviceAddress")
                // Store reference to BluetoothGatt
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.w("BluetoothGattCallback", "Successfully disconnected from $deviceAddress")
                gatt.close()
            } else {
                Log.w("BluetoothGattCallback", "Error when connecting to $deviceAddress: $status")
                gatt.close()
            }
        }
    }

    // Requests that the user enable Bluetooth if it's not enabled
    private var startBluetoothIntentForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) { // the actual check that Bluetooth is enabled
            showBluetoothDialog() // Sends request to show Bluetooth enable dialog
        }
    }

    // Sends the Bluetooth enable request to the system
    private fun showBluetoothDialog() {
        // Need to request permission to connect at runtime in Android 12
        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) { // Enters function body if permission has not been given to enable Bluetooth
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // S = Android 12
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN),
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

    // Calls and starts the app UI
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
        startBleScan()
    }

}