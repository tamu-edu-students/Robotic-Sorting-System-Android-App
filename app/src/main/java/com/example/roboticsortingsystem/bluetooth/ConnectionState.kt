package com.example.roboticsortingsystem.bluetooth

// Sealed interface: ConnectionState can only be one of these at a time
sealed interface ConnectionState {
    object Connected: ConnectionState
    object Disconnected: ConnectionState
    object Uninitialized: ConnectionState
    object Initializing: ConnectionState
}
