package com.example.roboticsortingsystem.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.roboticsortingsystem.*
import com.example.roboticsortingsystem.util.Resource
import com.example.roboticsortingsystem.bluetooth.DataReadPackage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

// Need to use Dagger to get current activity, Bluetooth adapter, and context
class DataReadWriteManager @Inject constructor(
    private val bluetoothAdapter: BluetoothAdapter,
    private val context: Context
) : DataReadInterface{

    // Allows data to be emitted to the ViewModel (and update the Compose UI when it's received)
    override val dataRead: MutableSharedFlow<Resource<DataReadPackage>> = MutableSharedFlow()

    // Bluetooth logic instantiated as needed
    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    // Starts Bluetooth scan (and ensures that the app has permission to do so)
    @SuppressLint("MissingPermission") // Prevents Android Studio from getting upset that there's no permission check here: they're in the Context extensions
    private fun startBleScan() {
        bleScanner.startScan(null, scanSettings, scanCallback)
        isScanning = true

    }

    // Stops Bluetooth scan
    @SuppressLint("MissingPermission")
    private fun stopBleScan() {
        bleScanner.stopScan(scanCallback)
        isScanning = false
    }

    // Applies scan settings
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)  // Scans for a short period of time at low latency
        .build()

    private var gatt: BluetoothGatt? = null
    private var isScanning = false // Not scanning by default
    private val coroutineScope = CoroutineScope(Dispatchers.Default) // Allows export of data to ViewModel


    // Notifies the application when a scan result is available
    private val scanCallback = object: ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
                with(result.device) {
                    Log.i("ScanCallback", "Found BLE device named ${name ?: "Unnamed"} with address $address") // Returns Unnamed if device does not broadcast a name
                    if (name == DEVICE_TO_CONNECT) {
                        Log.w("ScanCallback", "Match found! Connecting to $name at $address")
                        coroutineScope.launch {
                            dataRead.emit(Resource.Loading(message = "RSS found. Connecting..."))
                        }
                        stopBleScan() // Keeps device from continuing to scan for Bluetooth devices after connection, which wastes resources
                        connectGatt(context, false, gattCallback)
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

    @SuppressLint("MissingPermission")
    fun rssRead(
        gatt: BluetoothGatt,
    ) {
        val rssUUID = UUID.fromString(RSS_SERVICE_UUID)
        val rssWeightUUID = UUID.fromString(RSS_WEIGHT_UUID)
        val rssConfigUUID = UUID.fromString(RSS_CONFIG_UUID)
        val weightCharacteristic = gatt.getService(rssUUID).getCharacteristic(rssWeightUUID)
        val configCharacteristic = gatt.getService(rssUUID).getCharacteristic(rssConfigUUID)


        if (weightCharacteristic.isReadable()) { // Characteristic needs to be initialized and readable
            gatt.readCharacteristic(weightCharacteristic) // Value is actually read in onCharacteristicRead, this just returns a boolean
        }
        if (configCharacteristic.isReadable()) {
            gatt.readCharacteristic(configCharacteristic) // Not read: too fast?
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

    // Convert values to hex string for reading characteristics
    fun ByteArray.toHexString(): String =
        joinToString(separator = " ", prefix = "0x") {String.format("%02X", it)}

    // Callback for gatt connection
    @SuppressLint("MissingPermission")
    private val gattCallback = object: BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w("BluetoothGattCallback", "Successfully connected to $deviceAddress")
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    coroutineScope.launch {
                        dataRead.emit(Resource.Loading(message = "Discovering RSS services..."))
                    }
                    Handler(Looper.getMainLooper()).post {
                        gatt.discoverServices()
                    }
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.w("BluetoothGattCallback", "Successfully disconnected from $deviceAddress")
                coroutineScope.launch {
                    dataRead.emit(Resource.Success(data = DataReadPackage(0, 0, ConnectionState.Disconnected)))
                }
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
                rssRead(gatt) // Initial read to initialize ViewModel
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
            var rssConfig: Int = 0
            var rssWeight: Int = 0
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> { // Value read successfully
                        Log.i("BluetoothGattCallback", "Read characteristic $uuid:\n${value.toHexString()}")
                        if (uuid.toString() == RSS_WEIGHT_UUID) {
                            rssWeight = value.first().toInt()
                        } else {
                            rssConfig = value.first().toInt()
                        }
                    }
                    BluetoothGatt.GATT_READ_NOT_PERMITTED -> { // Read permission denied
                        Log.e("BluetoothGattCallback", "Read not permitted for $uuid")
                    }
                    else -> {
                        Log.e("BluetoothGattCallback", "Read failed for $uuid with error $status")
                    }
                }
            }
            // Push updated values to ViewModel
            val readValues = DataReadPackage (
                rssConfig,
                rssWeight,
                ConnectionState.Connected
            )
            coroutineScope.launch {
                dataRead.emit(
                    Resource.Success(data = readValues)
                )
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

        override fun onCharacteristicChanged( // Update ViewModel if a characteristic changes
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            if (gatt != null) {
                rssRead(gatt)
            }
        }
    }

    // Manage connection/reconnection as necessary
    @SuppressLint("MissingPermission")
    override fun reconnect() {
        gatt?.connect()
    }

    @SuppressLint("MissingPermission")
    override fun disconnect() {
        gatt?.disconnect()
    }

    @SuppressLint("MissingPermission")
    override fun receive() {
        coroutineScope.launch {
            dataRead.emit(Resource.Loading(message = "Scanning for RSS BLE"))
        }
        isScanning = true
        bleScanner.startScan(null, scanSettings, scanCallback)

    }

    @SuppressLint("MissingPermission")
    override fun closeConnection() {
        gatt?.close()
    }

}