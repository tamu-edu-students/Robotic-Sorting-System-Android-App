package com.example.roboticsortingsystem.ui.theme

import android.bluetooth.BluetoothAdapter
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
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

@Composable
// Common framework for the two text boxes: simplifies code in SizeSortingScreen
fun InputBox(
    @StringRes label: Int, // Value displayed on text box
    entry: String,
    onValueChange: (String) -> Unit = {},
    keyboardOptions: KeyboardOptions // Used to pass in a style of keyboard
) {

    TextField(
        value = entry,
        onValueChange = onValueChange,
        label = { Text(stringResource(id = label)) },
        keyboardOptions = keyboardOptions // Sets the type of keyboard as passed in by the caller
    )
}

// Converts raw numbers from ViewModel to an easily displayed string
fun sizeIn(raw: Int) : String {
    return if (raw % 100 != 1) { // If the first digit is 1, the machine is currently configured for color
        "$raw in."
    }
    else {
        "Currently configured for color"
    }
}

// Converts from user input string to int that goes to the ViewModel
fun sizeOut(raw: String): Int {
    return if (raw.toInt() > 9) {
        Log.e("sizeOut","Error: could not write input (out of range). Writing 0...")
        0 // Returns 0 if the input is out of bounds, which can be handled as an error
    } else {
        raw.toInt()
    }
}

// Shows the screen used to pass size configurations to the RSS
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SizeSortingScreen (
    modifier: Modifier = Modifier,
    onCancelButtonClicked: () -> Unit = {}, // Allows the main RSSScreen to pass in cancel button behavior
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


    // UI logic
    var size1Input by rememberSaveable { mutableStateOf("") } // Passed to InputBox to display user text. rememberSaveable: text stays when device rotated
    var currentSize = ""
    if (bleConnectionState == ConnectionState.Uninitialized) {
        currentSize = "Reading configuration value..."
    } else {
        currentSize = sizeIn(viewModel.configuration)
    }
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()), // Allows scrolling for smaller screens
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Size 1 input box
        InputBox(
            label = R.string.size_size1_box_label,
            entry = size1Input,
            onValueChange = {
                size1Input = it // Shows change to user
                viewModel.configuration = sizeOut(it) }, // Stores user input to ViewModel configuration
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number) // Note that this keyboard forces number inputs only
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.size_size1_solo_box_info),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        // Persistent column for current size
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Current color: $currentSize",
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                fontStyle = FontStyle.Italic
            )
        }
        ConfigurationCancelButton(onClick = { onCancelButtonClicked() })
        ConfigurationApplyButton(onClick = { viewModel.writeToRSS() }) // Writes configuration in ViewModel to RSS
    }
}

// Preview function used for debugging in Android Studio
@Preview
@Composable
fun SizeSortingScreenPreview() {
    //SizeSortingScreen()
}