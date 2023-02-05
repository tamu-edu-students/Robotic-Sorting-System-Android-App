package com.example.roboticsortingsystem.ui.theme

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.util.Log
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.example.roboticsortingsystem.components.RSSLoadingScreen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch

// Specifies maximum size for sorting: anything above this will not be written to the ViewModel
const val RSS_MAX_SIZE = 100

@OptIn(ExperimentalMaterial3Api::class)
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
fun sizeIn(raw: ByteArray) : String {
    return if (raw.first().toInt() == 1) { // If the first byte is 1, the machine is currently configured for color
        "Cutoff 1: ${raw[1]} cm, Cutoff 2: ${raw[2]} cm" // Second byte is the sort value
    }
    else {
        "Currently configured for color"
    }
}

// Shows toasts (small popup messages) as necessary
fun showToast(
    context: Context,
    toastText: String
) {
    Toast.makeText(context, toastText, LENGTH_SHORT).show()
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


    // UI logic
    var size1Input by rememberSaveable { mutableStateOf("") } // Passed to InputBox to display user text. rememberSaveable: text stays when device rotated
    var size2Input by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current // Used to show toasts
    var currentSize = ""
    if (bleConnectionState == ConnectionState.Uninitialized) {
        currentSize = "Reading configuration value..."
    } else {
        currentSize = sizeIn(viewModel.configuration)
    }
    if (bleConnectionState == ConnectionState.Connected) {
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
                    if (it == "") { // Prevents crash when clearing the input field (due to trying to set a null config value in the ViewModel)
                        size1Input = "" // Shows an empty box
                        viewModel.configuration[0] = 1
                        viewModel.configuration[1] = 1
                        viewModel.configuration[2] = viewModel.configuration[2] // Necessary to keep the value of the second cutoff when just the first is changed
                        viewModel.configuration[3] = viewModel.configuration[3] // Same purpose for belt control bit
                    } else if (it.toInt() in 1 until RSS_MAX_SIZE) {
                        size1Input = it // Shows change to user
                        viewModel.configuration = byteArrayOf(1, it.toByte(), viewModel.configuration[2], viewModel.configuration[3]) // Stores user input to ViewModel configuration. 1 = sorting by size
                    } else {
                        size1Input = RSS_MAX_SIZE.toString()
                        viewModel.configuration[0] = 1 // Only change relevant bytes in the array
                        viewModel.configuration[1] = RSS_MAX_SIZE.toByte() // Corresponds to 1st sorting cutoff
                        viewModel.configuration[2] = viewModel.configuration[2]
                        viewModel.configuration[3] = viewModel.configuration[3]
                        showToast(context, "Size cutoff must be between 1 and $RSS_MAX_SIZE.")
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number) // Note that this keyboard forces number inputs only
                // TODO: why does hitting the enter button on this cause a crash? Maybe have it just go to the next box/send config...
            )


            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(id = R.string.size_size1_solo_box_info),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))


            // Size 2 input box
            InputBox(
                label = R.string.size_size2_box_label,
                entry = size2Input,
                onValueChange = {
                    if (it == "") {
                        size2Input = ""
                        viewModel.configuration[0] = 1
                        viewModel.configuration[1] = viewModel.configuration[1]
                        viewModel.configuration[2] = 1 // Index 2 corresponds to place in array for 2nd cutoff
                        viewModel.configuration[3] = viewModel.configuration[3]
                    } else if (it.toInt() in 0 until RSS_MAX_SIZE) { // 2nd value accepts 0
                        size2Input = it // Shows change to user
                        viewModel.configuration = byteArrayOf(1, viewModel.configuration[1], it.toByte(), viewModel.configuration[3]) // Stores user input to ViewModel configuration. 1 = sorting by size
                    } else {
                        size2Input = RSS_MAX_SIZE.toString()
                        viewModel.configuration[0] = 1
                        viewModel.configuration[1] = viewModel.configuration[1]
                        viewModel.configuration[2] = RSS_MAX_SIZE.toByte() // Corresponds to 2nd sorting cutoff
                        viewModel.configuration[3] = viewModel.configuration[3]
                        showToast(context, "Size cutoff must be between 0 and $RSS_MAX_SIZE.")
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number) // Note that this keyboard forces number inputs only
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = (stringResource(id = R.string.size_size2_default_info)),
                textAlign = TextAlign.Center
            )
            Text(
                text = (stringResource(id = R.string.size_size2_box_info)),
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
                    text = "Current size: $currentSize",
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    fontStyle = FontStyle.Italic
                )
            }
            ConfigurationCancelButton(onClick = { onCancelButtonClicked() })
            ConfigurationApplyButton(onClick = {
                if (((viewModel.configuration[2].toInt()) != 0) && (viewModel.configuration[2] <= viewModel.configuration[1])) {
                    showToast(context, "Sorting cutoff 1 must be less than sorting cutoff 2. Please choose again.")
                } else if (viewModel.configuration[1] in 1 until RSS_MAX_SIZE) { // Checks that value is inside acceptable bounds
                    viewModel.writeToRSS() // Writes configuration in ViewModel to RSS
                    showToast(context, "Size sorting configuration written to system.")
                } else { // Don't write value and notify the user
                    showToast(context, "One or more sorting cutoffs are out of range. Please choose again.")
                }
            })
        }
    } else { // Shows a loading screen that prevents the user from interacting with the configuration before connection
        RSSLoadingScreen()
    }
}


// Preview function used for debugging in Android Studio
@Preview
@Composable
fun SizeSortingScreenPreview() {
    //SizeSortingScreen()
}