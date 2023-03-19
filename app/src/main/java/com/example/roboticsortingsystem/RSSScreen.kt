package com.example.roboticsortingsystem

import android.bluetooth.BluetoothAdapter
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.roboticsortingsystem.bluetooth.ConnectionState
import com.example.roboticsortingsystem.bluetooth.PermissionState
import com.example.roboticsortingsystem.bluetooth.SystemBroadcastReceiver
import com.example.roboticsortingsystem.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState


// Enum class holds names of screens in the app
enum class RSSScreen(@StringRes val title: Int) {
    Initial(title = R.string.initial_screen),
    Support(title = R.string.support_screen),
    MachineInfo(title = R.string.machine_info_screen),
    Configuration(title = R.string.configuration_screen),
    SizeSorting(title = R.string.size_screen),
    ColorSorting(title = R.string.color_screen),
    DefectSorting(title = R.string.defect_screen),
    BeltControl(title = R.string.belt_screen)
}

// Displays top bar and allows backward navigation where possible
@OptIn(ExperimentalMaterial3Api::class) // Many parts of Material 3 are still technically "experimental"
@Composable
fun RSSAppTopBar(
    currentScreen: RSSScreen, // Passes in currently displayed screen
    canNavigateBack: Boolean, // Determines whether to show a back button
    navigateBack: () -> Unit, // Provides "back" functionality
    modifier: Modifier = Modifier
) {
    CenterAlignedTopAppBar(
        title = { Text(
            stringResource(id = currentScreen.title),
            fontWeight = FontWeight.Bold
        ) },
        modifier = modifier,
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateBack) { // Show back button if and only if back navigation is allowed
                    Icon(
                        imageVector = Icons.Filled.ArrowBack, // Uses standard Android back icon
                        contentDescription = stringResource(id = R.string.common_back_button) // Adds content description for screen readers
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun RSSApp( // Controls navigation between screens
    modifier: Modifier = Modifier, // Good practice to pass a default modifier
    onBluetoothStateChanged: () -> Unit, // Allows interaction with Bluetooth in UI
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
    // Navigation controller
    // *************************************************

    val navController = rememberNavController() // Initializes NavController used to move between screens
    val backStackEntry by navController.currentBackStackEntryAsState() // Stores the previous screen to be passed to the nav bar (to determine if backwards navigation is possible)
    val currentScreen = RSSScreen.valueOf(
        backStackEntry?.destination?.route ?: RSSScreen.Initial.name // Provides a default value if backStackEntry is null
    )

    Scaffold(
        topBar = {
            RSSAppTopBar(
                currentScreen = currentScreen,
                canNavigateBack = navController.previousBackStackEntry != null, // Only show back button if there's a previous screen to go back to
                navigateBack = { navController.navigateUp() }) // navController function that goes back to the last screen
        }
    ) {
        NavHost( // Controls screen navigation for the whole app
            navController = navController,
            startDestination = RSSScreen.Initial.name, // Tells the NavHost to start at the initial screen
            modifier = Modifier.padding(it) // Padding value required for Material 3 deconfliction
        ) { // Call composables corresponding to each screen
            composable(route = RSSScreen.Initial.name) { // Calling the function within this composable allows navigation to/from the screen
                InitialScreen(
                    onSupportButtonClicked = {navController.navigate(RSSScreen.Support.name)}, // Calls the NavController to move to the support screen
                    onMachineInfoButtonClicked = {navController.navigate(RSSScreen.MachineInfo.name)},
                    onConfigurationButtonClicked = {navController.navigate(RSSScreen.Configuration.name)},
                    onBeltButtonClicked = {navController.navigate(RSSScreen.BeltControl.name)},
                    bleConnectionState = bleConnectionState // Controls loading screen during BLE connection
                )
            }
            composable(route = RSSScreen.Support.name) {
                SupportScreen()
            }
            composable(route = RSSScreen.MachineInfo.name) {
                MachineInfoScreen(modifier, onBluetoothStateChanged)
            }
            composable(route = RSSScreen.Configuration.name) {
                ConfigurationScreen(
                    onSizeButtonClicked = {navController.navigate(RSSScreen.SizeSorting.name)},
                    onColorButtonClicked = {navController.navigate(RSSScreen.ColorSorting.name)},
                    onDefectButtonClicked = {navController.navigate(RSSScreen.DefectSorting.name)},
                    bleConnectionState = bleConnectionState
                )
            }
            composable(route = RSSScreen.SizeSorting.name) {
                SizeSortingScreen(
                    onCancelButtonClicked = { returnToStart(navController) },
                    onBluetoothStateChanged = onBluetoothStateChanged,
                    bleConnectionState = bleConnectionState
                )
            }
            composable(route = RSSScreen.ColorSorting.name) {
                ColorSortingScreen(
                    onCancelButtonClicked = { returnToStart(navController) },
                    onBluetoothStateChanged = onBluetoothStateChanged,
                    bleConnectionState = bleConnectionState
                )
            }
            composable(route = RSSScreen.DefectSorting.name) {
                DefectSortingScreen(
                    onBluetoothStateChanged = { onBluetoothStateChanged },
                    bleConnectionState = bleConnectionState)
            }
            composable(route = RSSScreen.BeltControl.name) {
                BeltControlScreen(onBluetoothStateChanged = onBluetoothStateChanged)
            }
        }
    }
}

// When a "Cancel" button is pressed, this moves back to the initial screen
private fun returnToStart(
    navController: NavHostController
) {
    navController.popBackStack(RSSScreen.Initial.name, inclusive = false) // Inclusive = false: leaves the initial screen in the stack to display
}

// Preview function
@Preview
@Composable
fun RSSAppPreview() {
    // RSSApp()
}