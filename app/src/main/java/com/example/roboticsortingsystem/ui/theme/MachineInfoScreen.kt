package com.example.roboticsortingsystem.ui.theme

import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.roboticsortingsystem.R

// Provides common formatting for all text in the diagnostic box
@Composable
fun DiagnosticText(
    @StringRes info: Int
) {
    Text(
        text = stringResource(id = info),
        textAlign = TextAlign.Left
    )
}

// Draws the screen that shows diagnostic information from the RSS
@Composable
fun MachineInfoScreen(
    modifier: Modifier = Modifier
) {
    /* TODO: add functionality that shows diagnostic info from the machine
    * Such as:
    * - Current color sensed
    * - Current weight sensed for each bin
    * - Current size sensed
    * */

    // This is a placeholder: actual info will go here during integration in 404
    // Nested columns ensure that arrangement works correctly
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .weight(1f)
                .border(width = 1.dp, color = Color.Green) // Basic theming to identify this as diagnostic info: will make more visually appealing later
        ) {
            // TODO: change to a fixed-width font in DiagnosticTest function
            // TODO: replace hard-coded sample data in strings with actual data
            DiagnosticText(info = R.string.machine_info_current_color)
            DiagnosticText(info = R.string.machine_info_current_size)
            DiagnosticText(info = R.string.machine_info_current_weight_bin_1)
            DiagnosticText(info = R.string.machine_info_current_weight_bin_2)
            DiagnosticText(info = R.string.machine_info_current_weight_bin_3)
        }
    }
}

@Preview
@Composable
fun MachineInfoScreenPreview() {
    MachineInfoScreen()
}