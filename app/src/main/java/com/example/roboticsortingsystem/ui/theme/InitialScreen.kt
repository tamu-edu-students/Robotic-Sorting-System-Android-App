package com.example.roboticsortingsystem.ui.theme

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.roboticsortingsystem.R

@Composable
fun InitialScreen( // Creates the screen initially shown on launching the app
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        InitialScreenButton(labelResourceId = R.string.configuration_button, onClick = { /*TODO*/ })
        Spacer(modifier = Modifier.height(16.dp))
        InitialScreenButton(labelResourceId = R.string.machine_info_button, onClick = { /*TODO*/ })
        Spacer(modifier = Modifier.height(16.dp))
        InitialScreenButton(labelResourceId = R.string.support_button, onClick = { /*TODO*/ })
    }
}

// Provides a common framework for the three buttons on this screen
@Composable
fun InitialScreenButton(
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

// Function allowing preview
@Preview
@Composable
fun InitialScreenPreview() {
    InitialScreen()
}