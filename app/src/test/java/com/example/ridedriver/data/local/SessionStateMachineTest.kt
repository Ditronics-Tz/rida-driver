package com.example.ridedriver.data.local

import org.junit.Assert.*
import org.junit.Test

class SessionStateMachineTest {
    @Test fun refreshCanOnlyWriteToItsOriginalLiveLogin() {
        assertFalse(SessionWriteGuard.canApply(false, "", "original"))
        assertFalse(SessionWriteGuard.canApply(true, "replacement", "original"))
        assertTrue(SessionWriteGuard.canApply(true, "original", "original"))
        assertTrue(SessionWriteGuard.canApply(false, "", null))
    }
    @Test fun registrationMatchesServerCharacterAndByteBounds() {
        assertNotNull(AuthValidation.registrationPasswordError("short123"))
        assertNull(AuthValidation.registrationPasswordError("LongEnough123!"))
        assertNotNull(AuthValidation.registrationPasswordError("a".repeat(73)))
        assertNotNull(AuthValidation.registrationPasswordError("😀".repeat(5)))
        assertNotNull(AuthValidation.registrationPasswordError("😀".repeat(20)))
    }
    @Test fun aNewLoginCannotInheritPreviousAccountFieldsOrCredentials() {
        val phone = androidx.datastore.preferences.core.stringPreferencesKey("phone")
        val photo = androidx.datastore.preferences.core.stringPreferencesKey("profile_pic")
        val token = androidx.datastore.preferences.core.stringPreferencesKey("refresh_token")
        val identity = androidx.datastore.preferences.core.stringPreferencesKey("session_id")
        val preferences = androidx.datastore.preferences.core.mutablePreferencesOf(
            phone to "+255700000000", photo to "https://example.test/old-photo", token to "old-refresh", identity to "old-login")
        SessionWriteGuard.prepareLogin(preferences, "new-login")
        assertNull(preferences[phone])
        assertNull(preferences[photo])
        assertNull(preferences[token])
        assertEquals("new-login", preferences[identity])
    }

}
