package com.example.data.notifications

import android.util.Log
import com.example.data.model.PushNotificationItem
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FishGardenMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New Firebase Cloud Messaging Token received: $token")
        FcmNotificationManager.logFcmEvent("FCM Token Refreshed: ${token.take(16)}...")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message received from: ${remoteMessage.from}")

        // 1. Extract data payload
        val data = remoteMessage.data
        val orderNumber = data["orderNumber"] ?: data["order_number"] ?: "FG-ORDER"
        val status = data["status"] ?: data["orderStatus"] ?: "Updated"
        val adminNotes = data["adminNotes"] ?: data["notes"] ?: ""

        // 2. Extract notification payload or fallback to data
        val title = remoteMessage.notification?.title
            ?: data["title"]
            ?: "🐠 Fish Garden: Order #$orderNumber Updated"

        val body = remoteMessage.notification?.body
            ?: data["body"]
            ?: data["message"]
            ?: "Your order status is now '$status'."

        Log.d(TAG, "FCM Notification Payload -> Title: $title | Body: $body")

        // 3. Post Android System Notification
        NotificationHelper.showOrderStatusNotification(
            context = applicationContext,
            title = title,
            body = body,
            orderNumber = orderNumber,
            status = status,
            adminNotes = adminNotes
        )

        // 4. Log to FCM events
        FcmNotificationManager.logFcmEvent("FCM Incoming: [$orderNumber] '$title'")
    }

    companion object {
        private const val TAG = "FishGardenMessaging"
    }
}
