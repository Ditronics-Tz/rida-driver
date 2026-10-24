package com.example.ridedriver.data.remote

import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

/** Turns known API validation failures into short messages safe to show in the app. */
object RidaApiErrors {
    fun message(error: Throwable, fallback: String): String {
        if (error is IllegalArgumentException) return error.message ?: fallback
        if (error is IOException) return "Can't reach Rida right now. Check your connection and try again."

        val httpError = error as? HttpException ?: return fallback
        val responseText = runCatching { httpError.response()?.errorBody()?.string().orEmpty() }.getOrDefault("")
        val detail = runCatching {
            val json = JSONObject(responseText)
            sequenceOf("detail", "message", "error", "errors")
                .mapNotNull { key -> json.opt(key)?.toString()?.takeIf(String::isNotBlank) }
                .firstOrNull()
                .orEmpty()
        }.getOrDefault("")
        val text = detail.lowercase()
        return when {
            "account_role_mismatch" in responseText.lowercase() || "account type" in text -> "Use your driver account to sign in. Passenger accounts use the passenger app."
            "email" in text && ("already" in text || "exists" in text || "taken" in text) ||
                httpError.code() == 409 && "phone" !in text -> "An account already uses this email address. Sign in instead."
            "phone" in text && ("already" in text || httpError.code() == 409) -> "This phone number is already linked to another account."
            "phone" in text && ("invalid" in text || "valid" in text) -> "Enter a valid Tanzania phone number."
            "email" in text && ("invalid" in text || "valid" in text) -> "Enter a valid email address."
            "password" in text && ("weak" in text || "short" in text || "strong" in text) -> "Choose a stronger password."
            httpError.code() == 401 -> "Email or password is incorrect."
            httpError.code() == 403 -> "Your account can't sign in right now. Contact Rida Support."
            else -> fallback
        }
    }
}
