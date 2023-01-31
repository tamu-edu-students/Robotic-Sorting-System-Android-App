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
import androidx.compose.ui.platform.LocalContext
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
import com.example.roboticsortingsystem.components.RSSLoadingScreen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

// Note: showToast function is defined in SizeSortingScreen.kt

// Creates column of radio buttons for color choice (should be called from inside a column)
@Composable
fun ColorButtons(
    modifier: Modifier = Modifier,
    allowsNone: Boolean,
    onSelectionChanged: (String) -> Unit = {} // Notifies caller that a selection was made/changed
) {

    // List stores all possible color choices
    val colorChoices = listOf(
        stringResource(id = R.string.color_red),
        stringResource(id = R.string.color_orange),
        stringResource(id = R.string.color_yellow),
        stringResource(id = R.string.color_green),
        stringResource(id = R.string.color_purple),
        stringResource(id = R.string.color_brown)
    )

    // Use list with "none" option if allowsNone = true
    val colorChoicesWithNone = listOf(
        stringResource(id = R.string.color_none),
        stringResource(id = R.string.color_red),
        stringResource(id = R.string.color_orange),
        stringResource(id = R.string.color_yellow),
        stringResource(id = R.string.color_green),
        stringResource(id = R.string.color_purple),
        stringResource(id = R.string.color_brown)
    )

    // Stores value selected by user and preserves it across rebuilds
    var selectedButton by rememberSaveable { mutableStateOf("") }

    // Creates a row for each color
    // Adapted from Google Android Basics with Compose "Cupcake" example application
    if (allowsNone) {
        colorChoicesWithNone.forEach { item ->
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
    } else {
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
}

// Translates the raw value received from the RSS to the current color
fun colorIn(raw: ByteArray) : String {
    // If the first digit is 0, the machine is currently configured for size
    var color1 = ""
    var color2 = ""
    if (raw.first().toInt() == 2) {
        color1 = when (raw[1].toInt()) { // The second digit corresponds to the first cutoff
            1 -> { "Red" }
            2 -> { "Orange"}
            3 -> { "Yellow"}
            4 -> { "Green"}
            5 -> { "Purple"}
            6 -> { "Brown"}
            else -> { "Unknown"}
        }
        color2 = when (raw[2].toInt()) {
            0 -> { "None" } // Only the second cutoff can have a 0/none value
            1 -> { "Red" }
            2 -> { "Orange"}
            3 -> { "Yellow"}
            4 -> { "Green"}
            5 -> { "Purple"}
            6 -> { "Brown"}
            else -> { "Unknown"}
        }
        return "Color 1: $color1, Color 2: $color2"
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
    var color1Input by rememberSaveable { mutableStateOf("") } // Stores user's chosen colors
    var color2Input by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current // Used to show toasts
    var currentColor = ""
    if (bleConnectionState == ConnectionState.Uninitialized) {
        currentColor = "Reading configuration value..."
    } else {
        currentColor = colorIn(viewModel.configuration)
    }

    if (bleConnectionState == ConnectionState.Connected) {
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
            ColorButtons(
                allowsNone = false, // "None" option only allowed on 2nd color
                onSelectionChanged = {
                    color1Input = it
                    when(it) { // Takes the number corresponding to the button the user selected and writes it to the ViewModel (with leading 2 for color configuration)
                    "Red" -> {
                        viewModel.configuration[0] = 2
                        viewModel.configuration[1] = 1
                        viewModel.configuration[2] = viewModel.configuration[2]
                        viewModel.configuration[3] = viewModel.configuration[3]
                    }
                    "Orange" -> {
                        viewModel.configuration[0] = 2
                        viewModel.configuration[1] = 2
                        viewModel.configuration[2] = viewModel.configuration[2]
                        viewModel.configuration[3] = viewModel.configuration[3]
                    }
                    "Yellow" -> {
                        viewModel.configuration[0] = 2
                        viewModel.configuration[1] = 3
                        viewModel.configuration[2] = viewModel.configuration[2]
                        viewModel.configuration[3] = viewModel.configuration[3]
                    }
                    "Green" -> {
                        viewModel.configuration[0] = 2
                        viewModel.configuration[1] = 4
                        viewModel.configuration[2] = viewModel.configuration[2]
                        viewModel.configuration[3] = viewModel.configuration[3]
                    }
                    "Purple" -> {
                        viewModel.configuration[0] = 2
                        viewModel.configuration[1] = 5
                        viewModel.configuration[2] = viewModel.configuration[2]
                        viewModel.configuration[3] = viewModel.configuration[3]
                    }
                    "Brown" -> {
                        viewModel.configuration[0] = 2
                        viewModel.configuration[1] = 6
                        viewModel.configuration[2] = viewModel.configuration[2]
                        viewModel.configuration[3] = viewModel.configuration[3]
                    }
                    else -> {} // Don't change anything if the returned color isn't valid
                }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            // Color 2 selection
            Text(
                text = stringResource(id = R.string.color_color2),
                textAlign = TextAlign.Center,
                fontSize = 18.sp
            )
            Divider(thickness = 1.dp, modifier = modifier.padding(top = 16.dp, bottom = 16.dp))
            ColorButtons(
                allowsNone = true,
                onSelectionChanged = {
                    color2Input = it
                    when(it) { // Takes the number corresponding to the button the user selected and writes it to the ViewModel (with leading 2 for color configuration)
                    "None (for 2-bin sorting)" -> {
                        viewModel.configuration[0] = 2
                        viewModel.configuration[1] = viewModel.configuration[1]
                        viewModel.configuration[2] = 0
                        viewModel.configuration[3] = viewModel.configuration[3]
                    }
                    "Red" -> {
                        viewModel.configuration[0] = 2
                        viewModel.configuration[1] = viewModel.configuration[1]
                        viewModel.configuration[2] = 1
                        viewModel.configuration[3] = viewModel.configuration[3]
                    }
                    "Orange" -> {
                        viewModel.configuration[0] = 2
                        viewModel.configuration[1] = viewModel.configuration[1]
                        viewModel.configuration[2] = 2
                        viewModel.configuration[3] = viewModel.configuration[3]
                    }
                    "Yellow" -> {
                        viewModel.configuration[0] = 2
                        viewModel.configuration[1] = viewModel.configuration[1]
                        viewModel.configuration[2] = 3
                        viewModel.configuration[3] = viewModel.configuration[3]
                    }
                    "Green" -> {
                        viewModel.configuration[0] = 2
                        viewModel.configuration[1] = viewModel.configuration[1]
                        viewModel.configuration[2] = 4
                        viewModel.configuration[3] = viewModel.configuration[3]
                    }
                    "Purple" -> {
                        viewModel.configuration[0] = 2
                        viewModel.configuration[1] = viewModel.configuration[1]
                        viewModel.configuration[2] = 5
                        viewModel.configuration[3] = viewModel.configuration[3]
                    }
                    "Brown" -> {
                        viewModel.configuration[0] = 2
                        viewModel.configuration[1] = viewModel.configuration[1]
                        viewModel.configuration[2] = 6
                        viewModel.configuration[3] = viewModel.configuration[3]
                    }
                    else -> {} // Don't change anything if the returned color isn't valid
                }
            })

            // Persistent column for current color
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Configuration currently on RSS: $currentColor", // Doesn't show live updates because of the way radio buttons work in Kotlin
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    fontStyle = FontStyle.Italic
                )
            }
            ConfigurationCancelButton(onClick = { onCancelButtonClicked() })
            ConfigurationApplyButton(onClick = {
                if (viewModel.configuration[1] == viewModel.configuration[2]) { // The colors can't be the same
                    showToast(context, "The sorting colors cannot be the same. Please choose again."
                    )
                } else {
                    viewModel.writeToRSS() // Tells the ViewModel to write to the RSS
                    showToast(context, "Color sorting configuration written to system.")
                }
            })
        }
    } else {
        RSSLoadingScreen()
    }
}

// Preview function
@Preview
@Composable
fun ColorSortingScreenPreview() {
    // ColorSortingScreen()
}