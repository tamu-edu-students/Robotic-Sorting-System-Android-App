package com.example.roboticsortingsystem

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent.getActivity
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

private const val GATT_MAX_MTU_SIZE = 517 // as specified by Android source code
// Sets name of Bluetooth device to automatically connect to
private const val DEVICE_TO_CONNECT = "Robotic Sorting System"
// UUIDs for Robotic Sorting System service and characteristics
private const val RSS_SERVICE_UUID = "4f5a4acc-6434-4d33-a791-589fdca0daf5"
private const val RSS_WEIGHT_UUID = "4f5641bf-1119-4d1f-932d-fff7840ddc02"
private const val RSS_CONFIG_UUID = "89097689-8bc2-44cb-9142-f17c71ed24f8"

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
                // writeRSSConfig(gatt, byteArrayOf(0x15)) // Test of write functionality
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
        // Handle callback after write
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Log.i("BluetoothGattCallback", "Wrote value ${value.toHexString()} to service $uuid")
                        readRSSConfig(gatt) // Temporally spaced read test
                    }
                    BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> { // Invoked if data packet is bigger than MTU
                        Log.e("BluetoothGattCallback", "Attempted write exceeded size of MTU")
                    }
                    BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> {
                        Log.e("BluetoothGattCallback", "Write to service $uuid not permitted")
                    }
                    else -> {
                        Log.e("BluetoothGattCallback", "Write to characteristic of $uuid failed with error $status")
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
    // RSS services: UUID e2d36f99-8909-4136-9a49-d825508b297b, weight characteristic read-only (0x1234), configuration characteristic write/read (0x5678)
    @SuppressLint("MissingPermission")
    private fun readRSSWeight(gatt: BluetoothGatt) {
        val rssUUID = UUID.fromString(RSS_SERVICE_UUID)
        val rssWeightUUID = UUID.fromString(RSS_WEIGHT_UUID)
        val rssWeight = gatt.getService(rssUUID)?.getCharacteristic(rssWeightUUID)
        if (rssWeight?.isReadable() == true) {
            gatt.readCharacteristic(rssWeight)
        }
    }

    @SuppressLint("MissingPermission")
    private fun readRSSConfig(gatt: BluetoothGatt) {
        val rssUUID = UUID.fromString(RSS_SERVICE_UUID)
        val rssConfigUUID = UUID.fromString(RSS_CONFIG_UUID)
        val rssConfig = gatt.getService(rssUUID)?.getCharacteristic(rssConfigUUID)
        if (rssConfig?.isReadable() == true) {
            gatt.readCharacteristic(rssConfig)
        }
    }

    @SuppressLint("MissingPermission")
    private fun writeRSSConfig(gatt: BluetoothGatt, payload: ByteArray) {
        val rssUUID = UUID.fromString(RSS_SERVICE_UUID)
        val rssConfigUUID = UUID.fromString(RSS_CONFIG_UUID)
        val rssConfig = gatt.getService(rssUUID)?.getCharacteristic(rssConfigUUID)
        val writeType = when {
            rssConfig?.isWritable() == true -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT // Default write (with response)
            rssConfig?.isWritableWithoutResponse() == true -> BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            else -> error("Cannot write configuration")
        }
        rssConfig.writeType = writeType // Sets type of write
        rssConfig.value = payload // Sets data to write
        gatt.writeCharacteristic(rssConfig) // Writes to peripheral
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