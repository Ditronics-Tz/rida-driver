package com.example.ridedriver.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GoogleSignInPolicyTest {

    @Test
    fun serverSessionIsAccepted() {
        assertNull(GoogleSignInPolicy.rejection("access-token"))
    }

    @Test
    fun blankAccessTokenIsRejected() {
        assertEquals(
            "Rida did not return an access token",
            GoogleSignInPolicy.rejection(" ")
        )
    }
}
