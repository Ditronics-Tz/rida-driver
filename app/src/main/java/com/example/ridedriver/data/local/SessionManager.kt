package com.example.ridedriver.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ridedriver.data.remote.SessionResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.sessionStore by preferencesDataStore("session")

/**
 * Persistent session store (DataStore). Persists who is signed in so the app can
 * skip auth on relaunch and restore session seamlessly.
 */
object SessionManager {

    private val LOGGED_IN = booleanPreferencesKey("logged_in")
    private val PROFILE_COMPLETED = booleanPreferencesKey("profile_completed")
    private val USER_ID = stringPreferencesKey("user_id")
    private val NAME = stringPreferencesKey("name")
    private val HANDLE = stringPreferencesKey("handle")
    private val PHONE = stringPreferencesKey("phone")
    private val EMAIL = stringPreferencesKey("email")
    private val ACCESS_TOKEN = stringPreferencesKey("access_token")
    private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    private val PROFILE_PIC = stringPreferencesKey("profile_pic")
    private val PROFILE_PIC_VERSION = longPreferencesKey("profile_pic_version")

    data class Session(
        val loggedIn: Boolean = false,
        val userId: String = "",
        val name: String = "",
        val handle: String = "",
        val phone: String = "",
        val email: String = "",
        val profileCompleted: Boolean? = null,
        val accessToken: String? = null,
        val refreshToken: String? = null,
        val profilePictureUrl: String? = null,
        val profilePictureVersion: Long = 0
    ) {
        val isProfileComplete: Boolean
            get() = profileCompleted ?: (name.isNotBlank() && phone.isNotBlank())
    }

    fun observe(context: Context): Flow<Session> =
        context.sessionStore.data.map { prefs ->
            Session(
                loggedIn = prefs[LOGGED_IN] == true,
                userId = prefs[USER_ID].orEmpty(),
                name = prefs[NAME].orEmpty().takeUnless { it == "Rida Rider" }.orEmpty(),
                handle = prefs[HANDLE] ?: "",
                phone = prefs[PHONE] ?: "",
                email = prefs[EMAIL] ?: "",
                profileCompleted = prefs[PROFILE_COMPLETED],
                accessToken = prefs[ACCESS_TOKEN],
                refreshToken = prefs[REFRESH_TOKEN],
                profilePictureUrl = prefs[PROFILE_PIC],
                profilePictureVersion = prefs[PROFILE_PIC_VERSION] ?: 0
            )
        }

    suspend fun isLoggedIn(context: Context): Boolean =
        context.sessionStore.data.map { it[LOGGED_IN] == true }.first()

    suspend fun save(
        context: Context,
        name: String,
        handle: String,
        phone: String = "",
        email: String = "",
        profileCompleted: Boolean? = null,
        accessToken: String? = null,
        refreshToken: String? = null,
        profilePictureUrl: String? = null
    ) {
        context.sessionStore.edit { prefs ->
            prefs[LOGGED_IN] = true
            if (name.isBlank() || name.trim() == "Rida Rider") prefs.remove(NAME)
            else prefs[NAME] = name.trim()
            prefs[HANDLE] = handle
            if (phone.isNotBlank()) prefs[PHONE] = phone
            if (email.isNotBlank()) prefs[EMAIL] = email
            if (profileCompleted == null) prefs.remove(PROFILE_COMPLETED)
            else prefs[PROFILE_COMPLETED] = profileCompleted
            if (!accessToken.isNullOrBlank()) prefs[ACCESS_TOKEN] = accessToken
            if (!refreshToken.isNullOrBlank()) prefs[REFRESH_TOKEN] = refreshToken
            if (!profilePictureUrl.isNullOrBlank()) prefs[PROFILE_PIC] = profilePictureUrl
        }
    }

    suspend fun saveGoogleSession(
        context: Context,
        response: SessionResponse
    ) {
        com.example.ridedriver.data.remote.DriverAuthContract.validateSession(response)
        val user = response.user
        val name = user.fullName?.takeIf { it.isNotBlank() } ?: user.name.orEmpty()
        val email = user.email.orEmpty()
        val phone = user.phoneNumber?.takeIf { it.isNotBlank() } ?: user.phone.orEmpty()
        context.sessionStore.edit { prefs ->
            prefs[LOGGED_IN] = true
            prefs[USER_ID] = user.id
            if (name.isBlank()) prefs.remove(NAME) else prefs[NAME] = name.trim()
            if (email.isBlank()) {
                prefs.remove(HANDLE)
                prefs.remove(EMAIL)
            } else {
                prefs[HANDLE] = "@${email.substringBefore("@")}"
                prefs[EMAIL] = email
            }
            if (phone.isBlank()) prefs.remove(PHONE) else prefs[PHONE] = phone
            response.isProfileComplete()?.let { prefs[PROFILE_COMPLETED] = it }
                ?: prefs.remove(PROFILE_COMPLETED)
            prefs[ACCESS_TOKEN] = response.getEffectiveAccessToken()
            response.getEffectiveRefreshToken().takeIf { it.isNotBlank() }?.let {
                prefs[REFRESH_TOKEN] = it
            } ?: prefs.remove(REFRESH_TOKEN)
            // Load the account's server profile separately; never carry another account's photo.
            prefs.remove(PROFILE_PIC)
            prefs.remove(PROFILE_PIC_VERSION)
        }
    }

    suspend fun updateTokens(context: Context, newAccessToken: String, newRefreshToken: String? = null) {
        context.sessionStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = newAccessToken
            if (!newRefreshToken.isNullOrBlank()) {
                prefs[REFRESH_TOKEN] = newRefreshToken
            }
        }
    }

    suspend fun updateProfile(context: Context, name: String, phone: String, email: String) {
        context.sessionStore.edit { prefs ->
            if (name.isNotBlank()) prefs[NAME] = name.trim() else prefs.remove(NAME)
            if (phone.isNotBlank()) prefs[PHONE] = phone else prefs.remove(PHONE)
            if (email.isNotBlank()) {
                prefs[EMAIL] = email
                if (prefs[HANDLE].isNullOrBlank()) prefs[HANDLE] = email
            }
            prefs.remove(PROFILE_COMPLETED)
        }
    }

    suspend fun updateProfilePicture(context: Context, url: String, forceRefresh: Boolean = false) {
        context.sessionStore.edit { prefs ->
            if (prefs[PROFILE_PIC] != url || forceRefresh) prefs[PROFILE_PIC_VERSION] = System.currentTimeMillis()
            prefs[PROFILE_PIC] = url
        }
    }

    suspend fun clear(context: Context) {
        context.sessionStore.edit { it.clear() }
    }
}

/** Shared validation for email/password auth fields and non-auth phone forms. */
object AuthValidation {
    fun emailError(value: String): String? =
        com.example.ridedriver.ui.common.TzEmail.validate(value)?.let {
            if (value.isBlank()) "Enter your email address" else "Enter a valid email address"
        }

    fun phoneError(value: String): String? =
        if (com.example.ridedriver.ui.common.TzPhone.normalize(value) == null) {
            "Enter a valid Tanzania phone number"
        } else null

    fun passwordError(value: String): String? {
        if (value.isEmpty()) return "Enter your password"
        if (value.length < 6) return "Password must be at least 6 characters"
        return null
    }

}
