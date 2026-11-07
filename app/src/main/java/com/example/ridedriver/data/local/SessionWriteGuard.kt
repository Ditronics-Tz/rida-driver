package com.example.ridedriver.data.local

/** Used inside the DataStore edit transaction: refreshes may never create or switch a login. */
internal object SessionWriteGuard {
    fun prepareLogin(preferences: androidx.datastore.preferences.core.MutablePreferences, sessionId: String) {
        preferences.clear()
        preferences[androidx.datastore.preferences.core.stringPreferencesKey("session_id")] = sessionId
    }

    fun canApply(loggedIn: Boolean, currentSessionId: String, expectedSessionId: String?): Boolean =
        expectedSessionId == null || (loggedIn && currentSessionId == expectedSessionId)
}
