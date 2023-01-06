package com.example.roboticsortingsystem

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
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
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject

const val GATT_MAX_MTU_SIZE = 517 // as specified by Android source code
// Sets name of Bluetooth device to automatically connect to
const val DEVICE_TO_CONNECT = "Robotic Sorting System"
// UUIDs for Robotic Sorting System service and characteristics
const val RSS_SERVICE_UUID = "4f5a4acc-6434-4d33-a791-589fdca0daf5"
const val RSS_WEIGHT_UUID = "4f5641bf-1119-4d1f-932d-fff7840ddc02"
const val RSS_CONFIG_UUID = "89097689-8bc2-44cb-9142-f17c71ed24f8"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var bluetoothAdapter: BluetoothAdapter

    // Instantiates the Bluetooth LE scanner
    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
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
    /* override fun onStart() {
        super.onStart()
        showBluetoothDialog()
        startBleScan()
    } */

}