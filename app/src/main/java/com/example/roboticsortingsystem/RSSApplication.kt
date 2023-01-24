package com.example.roboticsortingsystem

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// This class (an extension of Application) tells Hilt where to inject dependencies
@HiltAndroidApp
class RSSApplication : Application() {
}