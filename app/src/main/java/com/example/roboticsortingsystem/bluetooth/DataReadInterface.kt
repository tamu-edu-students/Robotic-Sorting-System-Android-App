package com.example.roboticsortingsystem.bluetooth

import com.example.roboticsortingsystem.util.Resource
import kotlinx.coroutines.flow.MutableSharedFlow

// Use of interface allows easy interaction with ViewModel (and safer testing since you can
// create a "fake" data package on an interface

interface DataReadInterface {

    val dataRead: MutableSharedFlow<Resource<DataReadPackage>>

    fun reconnect()

    fun disconnect()

    fun recieve()

    fun closeConnection()

}