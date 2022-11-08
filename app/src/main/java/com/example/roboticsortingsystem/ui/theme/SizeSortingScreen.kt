package com.example.roboticsortingsystem.ui.theme

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roboticsortingsystem.R

@Composable
// Common framework for the two text boxes: simplifies code in SizeSortingScreen
// TODO: make text entry work
fun InputBox(
    @StringRes label: Int, // Value displayed on text box
    onValueChange: (String) -> Unit = {},
    keyboardOptions: KeyboardOptions // Used to pass in a style of keyboard
) {
    val entry by remember { mutableStateOf("") } // Stores value input by user

    TextField(
        value = entry,
        onValueChange = onValueChange,
        label = { Text(stringResource(id = label)) },
        keyboardOptions = keyboardOptions // Sets the type of keyboard as passed in by the caller
    )
}

// Shows the screen used to pass size configurations to the RSS
@Composable
fun SizeSortingScreen (
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Screen title
        Text(
            text = stringResource(id = R.string.size_button), // Header is the same text as the button
            textAlign = TextAlign.Center,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))
        // Size 1 input box
        InputBox(
            label = R.string.size_size1_box_label,
            onValueChange = { /* TODO */ },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number) // Note that this keyboard forces number inputs only
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.size_size1_box_info),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        // Size 2 input box
        InputBox(
            label = R.string.size_size2_box_label,
            onValueChange = { /* TODO */ },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Informational text
        Text(
            text = stringResource(id = R.string.size_size2_default_info),
            textAlign = TextAlign.Center,
            fontStyle = FontStyle.Italic
        )
        Text(
            text = stringResource(id = R.string.size_size2_box_info),
            textAlign = TextAlign.Center
        )
    }
}

// Preview function used for debugging in Android Studio
@Preview
@Composable
fun SizeSortingScreenPreview() {
    SizeSortingScreen()
}