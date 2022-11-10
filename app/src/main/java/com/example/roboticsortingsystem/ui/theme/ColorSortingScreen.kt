package com.example.roboticsortingsystem.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roboticsortingsystem.R
import com.example.roboticsortingsystem.components.ConfigurationApplyButton
import com.example.roboticsortingsystem.components.ScreenTitle

// Creates column of radio buttons for color choice (should be called from inside a column)
@Composable
fun ColorButtons(
    modifier: Modifier = Modifier,
    onSelectionChanged: (String) -> Unit = {} // Notifies caller that a selection was made/changed
) {
    // List stores all possible color choices
    val colorChoices = listOf(
        stringResource(id = R.string.color_red),
        stringResource(id = R.string.color_orange),
        stringResource(id = R.string.color_yellow),
        stringResource(id = R.string.color_green),
        stringResource(id = R.string.color_purple),
        stringResource(id = R.string.color_brown),
    )

    // Stores value selected by user and preserves it across rebuilds
    var selectedButton by rememberSaveable { mutableStateOf("")}

    // Creates a row for each color
    // Adapted from Google Android Basics with Compose "Cupcake" example application
    colorChoices.forEach { item ->
        Row(
            modifier = Modifier.selectable(
                selected = selectedButton == item,
                onClick = {
                    selectedButton = item
                    onSelectionChanged(item)
                }
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selectedButton == item,
                onClick = {
                    selectedButton = item
                    onSelectionChanged(item)
                })
            Text(item)
        }
    }
}

@Composable
fun ColorSortingScreen(
    modifier: Modifier = Modifier
) {
    var color1Input by remember { mutableStateOf("") } // Stores user's chosen colors
    var color2Input by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()), // Allows vertical scrolling of column
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Screen title w/ 32dp spacer
        ScreenTitle(title = R.string.configuration_color_button)
        // Color 1 selection
        Text(
            text = stringResource(id = R.string.color_color1),
            textAlign = TextAlign.Center,
            fontSize = 18.sp
        )
        Divider(thickness = 1.dp, modifier = modifier.padding(top = 16.dp, bottom = 16.dp))
        ColorButtons()
        // Color 2 selection
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.color_color2),
            textAlign = TextAlign.Center,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(id = R.string.color_color2_info),
            textAlign = TextAlign.Center,
            fontSize = 10.sp
        )
        Divider(thickness = 1.dp, modifier = modifier.padding(top = 8.dp, bottom = 16.dp))
        ColorButtons()
        Spacer(modifier = Modifier.height(32.dp))
        ConfigurationApplyButton(onClick = { /*TODO*/ })
    }
}

// Preview function
@Preview
@Composable
fun ColorSortingScreenPreview() {
    ColorSortingScreen()
}