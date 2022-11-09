package com.example.roboticsortingsystem.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.roboticsortingsystem.R
import com.example.roboticsortingsystem.components.ScreenSelectButton

// Creates screen to choose from size sorting or color sorting
@Composable
fun ConfigurationScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center // TODO: this doesn't seem to center the column vertically, why?
    ) {
        ScreenSelectButton(labelResourceId = R.string.configuration_size_button, onClick = { /*TODO*/ })
        Spacer(modifier = Modifier.height(16.dp))
        ScreenSelectButton(labelResourceId = R.string.configuration_color_button, onClick = { /*TODO*/ })
    }
}

// Preview function used for debugging in Android Studio
@Preview
@Composable
fun ConfigurationScreenPreview() {
    ConfigurationScreen()
}