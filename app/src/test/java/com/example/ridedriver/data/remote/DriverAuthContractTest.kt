package com.example.ridedriver.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Test

class DriverAuthContractTest {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @Test fun driverGoogleExchangeSendsRoleAndServerChallenge() {
        val json = moshi.adapter(GoogleExchangeRequest::class.java).toJson(GoogleExchangeRequest("google-token", "server-challenge"))
        assertTrue(json.contains("\"role\":\"driver\""))
        assertTrue(json.contains("\"challengeId\":\"server-challenge\""))
        assertFalse(json.contains("picture"))
    }

    @Test fun acceptsBackendSnakeCaseSessionAndRejectsPassengerOrMissingTokens() {
        val adapter = moshi.adapter(SessionResponse::class.java)
        val session = adapter.fromJson("""{"user":{"id":"driver-id","role":"driver"},"access_token":"rida-access","refresh_token":"rida-refresh"}""")!!
        DriverAuthContract.validateSession(session)
        for (invalid in listOf(session.copy(user = session.user.copy(role = "passenger")), session.copy(refreshTokenSnake = ""), session.copy(accessTokenSnake = ""))) {
            assertThrows(IllegalArgumentException::class.java) { DriverAuthContract.validateSession(invalid) }
        }
    }

    @Test fun registrationRespectsBackendPasswordBounds() {
        assertNotNull(DriverAuthContract.registrationPasswordError("short123"))
        assertNotNull(DriverAuthContract.registrationPasswordError("é".repeat(37)))
        assertNull(DriverAuthContract.registrationPasswordError("long-unique-driver-password!"))
    }
}
