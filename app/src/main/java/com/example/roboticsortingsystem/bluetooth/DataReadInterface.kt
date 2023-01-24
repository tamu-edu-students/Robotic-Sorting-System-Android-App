package com.example.roboticsortingsystem.bluetooth

import com.example.roboticsortingsystem.util.Resource
import kotlinx.coroutines.flow.MutableSharedFlow

// Use of interface allows easy interaction with ViewModel (and safer testing since you can
// create a "fake" data package on an interface

interface DataReadInterface {

    val configRead: MutableSharedFlow<Resource<ConfigurationPackage>>
    val weightRead: MutableSharedFlow<Resource<WeightPackage>>
    val connectionStateRead: MutableSharedFlow<Resource<ConnectionStatePackage>>

    fun reconnect()

    fun disconnect()

    fun receive()

    fun closeConnection()

    fun write(config: UInt) // Necessary to pass UInt to write from ViewModel
}