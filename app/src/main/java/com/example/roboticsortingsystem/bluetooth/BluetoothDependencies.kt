package com.example.roboticsortingsystem.bluetooth

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// This module allows Dagger to inject key Bluetooth components, such as the Bluetooth
// adapter, wherever they're needed.
@Module
@InstallIn(SingletonComponent::class)
object BluetoothDependencies {
    @Provides
    @Singleton
    fun provideBluetoothAdapter(@ApplicationContext context: Context):BluetoothAdapter {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return manager.adapter
    }

    // Provide ViewModel where needed
    @Provides
    @Singleton
    fun provideDataReadInterface(
        // Can't inject activity into a ViewModel. how else can this work?
        @ApplicationContext context: Context,
        bluetoothAdapter: BluetoothAdapter
    ):DataReadInterface {
        return DataReadWriteManager(bluetoothAdapter, context)
    }

}