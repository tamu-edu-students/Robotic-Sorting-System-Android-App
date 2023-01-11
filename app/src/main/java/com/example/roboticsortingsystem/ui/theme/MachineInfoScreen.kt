package com.example.roboticsortingsystem.ui.theme

import android.bluetooth.BluetoothAdapter
import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.roboticsortingsystem.R
import com.example.roboticsortingsystem.RSSViewModel
import com.example.roboticsortingsystem.bluetooth.ConnectionState
import com.example.roboticsortingsystem.bluetooth.PermissionState
import com.example.roboticsortingsystem.bluetooth.SystemBroadcastReceiver
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

// Provides common formatting for all text in the diagnostic box
@Composable
fun DiagnosticText(
    info: String
) {
    Text(
        text = info,
        textAlign = TextAlign.Center
    )
}

// Draws the screen that shows diagnostic information from the RSS
@OptIn(ExperimentalPermissionsApi::class)   // The Accompanist Jetpack Compose permissions library is still technically experimental,
                                            // but necessary for Composables to check permissions
@Composable
fun MachineInfoScreen(
    modifier: Modifier = Modifier,
    onBluetoothStateChanged: () -> Unit,
    viewModel: RSSViewModel = hiltViewModel() // Exposes Bluetooth data from the underlying Bluetooth framework via ViewModel
) {
    // SystemBroadcastReceiver listens for state changes from the BluetoothAdapter (e.g. user turns off Bluetooth) 
    // and launches a re-activation prompt from the passed-in onBluetoothStateChanged function
    SystemBroadcastReceiver(systemAction = BluetoothAdapter.ACTION_STATE_CHANGED) { bluetoothState ->
        val action = bluetoothState?.action ?: return@SystemBroadcastReceiver // return if bluetoothState does not exist
        if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            onBluetoothStateChanged()
        }
    }

    val permissionState = rememberMultiplePermissionsState(permissions = PermissionState.permissions)
    val lifecycleOwner = LocalLifecycleOwner.current
    val bleConnectionState = viewModel.connectionState

    // DisposableEffect: checks for changes and relates that to the lifecycle owner (the class that "owns" the lifecycle)
    DisposableEffect(
        key1 = lifecycleOwner,
        effect = {
            val observer = LifecycleEventObserver {_, event -> // Observes lifecycle state for use by this DisposableEffect
                if(event == Lifecycle.Event.ON_START){
                    permissionState.launchMultiplePermissionRequest() // Launches request for relevant permissions on start
                    if(permissionState.allPermissionsGranted && bleConnectionState == ConnectionState.Disconnected){ // Can simply reconnect if all permissions have already been given
                        viewModel.reconnect()
                    }
                }
                if(event == Lifecycle.Event.ON_STOP){ // UI prompts viewModel to terminate Bluetooth connection at end of lifecycle
                    if (bleConnectionState == ConnectionState.Connected){
                        viewModel.disconnect()
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose { // Gets rid of observer on close to free up system resources
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    )

    // LaunchedEffect launches when all permissions are granted: specifically, starts connection if all permissions are granted and the connection is not already established
    LaunchedEffect(key1 = permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted) {
            if (bleConnectionState  == ConnectionState.Uninitialized) {
                viewModel.initializeConnection()
            }
        }
    }

    /* TODO: add functionality that shows diagnostic info from the machine
    * Such as:
    * - Current color sensed
    * - Current weight sensed for each bin
    * - Current size sensed
    * */

    // This is a placeholder: actual info will go here during integration in 404
    // Nested columns ensure that arrangement works correctly
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .weight(1f)
                .border(width = 1.dp, color = Color.Green) // Basic theming to identify this as diagnostic info: will make more visually appealing later
        ) {
            // TODO: change to a fixed-width font in DiagnosticTest function
            if (bleConnectionState == ConnectionState.Uninitialized) {
                DiagnosticText(info = "Uninitialized")
            }
            else if (bleConnectionState == ConnectionState.Initializing) {
                if (viewModel.initializingMessage != null) {
                    Text(
                        text = viewModel.initializingMessage!!
                    )
                }
            }
            else if (bleConnectionState == ConnectionState.Connected) {
                DiagnosticText(info = "Weight: ${viewModel.weight}")
                DiagnosticText(info = "Configuration: ${viewModel.configuration}")
                DiagnosticText(info = "Connection state: ${viewModel.connectionState}")
            }
            else if (viewModel.errorMessage != null) {
                DiagnosticText(info = viewModel.errorMessage!!)
            }
        }
    }
}

@Preview
@Composable
fun MachineInfoScreenPreview() {
    // MachineInfoScreen()
}