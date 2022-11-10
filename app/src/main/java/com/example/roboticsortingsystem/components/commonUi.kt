package com.example.roboticsortingsystem.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.roboticsortingsystem.R

// This file contains common UI elements used in multiple screens to increase code readability.
// Provides a common framework for the screen selection buttons
@Composable
fun ScreenSelectButton(
    @StringRes labelResourceId: Int, // Used to put text on the button
    onClick: () -> Unit, // Used to determine click behavior
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier.fillMaxWidth()
    ) {
        Text(stringResource(id = labelResourceId))
    }
}

// Provides a common framework for configuration application buttons in the two sorting screens
@Composable
fun ConfigurationApplyButton(
    onClick: () -> Unit, // Will eventually start a process to send the configuration to the RSS
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick
    ) {
        Text(stringResource(id = R.string.common_send_button))
    }
}