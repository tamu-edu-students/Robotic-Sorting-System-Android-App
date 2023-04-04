package com.example.roboticsortingsystem.ui.theme

import android.bluetooth.BluetoothAdapter
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.roboticsortingsystem.RSSViewModel
import com.example.roboticsortingsystem.bluetooth.ConnectionState
import com.example.roboticsortingsystem.bluetooth.PermissionState
import com.example.roboticsortingsystem.bluetooth.SystemBroadcastReceiver
import com.example.roboticsortingsystem.components.RSSLoadingScreen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

// Provides common formatting for all diagnostic cards
@Composable
fun DiagnosticCard(
    title: String,
    info: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
            Text(
                text = info,
                fontSize = 24.sp
            )
        }
    }
}

// Provides diagnostic text for size sorting that can be expanded to n cutoffs
@Composable
fun SizeSortInfo (
    cutoffNumber: Int,
    viewModel: RSSViewModel = hiltViewModel()
) {
    if (viewModel.configuration[cutoffNumber].toInt() != 0) { // If no second cutoff is specified (is equal to 0), only one cutoff should be shown
        DiagnosticCard(title = "Size cutoff $cutoffNumber: ", info = "${viewModel.configuration[cutoffNumber]} cm")
    }
}

// Provides diagnostic text for color sorting that can be expanded to n cutoffs
@Composable
fun ColorSortInfo (
    binNumber: Int,
    viewModel: RSSViewModel = hiltViewModel()
) {
    when (viewModel.configuration[binNumber].toInt()) { // Displays correct color based on encoding
        1 -> { DiagnosticCard(title = "Color for bin $binNumber: ", info = "Red") }
        2 -> { DiagnosticCard(title = "Color for bin $binNumber: ", info = "Orange") }
        3 -> { DiagnosticCard(title = "Color for bin $binNumber: ", info = "Yellow") }
        4 -> { DiagnosticCard(title = "Color for bin $binNumber: ", info = "Green") }
        5 -> { DiagnosticCard(title = "Color for bin $binNumber: ", info = "Purple") }
        6 -> { DiagnosticCard(title = "Color for bin $binNumber: ", info = "Brown") }
        else -> { DiagnosticCard(title = "Color for bin $binNumber: ", info = "Unknown") }
    }
}

// Provides diagnostic text for defect-based sorting

@Composable
fun DefectSortInfo (
    viewModel: RSSViewModel = hiltViewModel()
) {
    DiagnosticCard(title = "Bin 1: ", info = "Healthy fruit")
    DiagnosticCard(title = "Bin 2: ", info = "Defective fruit")
}

@Composable
fun BeltControlInfo(
    beltControlValue: Int
) {
    when (beltControlValue) {
        -1 -> { DiagnosticCard(title = "Belt status: ", info = "FAULT")} // Intended for obstructed belt situation
        0 -> { DiagnosticCard(title = "Belt status: ", info = "Stopped") }
        1 -> { DiagnosticCard(title = "Belt status: ", info = "Running") }
        else -> { DiagnosticCard(title = "Belt status: ", info = "Unknown") }
    }
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


    // Nested columns ensure that arrangement works correctly
    if (bleConnectionState == ConnectionState.Connected) {


        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyColumn(
                modifier = modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (bleConnectionState == ConnectionState.Connected) {
                    // Display weight using "item" for LazyColumn
                    // Handle sensor error (sensor value = -1)
                    if (viewModel.weight[0].toInt() == 255) {
                        item {(DiagnosticCard(title = "Weight for bin 1: ", info = "FAULT"))}
                    } else {
                        item {(DiagnosticCard(title = "Weight for bin 1: ", info = "${viewModel.weight[0]}"))}
                    }

                    if (viewModel.weight[1].toInt() == 255) {
                        item {(DiagnosticCard(title = "Weight for bin 2: ", info = "FAULT"))}
                    } else {
                        item {(DiagnosticCard(title = "Weight for bin 2: ", info = "${viewModel.weight[1]}"))}
                    }

                    if (viewModel.weight[2].toInt() == 255) {
                        item {(DiagnosticCard(title = "Weight for bin 3: ", info = "FAULT"))}
                    } else {
                        item {(DiagnosticCard(title = "Weight for bin 3: ", info = "${viewModel.weight[2]}"))}
                    }
                    // Display sensor error
                    if (viewModel.weight[3].toInt() == 255) {
                        item {(DiagnosticCard(title = "Sensor status: ", info = "FAULT"))}
                    } else {
                        item {(DiagnosticCard(title = "Sensor status: ", info = "Normal"))}
                    }
                    // Display sorting configuration
                    item { when (viewModel.configuration.first().toInt()) {
                        1 -> { // Indicates size configuration
                            DiagnosticCard(title = "Sorting type: ", info = "Size")
                            SizeSortInfo(cutoffNumber = 1, viewModel)
                            SizeSortInfo(cutoffNumber = 2, viewModel)
                        }
                        2 -> { // Indicates color configuration
                            DiagnosticCard(title = "Sorting type: ", info = "Color")
                            ColorSortInfo(binNumber = 1, viewModel)
                            ColorSortInfo(binNumber = 2, viewModel)
                        }
                        3 -> { // Indicates defect configuration
                            DefectSortInfo(viewModel)
                        }
                        else -> DiagnosticCard(title = "Sorting type: ", info = "Unknown")
                    } }
                    // Display belt state
                    item {if (viewModel.configuration.lastIndex == 3) { // When setting up the screen, the last index may not be 3
                        BeltControlInfo(beltControlValue = viewModel.configuration[3].toInt())
                    }}
                    // Display connection state
                    item {
                        when (viewModel.connectionState) {
                            ConnectionState.Connected -> DiagnosticCard(title = "Connection state: ", info = "Connected")
                            ConnectionState.Disconnected -> DiagnosticCard(title = "Connection state: ", info = "Disconnected")
                            ConnectionState.Initializing -> DiagnosticCard(title = "Connection state: ", info = "Initializing")
                            ConnectionState.Uninitialized -> DiagnosticCard(title = "Connection state: ", info = "Uninitialized")
                            else -> DiagnosticCard(title = "Connection state: ", info = "Unknown")
                        }
                    }
                }
                else if (viewModel.errorMessage != null) {
                    item {DiagnosticCard(title = "viewModel error: ", info = viewModel.errorMessage!!)}
                }
            }
        }
    } else {
        RSSLoadingScreen()
    }
}

@Preview
@Composable
fun MachineInfoScreenPreview() {
    // MachineInfoScreen()
}