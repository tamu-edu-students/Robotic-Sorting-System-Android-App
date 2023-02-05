package com.example.roboticsortingsystem.ui.theme

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.roboticsortingsystem.R

// Provides common formatting for all text on this screen
@Composable
fun SupportText(
    @StringRes supportInfo: Int
) {
    Text(
        text = stringResource(id = supportInfo),
        textAlign = TextAlign.Justify
    )
}

// Draws the screen that shows support and project information
@Composable
fun SupportScreen (
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SupportText(supportInfo = R.string.support_project_info)
        Spacer(modifier = modifier.height(8.dp))
        SupportText(supportInfo = R.string.support_project_contact)
    }
}

// Preview function
@Preview
@Composable
fun SupportScreenPreview() {
    SupportScreen()
}