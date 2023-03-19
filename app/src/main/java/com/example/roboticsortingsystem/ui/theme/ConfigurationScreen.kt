package com.example.roboticsortingsystem.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.roboticsortingsystem.R
import com.example.roboticsortingsystem.bluetooth.ConnectionState
import com.example.roboticsortingsystem.components.ScreenSelectButton

// Creates screen to choose from size sorting or color sorting
@Composable
fun ConfigurationScreen(
    modifier: Modifier = Modifier,
    onSizeButtonClicked: () -> Unit = {},
    onColorButtonClicked: () -> Unit = {},
    onDefectButtonClicked: () -> Unit = {},
    bleConnectionState: ConnectionState
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ScreenSelectButton(labelResourceId = R.string.configuration_size_button, descripResourceId = R.string.size_description, onClick = { onSizeButtonClicked() })
        Spacer(modifier = Modifier.height(16.dp))
        ScreenSelectButton(labelResourceId = R.string.configuration_color_button, descripResourceId = R.string.color_description, onClick = { onColorButtonClicked() })
        Spacer(modifier = Modifier.height(16.dp))
        ScreenSelectButton(labelResourceId = R.string.configuration_defect_button, descripResourceId = R.string.defect_description, onClick = { onDefectButtonClicked() })
    }
}

// Preview function used for debugging in Android Studio
@Preview
@Composable
fun ConfigurationScreenPreview() {
    // ConfigurationScreen()
}