package com.example.roboticsortingsystem.bluetooth

import android.bluetooth.BluetoothAdapter
import android.content.Context
import com.example.roboticsortingsystem.util.Resource
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

// Need to use Dagger to get current Bluetooth adapter and context
class DataReadManager @Inject constructor(
    private val bluetoothAdapter: BluetoothAdapter,
    private val context: Context
) : DataReadInterface{
    override val dataRead: MutableSharedFlow<Resource<DataReadPackage>>
        // No value by default on launch
        get() = MutableSharedFlow()

    // Bluetooth logic instantiated as needed


    override fun reconnect() {
        TODO("Not yet implemented")
    }

    override fun disconnect() {
        TODO("Not yet implemented")
    }

    override fun recieve() {
        TODO("Not yet implemented")
    }

    override fun closeConnection() {
        TODO("Not yet implemented")
    }

}