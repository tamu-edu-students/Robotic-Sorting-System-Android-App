package com.example.roboticsortingsystem.components

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roboticsortingsystem.R
import com.example.roboticsortingsystem.ui.theme.Shapes
import com.example.roboticsortingsystem.ui.theme.md_theme_light_onPrimary
import com.example.roboticsortingsystem.ui.theme.md_theme_light_primary

// This file contains common UI elements used in multiple screens to increase code readability.
// Provides a common framework for the screen selection buttons
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenSelectButton(
    @StringRes labelResourceId: Int, // Used to put text on the button
    @StringRes descripResourceId: Int,
    onClick: () -> Unit, // Used to determine click behavior
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = Shapes.large,
        modifier = Modifier
            .width(1200.dp)
            .height(IntrinsicSize.Min), // Sizes to the maximum size of the contents of the button
    ) { Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.fillMaxHeight()
    ) {
        Text(
            text = stringResource(id = labelResourceId),
            fontSize = 24.sp,
            textAlign = TextAlign.Left,
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .fillMaxWidth()
        )
        // Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = descripResourceId),
            fontSize = 18.sp,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Left,
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .fillMaxWidth()
        )
        }
    }
}

// Provides a common framework for the cancel button (that returns to the first screen) in the two sorting screens
@Composable
fun ConfigurationCancelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
   OutlinedButton(onClick = onClick) {
       Text(stringResource(id = R.string.common_cancel_button))
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

// Shows a loading screen that prevents the user from interacting with the app before a connection is made
@Composable
fun RSSLoadingScreen() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Connecting to Robotic Sorting System...",
            fontStyle = FontStyle.Italic
        )
    }
}