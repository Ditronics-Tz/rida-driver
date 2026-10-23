package com.example.ridedriver.data.driver

import com.example.ridedriver.BuildConfig

/**
 * Singleton provider for [DriverRepository].
 * Configurable via `BuildConfig.USE_MOCK_DRIVER_API`.
 */
object DriverRepositoryProvider {

    private val mockInstance: DriverRepository by lazy { MockDriverRepository() }
    private val remoteInstance: DriverRepository by lazy { RemoteDriverRepository() }

    fun get(): DriverRepository {
        return if (BuildConfig.USE_MOCK_DRIVER_API) mockInstance else remoteInstance
    }
}
