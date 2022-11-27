package com.example.roboticsortingsystem.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.roboticsortingsystem.R
import com.example.roboticsortingsystem.components.ScreenSelectButton

@Composable
fun InitialScreen( // Creates the screen initially shown on launching the app
    modifier: Modifier = Modifier, // Passing a default modifier is good practice
    onSupportButtonClicked: () -> Unit = {}, // Handles navigation to the support screen when requested
    onMachineInfoButtonClicked: () -> Unit = {}, // Same for machine info screen
    onConfigurationButtonClicked: () -> Unit = {},
) {
    Column( // Places all of the buttons in a "column" object for easy alignment
        modifier = modifier
            .fillMaxWidth() // Makes buttons as wide as the screen: will be used for styling later
            .fillMaxHeight(), // Centers the buttons vertically
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ScreenSelectButton(labelResourceId = R.string.configuration_button, onClick = { onConfigurationButtonClicked() })
        Spacer(modifier = Modifier.height(16.dp))
        ScreenSelectButton(labelResourceId = R.string.machine_info_button, onClick = { onMachineInfoButtonClicked() })
        Spacer(modifier = Modifier.height(16.dp))
        ScreenSelectButton(labelResourceId = R.string.support_button, onClick = { onSupportButtonClicked() })
    }
}

// Preview function used for debugging in Android Studio
@Preview
@Composable
fun InitialScreenPreview() {
    InitialScreen()
}