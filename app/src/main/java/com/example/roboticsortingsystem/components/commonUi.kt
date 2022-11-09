package com.example.roboticsortingsystem.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

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