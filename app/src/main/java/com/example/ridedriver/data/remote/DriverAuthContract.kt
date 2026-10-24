package com.example.ridedriver.data.remote

object DriverAuthContract {
    fun validateSession(response: SessionResponse) {
        require(response.user.role == "driver") { "Use a driver account to sign in to this app." }
        require(response.getEffectiveAccessToken().isNotBlank() && response.getEffectiveRefreshToken().isNotBlank()) {
            "The server returned an incomplete session. Try signing in again."
        }
    }
    fun registrationPasswordError(password: String): String? = when {
        password.length < 10 -> "Choose a password with at least 10 characters."
        password.toByteArray(Charsets.UTF_8).size > 72 -> "Password must be at most 72 bytes."
        else -> null
    }
}
