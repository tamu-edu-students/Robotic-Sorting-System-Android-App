package com.example.roboticsortingsystem

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
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
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.roboticsortingsystem.bluetooth.hasRequiredRuntimePermissions
import com.example.roboticsortingsystem.bluetooth.requestRelevantRuntimePermissions
import com.example.roboticsortingsystem.ui.theme.RoboticSortingSystemTheme
import java.util.*

private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
private const val RUNTIME_PERMISSION_REQUEST_CODE = 2
private const val GATT_MAX_MTU_SIZE = 517 // as specified by Android source code
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

    // Debug function used to discover services on connected Bluetooth devices
    private fun BluetoothGatt.printGattTable() {
        if (services.isEmpty()) {
            Log.i("printGattTable", "No services found (may need to call discoverServices()")
            return
        }
        services.forEach { service -> // Creates a unique entry for each service
            val characteristicsTable = service.characteristics.joinToString ( // Table formatting
                separator = "\n|--",
                prefix = "|--"
            ) { it.uuid.toString() }
            Log.i("printGattTable", "\nService ${service.uuid}\nCharacteristics:\n$characteristicsTable") // Actually prints table to logcat
        }
    }

    // Callback for gatt connection
    @SuppressLint("MissingPermission")
    private val gattCallback = object: BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w("BluetoothGattCallback", "Successfully connected to $deviceAddress")
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Handler(Looper.getMainLooper()).post {
                        gatt.discoverServices()
                    }
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.w("BluetoothGattCallback", "Successfully disconnected from $deviceAddress")
                gatt.close()
            } else {
                Log.w("BluetoothGattCallback", "Error when connecting to $deviceAddress: $status")
                gatt.close()
            }
        }
        // Set behavior when a service is discovered
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                Log.w("BluetoothGattCallback", "Discovered ${services.size} services for device ${device.name} at ${device.address}")
                printGattTable() // Prints table of services
                gatt.requestMtu(GATT_MAX_MTU_SIZE) // Note minimum MTU size is 23
                readRSSWeight(gatt) // Test of read functionality
            }
        }
        // Request larger Maximum Transmission Unit (MTU)
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.w("BluetoothGattCallback","MTU changed to $mtu")
        }
        // Handle callback after read
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> { // Value read successfully
                        Log.i("BluetoothGattCallback", "Read characteristic $uuid:\n${value.toHexString()}")
                    }
                    BluetoothGatt.GATT_READ_NOT_PERMITTED -> { // Read permission denied
                        Log.e("BluetoothGattCallback", "Read not permitted for $uuid")
                    }
                    else -> {
                        Log.e("BluetoothGattCallback", "Read failed for $uuid with error $status")
                    }
                }
            }
        }
    }
    // Functions that check if a characteristic is readable/writable
    fun BluetoothGattCharacteristic.containsProperty(property: Int) : Boolean {
        return properties and property != 0 // Gets properties for the characteristic function to interpret
    }
    fun BluetoothGattCharacteristic.isReadable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_READ)
    fun BluetoothGattCharacteristic.isWritable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)
    fun BluetoothGattCharacteristic.isWritableWithoutResponse(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)

    // Convert values to hex string for reading characteristics
    fun ByteArray.toHexString(): String =
        joinToString(separator = " ", prefix = "0x") {String.format("%02X", it)}
    // RSS services: UUID 0x2022, weight characteristic read-only (0x1234), configuration characteristic write/read (0x5678)
    @SuppressLint("MissingPermission")
    private fun readRSSWeight(gatt: BluetoothGatt) {
        val rssUUID = UUID.fromString("e2d36f99-8909-4136-9a49-d825508b297b") // RSS service UUID
        val rssWeightUUID = UUID.fromString("00001234-0000-1000-8000-00805f9b34fb") // Weight characteristic UUID
        val rssWeight = gatt.getService(rssUUID)?.getCharacteristic(rssWeightUUID)
        if (rssWeight?.isReadable() == true) {
            gatt.readCharacteristic(rssWeight)
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