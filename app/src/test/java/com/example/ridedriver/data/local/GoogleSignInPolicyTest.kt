package com.example.ridedriver.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GoogleSignInPolicyTest {

    @Test
    fun driverSessionIsAccepted() {
        assertNull(GoogleSignInPolicy.rejection("driver", "access-token"))
    }

    @Test
    fun passengerSessionIsRejected() {
        val message = GoogleSignInPolicy.rejection("passenger", "access-token")
        assertEquals(
            "This Google account is a passenger account. Register as a driver with email, or sign in with your driver email and password.",
            message
        )
    }

    @Test
    fun missingRoleIsRejected() {
        assertEquals(
            "This Google account is a passenger account. Register as a driver with email, or sign in with your driver email and password.",
            GoogleSignInPolicy.rejection(null, "access-token")
        )
    }

    @Test
    fun blankAccessTokenIsRejectedBeforeRole() {
        assertEquals(
            "Rida did not return an access token",
            GoogleSignInPolicy.rejection("driver", " ")
        )
    }
}
