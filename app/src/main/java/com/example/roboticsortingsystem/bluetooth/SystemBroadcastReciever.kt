package com.example.roboticsortingsystem.bluetooth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

// This composable provides an interface between the Bluetooth logic and Jetpack Compose.
@Composable
fun SystemBroadcastReceiver(
    systemAction:String,
    onSystemEvent:(intent: Intent?) -> Unit
) {
    val context = LocalContext.current
    val currentOnSystemEvent by rememberUpdatedState(onSystemEvent)

    DisposableEffect(context, systemAction){
        val intentFilter = IntentFilter(systemAction) // Filters system intents to only the one passed in via
        val broadcast = object : BroadcastReceiver(){ // Overrides system BroadcastReceiver() to execute on the current system event passed in
            override fun onReceive(p0: Context?, intent: Intent?) {
                currentOnSystemEvent(intent)
            }
        }

        context.registerReceiver(broadcast, intentFilter)

        onDispose { // Disposes of the receiver when it's done to free up the resource
            context.unregisterReceiver(broadcast)
        }

    }
}