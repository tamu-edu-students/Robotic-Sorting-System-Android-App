package com.example.roboticsortingsystem.ui.theme

import android.bluetooth.BluetoothAdapter
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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
import com.example.roboticsortingsystem.components.ConfigurationApplyButton
import com.example.roboticsortingsystem.components.ConfigurationCancelButton
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import dagger.hilt.android.lifecycle.HiltViewModel

// Creates column of radio buttons for color choice (should be called from inside a column)
@Composable
fun ColorButtons(
    modifier: Modifier = Modifier,
    onSelectionChanged: (String) -> Unit = {} // Notifies caller that a selection was made/changed
) {
    // List stores all possible color choices
    val colorChoices = listOf(
        stringResource(id = R.string.color_red),
        stringResource(id = R.string.color_orange),
        stringResource(id = R.string.color_yellow),
        stringResource(id = R.string.color_green),
        stringResource(id = R.string.color_purple),
        stringResource(id = R.string.color_brown),
    )

    // Stores value selected by user and preserves it across rebuilds
    var selectedButton by rememberSaveable { mutableStateOf("")}

    // Creates a row for each color
    // Adapted from Google Android Basics with Compose "Cupcake" example application
    colorChoices.forEach { item ->
        Row(
            modifier = Modifier.selectable(
                selected = selectedButton == item,
                onClick = {
                    selectedButton = item
                    onSelectionChanged(item)
                }
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selectedButton == item,
                onClick = {
                    selectedButton = item
                    onSelectionChanged(item)
                })
            Text(item)
        }
    }
}

// Translates the raw value received from the RSS to the current color
fun colorIn(raw: Int) : String {
    // If the first digit is 0, the machine is currently configured for size
    if (raw % 100 != 0) {
        when (raw % 10) { // The second digit corresponds to a color
            0 -> { return "Red" }
            1 -> { return "Orange"}
            2 -> { return "Yellow"}
            3 -> { return "Green"}
            4 -> { return "Purple"}
            5 -> { return "Brown"}
            else -> { return "Unknown"}
        }
    } else {
        return "Currently configured for size"
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ColorSortingScreen(
    modifier: Modifier = Modifier,
    onCancelButtonClicked: () -> Unit = {},
    onBluetoothStateChanged: () -> Unit,
    viewModel: RSSViewModel = hiltViewModel()
) {

    // Bluetooth injection and update logic
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


    // UI
    var color1Input by remember { mutableStateOf("") } // Stores user's chosen colors
    var currentColor = ""
    if (bleConnectionState == ConnectionState.Uninitialized) {
        currentColor = "Reading configuration value..."
    } else {
        currentColor = colorIn(viewModel.configuration)
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()), // Allows vertical scrolling of column
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {

        // Color 1 selection
        Text(
            text = stringResource(id = R.string.color_color1),
            textAlign = TextAlign.Center,
            fontSize = 18.sp
        )
        Divider(thickness = 1.dp, modifier = modifier.padding(top = 16.dp, bottom = 16.dp))
        ColorButtons(onSelectionChanged = {
            when(it) { // Takes the number corresponding to the button the user selected and writes it to the ViewModel
                "Red" -> { viewModel.configuration = 10 }
                "Orange" -> { viewModel.configuration = 11 }
                "Yellow" -> { viewModel.configuration = 12 }
                "Green" -> { viewModel.configuration = 13 }
                "Purple" -> { viewModel.configuration = 14 }
                "Brown" -> { viewModel.configuration = 15 }
                else -> {} // Don't change anything if the returned color isn't valid
            }
        })
        // Can add Color 2 here if necessary

        // Persistent column for current color
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Current color: $currentColor",
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                fontStyle = FontStyle.Italic
            )
        }
        ConfigurationCancelButton(onClick = { onCancelButtonClicked() })
        ConfigurationApplyButton(onClick = { viewModel.writeToRSS() }) // Tells the ViewModel to write to the RSS
    }
}

// Preview function
@Preview
@Composable
fun ColorSortingScreenPreview() {
    // ColorSortingScreen()
}