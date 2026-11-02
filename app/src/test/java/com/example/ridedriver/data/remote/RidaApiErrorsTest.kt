package com.example.ridedriver.data.remote

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class RidaApiErrorsTest {

    @Test
    fun googleAccountLinkUsesTheBackendCode() {
        val error = httpError(
            409,
            """{"error":"Sign in to the existing account before linking Google","code":"ACCOUNT_LINK_REQUIRED"}"""
        )
        assertEquals(
            "This email already has a password account. Sign in with email. Google cannot link it yet.",
            RidaApiErrors.message(error, "fallback")
        )
    }

    @Test
    fun expiredGoogleChallengeIsDistinctFromABadPassword() {
        val error = httpError(
            401,
            """{"error":"Google challenge expired or already used","code":"GOOGLE_CHALLENGE_INVALID"}"""
        )
        assertEquals(
            "That Google sign-in expired. Try again.",
            RidaApiErrors.message(error, "fallback")
        )
    }

    private fun httpError(status: Int, json: String): HttpException {
        val body = json.toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Any>(status, body))
    }
}
