package com.example.roboticsortingsystem

import androidx.annotation.StringRes
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.roboticsortingsystem.ui.theme.*


// Enum class holds names of screens in the app
enum class RSSScreen(@StringRes val title: Int) {
    Initial(title = R.string.initial_screen),
    Support(title = R.string.support_screen),
    MachineInfo(title = R.string.machine_info_screen),
    Configuration(title = R.string.configuration_screen),
    SizeSorting(title = R.string.size_screen),
    ColorSorting(title = R.string.color_screen)
}

// TODO: Implement top bar

@Composable
fun RSSApp( // Controls navigation between screens
    modifier: Modifier = Modifier // Good practice to pass a default modifier
) {
    val navController = rememberNavController()

    Scaffold() {
        NavHost( // Controls screen navigation for the whole app
            navController = navController,
            startDestination = RSSScreen.Initial.name, // Tells the NavHost to start at the initial screen
            modifier = Modifier // Uses modifier passed into RSSApp
        ) { // Call composables corresponding to each screen
            composable(route = RSSScreen.Initial.name) { // Calling the function within this composable allows navigation to/from the screen
                InitialScreen(
                    onSupportButtonClicked = {navController.navigate(RSSScreen.Support.name)}, // Calls the NavController to move to the support screen
                    onMachineInfoButtonClicked = {navController.navigate(RSSScreen.MachineInfo.name)},
                    onConfigurationButtonClicked = {navController.navigate(RSSScreen.Configuration.name)}
                )
            }
            composable(route = RSSScreen.Support.name) {
                SupportScreen()
            }
            composable(route = RSSScreen.MachineInfo.name) {
                MachineInfoScreen()
            }
            composable(route = RSSScreen.Configuration.name) {
                ConfigurationScreen(
                    onSizeButtonClicked = {navController.navigate(RSSScreen.SizeSorting.name)},
                    onColorButtonClicked = {navController.navigate(RSSScreen.ColorSorting.name)}
                )
            }
            composable(route = RSSScreen.SizeSorting.name) {
                SizeSortingScreen()
            }
            composable(route = RSSScreen.ColorSorting.name) {
                ColorSortingScreen()
            }
        }
    }
}

// Preview function
@Preview
@Composable
fun RSSAppPreview() {
    RSSApp()
}