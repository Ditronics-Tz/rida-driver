package com.example.ridedriver.data.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.ridedriver.MainActivity
import com.example.ridedriver.R
import com.example.ridedriver.data.local.SessionManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class RidaMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        // Application observes the authenticated login before uploading it.
        PushNotifications.tokenChanged()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val loggedIn = runCatching {
            runBlocking { SessionManager.observe(applicationContext).first().loggedIn }
        }.getOrDefault(false)
        if (!loggedIn) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) return
        val title = message.notification?.title ?: message.data["title"] ?: getString(R.string.app_name)
        val body = message.notification?.body ?: message.data["body"] ?: return
        val tag = message.data["notification_id"] ?: message.messageId ?: java.util.UUID.randomUUID().toString()
        // Open normal authenticated navigation and reload server state. Push
        // payloads never authorize ride actions or replace the current offer.
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(this, tag.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, PushNotifications.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            NotificationManagerCompat.from(this).notify(tag, 0, notification)
        } catch (_: SecurityException) {
            // Permission can be revoked between checking and posting.
        }
    }
}
