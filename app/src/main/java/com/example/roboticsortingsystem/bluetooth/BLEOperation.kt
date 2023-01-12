package com.example.roboticsortingsystem.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import java.util.UUID

// Sealed class representing types of BLE operations for use in building queue
sealed class BLEOperation {
    abstract val gatt: BluetoothGatt
}

// Operations are represented as subclasses of BLEOperation

data class characteristicRead (
    override val gatt: BluetoothGatt,
    val serviceUUID: UUID,
    val characteristicUUID: UUID
        ) : BLEOperation()