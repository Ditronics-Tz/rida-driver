package com.example.ridedriver.data.local

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.ridedriver.data.remote.GoogleExchangeRequest
import com.example.ridedriver.data.remote.RidaApiClient
import com.example.ridedriver.data.remote.SessionResponse
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive

object GoogleAuthManager {

    const val WEB_CLIENT_ID = "173520268894-nhrolv9c3818n8o3r252i9sjudviq3rm.apps.googleusercontent.com"

    suspend fun signIn(context: Context): Result<SessionResponse> = authenticate(context)

    suspend fun register(context: Context): Result<SessionResponse> = authenticate(context)

    private suspend fun authenticate(context: Context): Result<SessionResponse> {
        return try {
            // A fresh server challenge is mandatory: its nonce is checked against the
            // signed Google ID token during the backend exchange.
            val challenge = RidaApiClient.api.googleChallenge(emptyMap())
            require(challenge.challengeId.isNotBlank() && challenge.nonce.isNotBlank()) {
                "The sign-in challenge was incomplete"
            }

            val googleOption = GetSignInWithGoogleOption.Builder(WEB_CLIENT_ID)
                .setNonce(challenge.nonce)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build()
            val activity = context.findActivity()
                ?: return Result.failure(IllegalStateException("Open this screen, then try Google again."))
            val response = CredentialManager.create(activity).getCredential(
                request = request,
                context = activity
            )

            val credential = response.credential as? CustomCredential
                ?: return Result.failure(IllegalStateException("Google returned an unsupported credential"))
            if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                return Result.failure(IllegalStateException("Google returned an unsupported credential"))
            }

            val googleCredential = try {
                GoogleIdTokenCredential.createFrom(credential.data)
            } catch (e: GoogleIdTokenParsingException) {
                Log.e(TAG, "Could not parse Google ID token credential", e)
                return Result.failure(e)
            }

            // Only the Rida session returned by this exchange can authenticate Rida API calls.
            val session = RidaApiClient.api.googleExchange(
                GoogleExchangeRequest(
                    idToken = googleCredential.idToken,
                    challengeId = challenge.challengeId
                )
            )
            GoogleSignInPolicy.rejection(session.getEffectiveAccessToken())?.let { reason ->
                return Result.failure(IllegalStateException(reason))
            }

            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            SessionManager.saveGoogleSession(context, session)
            Result.success(session)
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "User cancelled Google Sign-In")
            Result.failure(e)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential Manager could not complete Google Sign-In", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed before a Rida session was created", e)
            Result.failure(e)
        }
    }

    private const val TAG = "GoogleAuthManager"
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Accepts the session returned by POST /auth/google, including a passenger account. */
object GoogleSignInPolicy {
    fun rejection(accessToken: String): String? {
        if (accessToken.isBlank()) return "Rida did not return an access token"
        return null
    }
}
