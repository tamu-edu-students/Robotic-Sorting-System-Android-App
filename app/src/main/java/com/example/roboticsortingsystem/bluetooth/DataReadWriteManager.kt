package com.example.roboticsortingsystem.bluetooth

import android.Manifest
import android.annotation.SuppressLint
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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject

// Need to use Dagger to get current activity, Bluetooth adapter, and context
class DataReadWriteManager @Inject constructor(
    private val bluetoothAdapter: BluetoothAdapter,
    private val context: Context,
) : DataReadInterface{

    // Allows data to be emitted to the ViewModel (and update the Compose UI when it's received)
    override val configRead: MutableSharedFlow<Resource<ConfigurationPackage>> = MutableSharedFlow()
    override val weightRead: MutableSharedFlow<Resource<WeightPackage>> = MutableSharedFlow()
    override val connectionStateRead: MutableSharedFlow<Resource<ConnectionStatePackage>> = MutableSharedFlow()

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
                            connectionStateRead.emit(Resource.Loading(message = "RSS found. Connecting..."))
                        }
                        stopBleScan() // Keeps device from continuing to scan for Bluetooth devices after connection, which wastes resources
                        connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE) // Final argument specifies a BLE connection. Necessary for Android 13 to work
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
    suspend fun rssRead( // Suspend due to delay call
        gatt: BluetoothGatt
    ) {
        delay(500L) // Necessary to let the system "catch its breath"
        val rssUUID = UUID.fromString(RSS_SERVICE_UUID)
        val rssWeightUUID = UUID.fromString(RSS_WEIGHT_UUID)
        val rssConfigUUID = UUID.fromString(RSS_CONFIG_UUID)
        addOperationToQueue(characteristicRead(gatt, rssUUID, rssWeightUUID)) // Read weight
        addOperationToQueue(characteristicRead(gatt, rssUUID, rssConfigUUID))
    }

    @SuppressLint("MissingPermission")
    fun rssWrite(
        gatt: BluetoothGatt,
        configFromViewModel: ByteArray
    ) {
        val rssUUID = UUID.fromString(RSS_SERVICE_UUID)
        val rssConfigUUID = UUID.fromString(RSS_CONFIG_UUID)
        // Write configuration
        addOperationToQueue(characteristicWrite(gatt, rssUUID, rssConfigUUID, configFromViewModel))
        // Cannot write weight
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
                        configRead.emit(Resource.Loading(message = "Discovering RSS services..."))
                    }
                    Handler(Looper.getMainLooper()).post {
                        gatt.discoverServices()
                    }
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.w("BluetoothGattCallback", "Successfully disconnected from $deviceAddress")
                coroutineScope.launch {
                    connectionStateRead.emit(Resource.Success(ConnectionStatePackage(ConnectionState.Disconnected)))
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
                this@DataReadWriteManager.gatt = gatt // Sets DataReadWriteManager-level gatt variable so other functions can use it
                coroutineScope.launch {
                    rssRead(gatt) // Initial read to initialize ViewModel
                }
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
                        if (uuid.toString() == RSS_WEIGHT_UUID) {
                            val rssWeight = value
                            val weightCapsule = WeightPackage(rssWeight) // Puts the weight in a weightPackage ("capsule") to go to the ViewModel
                            coroutineScope.launch {
                                weightRead.emit(Resource.Success(weightCapsule))
                            }

                        } else {
                            val rssConfig = value // Writes whole ByteArray to ViewModel
                            val configCapsule = ConfigurationPackage(rssConfig)
                            coroutineScope.launch {
                                configRead.emit(Resource.Success(configCapsule))
                            }
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
            if (pendingOperation is characteristicRead) { // Callback is complete: indicate to queue that a read operation is done
                endOfOperation()
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
            if (pendingOperation is characteristicWrite) { // Indicates to queue that write is complete
                endOfOperation()
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
    override fun receive() { // Starts scan for BLE devices
        coroutineScope.launch {
            connectionStateRead.emit(Resource.Loading(message = "Scanning for RSS BLE"))
        }
        isScanning = true
        bleScanner.startScan(null, scanSettings, scanCallback)

    }

    @SuppressLint("MissingPermission")
    override fun closeConnection() {
        gatt?.close()
    }

    @SuppressLint("MissingPermission")
    override fun write(config: ByteArray) {
        gatt?.let { rssWrite(it, config) } // Check if gatt is null before passing it as a parameter
    }

    // Queue functions
    private val operationQueue = ConcurrentLinkedQueue<BLEOperation>() // Creates a queue of BLEOperations with helpful prebuilt methods
    private var pendingOperation: BLEOperation? = null // Stores next operation in queue

    // Add to queue
    @Synchronized // Prevents other functions from being run in parallel with this function, preventing temporal issues
    private fun addOperationToQueue (operation: BLEOperation) {
        operationQueue.add(operation)
        if (pendingOperation == null) {
            nextOperation() // Move on to the pending operation if there is one
        }
    }

    // Execute the operation on the top of the stack (first-in-first-out system)
    @Synchronized
    @SuppressLint("MissingPermission")
    private fun nextOperation() {
        // Return if another operation is already executing
        if (pendingOperation != null) {
            Log.e("nextOperation", "Error: attempted to call nextOperation during an operation execution")
            return
        }
        // Return if the operation queue is empty
        val operation = operationQueue.poll() ?: run {
            Log.i("nextOperation","nextOperation() called with nothing on queue: returning")
            return
        }
        // Otherwise, assign pendingOperation to operation on top of stack and execute it
        pendingOperation = operation

        when (operation) {
            is characteristicRead -> with (operation) {
                val characteristicToRead = gatt.getService(serviceUUID)?.getCharacteristic(characteristicUUID)
                if (characteristicToRead != null) {
                    if (characteristicToRead.isReadable()) {
                        gatt.readCharacteristic(characteristicToRead)
                    } else {
                        Log.e("nextOperation", "Characteristic with UUID $characteristicUUID is not readable")
                    }
                } else {
                    Log.e("nextOperation", "Did not find characteristic with UUID $characteristicUUID")
                }
                // Call to endOfOperation() is at end of onCharacteristicRead() callback
            }
            is characteristicWrite -> with (operation) {
                val characteristicToWrite = gatt.getService(serviceUUID)?.getCharacteristic(characteristicUUID)
                if (characteristicToWrite != null) {
                    val writeType = when { // Set write type based on whether the characteristic is writable w/ a response or not
                        characteristicToWrite.isWritable() -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                        characteristicToWrite.isWritableWithoutResponse() -> BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                        else -> { Log.e("nextOperation", "Cannot write to $characteristicUUID") }
                    }
                    // Set write type
                    characteristicToWrite.writeType = writeType
                    // Perform the write
                    characteristicToWrite.value = writeValue // Writes ByteArray to RSS
                    gatt.writeCharacteristic(characteristicToWrite)
                } else {
                    Log.e("nextOperation", "Did not find characteristic with UUID $characteristicUUID to write")
                }
                // Call to endOfOperation() is at end of onCharacteristicWrite() callback
            }
        }
    }

    // Notify queue that an operation has completed
    @Synchronized
    private fun endOfOperation() {
        Log.d("DataReadWriteManager", "End of $pendingOperation")
        pendingOperation = null
        // Move to next operation if there is one
        if (operationQueue.isNotEmpty()) {
            nextOperation()
        }
    }
}