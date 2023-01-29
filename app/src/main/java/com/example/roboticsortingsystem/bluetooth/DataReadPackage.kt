package com.example.roboticsortingsystem.bluetooth

// Provides a standard format for the data read from the RSS
data class ConfigurationPackage(
    val configuration: ByteArray // used for BLE transmission
)
data class WeightPackage(
    val weight: UInt
)
data class ConnectionStatePackage(
    val connectionState: ConnectionState
)