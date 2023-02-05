package com.example.roboticsortingsystem.ui.theme

import android.bluetooth.BluetoothAdapter
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.roboticsortingsystem.R
import com.example.roboticsortingsystem.RSSViewModel
import com.example.roboticsortingsystem.bluetooth.ConnectionState
import com.example.roboticsortingsystem.bluetooth.PermissionState
import com.example.roboticsortingsystem.bluetooth.SystemBroadcastReceiver
import com.example.roboticsortingsystem.components.RSSLoadingScreen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

// Processes incoming conveyor belt data to user-readable format
fun statusIn(valueIn: Byte): String {
    return when (valueIn.toInt()) {
        0 -> "Stopped"
        1 -> "Running"
        else -> "Unknown"
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BeltControlScreen (
    modifier: Modifier = Modifier,
    onBluetoothStateChanged: () -> Unit,
    viewModel: RSSViewModel = hiltViewModel()
) {
    // *************************************************
    // Standard Bluetooth injection/update logic from other screens
    // *************************************************

    // SystemBroadcastReceiver listens for state changes from the BluetoothAdapter (e.g. user turns off Bluetooth)
    // and launches a re-activation prompt from the passed-in onBluetoothStateChanged function
    SystemBroadcastReceiver(systemAction = BluetoothAdapter.ACTION_STATE_CHANGED) { bluetoothState ->
        val action = bluetoothState?.action
            ?: return@SystemBroadcastReceiver // return if bluetoothState does not exist
        if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            onBluetoothStateChanged()
        }
    }

    val permissionState =
        rememberMultiplePermissionsState(permissions = PermissionState.permissions)
    val lifecycleOwner = LocalLifecycleOwner.current
    val bleConnectionState = viewModel.connectionState

    // DisposableEffect: checks for changes and relates that to the lifecycle owner (the class that "owns" the lifecycle)
    DisposableEffect(
        key1 = lifecycleOwner,
        effect = {
            val observer =
                LifecycleEventObserver { _, event -> // Observes lifecycle state for use by this DisposableEffect
                    if (event == Lifecycle.Event.ON_START) {
                        permissionState.launchMultiplePermissionRequest() // Launches request for relevant permissions on start
                        if (permissionState.allPermissionsGranted && bleConnectionState == ConnectionState.Disconnected) { // Can simply reconnect if all permissions have already been given
                            viewModel.reconnect()
                        }
                    }
                    if (event == Lifecycle.Event.ON_STOP) { // UI prompts viewModel to terminate Bluetooth connection at end of lifecycle
                        if (bleConnectionState == ConnectionState.Connected) {
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
            if (bleConnectionState == ConnectionState.Uninitialized) {
                viewModel.initializeConnection()
            }
        }
    }


    // *************************************************
    // UI logic
    // *************************************************
    val context = LocalContext.current
    var currentState = ""
    if (bleConnectionState == ConnectionState.Uninitialized) {
        currentState = "Reading belt status..."
    } else {
        if (viewModel.configuration.lastIndex == 3) { // Prevents crash before ViewModel fully initializes by preventing a read out of index range
            currentState = statusIn(viewModel.configuration[3])
        }
    }
    if (bleConnectionState == ConnectionState.Connected) {
        Column (
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row() {
                Button(onClick = {
                    viewModel.configuration[0] = viewModel.configuration[0] // These are necessary to preserve the current values in the ViewModel
                    viewModel.configuration[1] = viewModel.configuration[1]
                    viewModel.configuration[2] = viewModel.configuration[2]
                    viewModel.configuration[3] = 0 // Indicates to RSS to stop belt
                    viewModel.writeToRSS()
                    showToast(context, "Belt stopped")
                }) {
                    // Stop button
                    Text(
                        text = stringResource(id = R.string.belt_stop),
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = {
                    viewModel.configuration[0] = viewModel.configuration[0]
                    viewModel.configuration[1] = viewModel.configuration[1]
                    viewModel.configuration[2] = viewModel.configuration[2]
                    viewModel.configuration[3] = 1 // Indicates to RSS to start belt
                    viewModel.writeToRSS()
                    showToast(context, "Belt started")
                }) {
                    // Start button
                    Text(
                        text = stringResource(id = R.string.belt_start),
                        fontSize = 18.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    } else {
        RSSLoadingScreen()
    }
}