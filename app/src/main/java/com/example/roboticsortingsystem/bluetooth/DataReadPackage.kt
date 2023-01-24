package com.example.roboticsortingsystem.bluetooth

// Provides a standard format for the data read from the RSS
data class ConfigurationPackage(
    val configuration: UInt // Numerical package use unsigned ints: no need for negative weight or configuration
)
data class WeightPackage(
    val weight: UInt
)
data class ConnectionStatePackage(
    val connectionState: ConnectionState
)