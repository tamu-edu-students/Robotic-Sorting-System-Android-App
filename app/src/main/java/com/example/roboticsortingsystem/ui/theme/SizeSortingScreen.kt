package com.example.roboticsortingsystem.ui.theme

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.roboticsortingsystem.R
import com.example.roboticsortingsystem.components.ConfigurationApplyButton
import com.example.roboticsortingsystem.components.ConfigurationCancelButton

@Composable
// Common framework for the two text boxes: simplifies code in SizeSortingScreen
fun InputBox(
    @StringRes label: Int, // Value displayed on text box
    entry: String,
    onValueChange: (String) -> Unit = {},
    keyboardOptions: KeyboardOptions // Used to pass in a style of keyboard
) {

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
    modifier: Modifier = Modifier,
    onCancelButtonClicked: () -> Unit = {} // Allows the main RSSScreen to pass in cancel button behavior
) {
    var size1Input by rememberSaveable { mutableStateOf("") } // Passed to InputBox to display user text
    var size2Input by rememberSaveable { mutableStateOf("") } // rememberSaveable: text stays when device rotated
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()), // Allows scrolling for smaller screens
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Size 1 input box
        InputBox(
            label = R.string.size_size1_box_label,
            entry = size1Input,
            onValueChange = { size1Input = it }, // Stores user inputs even as composable is recomposed on every input
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
            entry = size2Input,
            onValueChange = { size2Input = it },
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
        Spacer(modifier = Modifier.height(32.dp))
        ConfigurationCancelButton(onClick = { onCancelButtonClicked() })
        ConfigurationApplyButton(onClick = { /*TODO*/ }) // Call common configuration apply button
    }
}

// Preview function used for debugging in Android Studio
@Preview
@Composable
fun SizeSortingScreenPreview() {
    SizeSortingScreen()
}