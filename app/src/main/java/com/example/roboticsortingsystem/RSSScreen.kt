package com.example.roboticsortingsystem

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.roboticsortingsystem.ui.theme.*


// Enum class holds names of screens in the app
enum class RSSScreen(@StringRes val title: Int) {
    Initial(title = R.string.initial_screen),
    Support(title = R.string.support_screen),
    MachineInfo(title = R.string.machine_info_screen),
    Configuration(title = R.string.configuration_screen),
    SizeSorting(title = R.string.size_screen),
    ColorSorting(title = R.string.color_screen),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RSSApp( // Controls navigation between screens
    modifier: Modifier = Modifier, // Good practice to pass a default modifier
    onBluetoothStateChanged: () -> Unit // Allows interaction with Bluetooth in UI
) {
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
                    onBeltButtonClicked = {navController.navigate(RSSScreen.BeltControl.name)}
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
                    onColorButtonClicked = {navController.navigate(RSSScreen.ColorSorting.name)}
                )
            }
            composable(route = RSSScreen.SizeSorting.name) {
                SizeSortingScreen(
                    onCancelButtonClicked = { returnToStart(navController) },
                    onBluetoothStateChanged = onBluetoothStateChanged
                )
            }
            composable(route = RSSScreen.ColorSorting.name) {
                ColorSortingScreen(
                    onCancelButtonClicked = { returnToStart(navController) },
                    onBluetoothStateChanged = onBluetoothStateChanged
                )
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