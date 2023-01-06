package com.example.roboticsortingsystem.bluetooth

// Provides a standard format for the data read from the RSS
data class DataReadPackage(
    val configuration:Int,
    val weight:Int,
)
