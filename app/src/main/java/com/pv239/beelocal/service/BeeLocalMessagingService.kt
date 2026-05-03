package com.pv239.beelocal.service

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pv239.beelocal.R

class BeeLocalMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)


        val title = remoteMessage.notification?.title ?: "🐝 New Daily Challenge!"
        val body = remoteMessage.notification?.body ?: "A new spot has been posted. Can you find it?"

        showForegroundNotification(title, body)
    }

    private fun showForegroundNotification(title: String, body: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            val channelId = "daily_challenge_channel"
            val randomId = System.currentTimeMillis().toInt()

            val builder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.baseline_photo_camera_24)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            NotificationManagerCompat.from(this).notify(randomId, builder.build())
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        println("New Firebase Token generated: $token")
    }

}