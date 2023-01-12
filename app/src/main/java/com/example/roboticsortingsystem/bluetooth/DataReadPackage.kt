package com.example.roboticsortingsystem.bluetooth

// Provides a standard format for the data read from the RSS
data class ConfigurationPackage(
    val configuration: Int
)
data class WeightPackage(
    val weight: Int
)
data class ConnectionStatePackage(
    val connectionState: ConnectionState
)