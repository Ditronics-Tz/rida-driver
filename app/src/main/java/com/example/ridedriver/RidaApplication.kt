package com.example.ridedriver

import android.app.Application
import com.example.ridedriver.data.notifications.PushNotifications
import com.example.ridedriver.data.remote.RidaApiClient

class RidaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RidaApiClient.init(this)
        PushNotifications.start(this)
    }
}
