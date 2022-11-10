package com.example.roboticsortingsystem.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.roboticsortingsystem.R
import com.example.roboticsortingsystem.components.ScreenTitle

@Composable fun ColorSortingScreen(
    modifier: Modifier = Modifier
) {
    var color1Input by remember { mutableStateOf("") } // Stores user's chosen colors
    var color2Input by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Screen title w/ 32dp spacer
        ScreenTitle(title = R.string.configuration_color_button)

    }
}

// Preview function
@Preview
@Composable
fun ColorSortingScreenPreview() {
    ColorSortingScreen()
}