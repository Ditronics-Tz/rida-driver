package com.example.ridedriver.data.remote

import retrofit2.HttpException
import java.io.IOException

/** Turns known API validation failures into short messages safe to show in the app. */
object RidaApiErrors {
    fun message(error: Throwable, fallback: String): String {
        if (error is IOException) return "Can't reach Rida right now. Check your connection and try again."

        val httpError = error as? HttpException ?: return fallback
        val responseText = runCatching { httpError.response()?.errorBody()?.string().orEmpty() }.getOrDefault("")
        when (jsonString(responseText, "code")) {
            "ACCOUNT_LINK_REQUIRED" ->
                return "This email already has a password account. Sign in with email. Google cannot link it yet."
            "GOOGLE_CHALLENGE_INVALID" ->
                return "That Google sign-in expired. Try again."
            "GOOGLE_TOKEN_INVALID" ->
                return "Google could not verify this sign-in. Try again."
            "GOOGLE_NOT_CONFIGURED" ->
                return "Google sign-in is not available right now."
        }
        val detail = sequenceOf("detail", "message", "error", "errors")
            .firstNotNullOfOrNull { key -> jsonString(responseText, key).takeIf(String::isNotBlank) }
            .orEmpty()
        val text = detail.lowercase()
        return when {
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

    private fun jsonString(body: String, key: String): String {
        val match = Regex(""""$key"\s*:\s*"((?:\\.|[^"\\])*)"""").find(body) ?: return ""
        return match.groupValues[1]
    }
}
