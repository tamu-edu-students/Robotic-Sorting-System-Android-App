package com.example.roboticsortingsystem

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roboticsortingsystem.bluetooth.ConnectionState
import com.example.roboticsortingsystem.bluetooth.DataReadInterface
import com.example.roboticsortingsystem.bluetooth.DataReadPackage
import com.example.roboticsortingsystem.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RSSViewModel @Inject constructor(
    private val dataReadInterface: DataReadInterface // allows interaction with BLE connection logic
) : ViewModel() {
    // Define initial variables
    var initializingMessage by mutableStateOf<String?>(null)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var weight by mutableStateOf(0)
        private set // User should not write weight values
    var configuration by mutableStateOf(0)
    var connectionState by mutableStateOf<ConnectionState>(ConnectionState.Uninitialized)

    private fun subscribeToChanges() { // Updates ViewModel every time the read value changes
        viewModelScope.launch {
            dataReadInterface.dataRead.collect { result ->
                when (result) {
                    is Resource.Success -> { // If everything works right, store the received weight and configuration values in the ViewModel
                        weight = result.data.weight
                        configuration = result.data.configuration
                        connectionState = result.data.connectionState
                    }
                    is Resource.Loading -> {
                        initializingMessage = result.message // Allows a loading message while the connection is being established
                        connectionState = ConnectionState.Initializing
                    }
                    is Resource.Error -> {
                        errorMessage = result.errorMessage // Show an error message if there's an error
                        connectionState = ConnectionState.Uninitialized
                    }
                }
            }
        }
    }

    // Disconnect/reconnect is faster than dataReadInterface.closeConnection()
    fun disconnect() {
        dataReadInterface.disconnect()
    }

    fun reconnect() {
        dataReadInterface.reconnect()
    }

    // Start BLE connection and connect it to the ViewModel
    fun initializeConnection() {
        errorMessage = null
        subscribeToChanges() // Allows ViewModel to get BLE results
        dataReadInterface.receive() // Starts the Bluetooth scan
    }

    // Handle case where the ViewModel is cleared for whatever reason
    override fun onCleared() {
        super.onCleared()
        dataReadInterface.closeConnection() // Disconnect upon ViewModel clearing
    }
}