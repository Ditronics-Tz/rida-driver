package com.example.ridedriver.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.ridedriver.data.local.SessionManager
import com.example.ridedriver.data.remote.RidaApiClient
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object PushNotifications {
    const val CHANNEL_ID = "rida_updates"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refresh = MutableStateFlow(0L)

    fun tokenChanged() { refresh.update { it + 1 } }

    // Called once by Application, including when FCM starts the process.
    fun start(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Rida updates", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        scope.launch {
            combine(
                SessionManager.observe(context).map { if (it.loggedIn) it.sessionId else "" }.distinctUntilChanged(),
                refresh
            ) { sessionId, _ -> sessionId }.collectLatest { sessionId ->
                if (sessionId.isBlank()) {
                    context.getSystemService(NotificationManager::class.java).cancelAll()
                    return@collectLatest
                }
                var backoff = 5_000L
                while (true) {
                    try {
                        val token = FirebaseMessaging.getInstance().token.awaitResult()
                        val session = SessionManager.observe(context).first()
                        if (!session.loggedIn || session.sessionId != sessionId) return@collectLatest
                        val access = session.accessToken
                        if (access.isNullOrBlank()) return@collectLatest
                        // Bind to the captured login. The HTTP authenticator cannot
                        // replay this mutation under a different account.
                        val result = RidaApiClient.api.registerPushDevice("Bearer $access", mapOf("token" to token))
                        if (result.isSuccessful) return@collectLatest
                        if (result.code() == 409) {
                            // An offline sign-out may leave the old account's
                            // token registered. Obtain a fresh installation token.
                            FirebaseMessaging.getInstance().deleteToken().awaitResult()
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        // Retry provider/network/server failures without logging
                        // device tokens, credentials or response bodies.
                    }
                    delay(backoff)
                    backoff = (backoff * 2).coerceAtMost(300_000L)
                }
            }
        }
    }

    fun attach(activity: ComponentActivity) {
        val launcher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
        activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                tokenChanged() // Retry registration on every foreground resume.
                SessionManager.observe(activity).map { it.loggedIn }.distinctUntilChanged().collectLatest { loggedIn ->
                    if (loggedIn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        val preferences = activity.getSharedPreferences("push_permissions", Context.MODE_PRIVATE)
                        if (!preferences.getBoolean("requested", false)) {
                            preferences.edit().putBoolean("requested", true).apply()
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }
            }
        }
    }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
    addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}
