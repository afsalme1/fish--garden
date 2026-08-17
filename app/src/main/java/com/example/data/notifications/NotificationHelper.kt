package com.example.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity

object NotificationHelper {

    private const val TAG = "NotificationHelper"
    const val CHANNEL_ID_ORDERS = "fish_garden_order_updates"
    const val CHANNEL_NAME_ORDERS = "Fish Garden Live Orders & Dispatch"
    const val CHANNEL_DESC_ORDERS = "Notifications for aquarium order confirmations, live packaging, courier shipping, and delivery updates."

    /**
     * Creates system notification channels required on Android 8.0+ (API 26+)
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (notificationManager != null) {
                val channel = NotificationChannel(
                    CHANNEL_ID_ORDERS,
                    CHANNEL_NAME_ORDERS,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESC_ORDERS
                    enableLights(true)
                    lightColor = Color.CYAN
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 250, 150, 250)
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "Notification channel '$CHANNEL_ID_ORDERS' initialized.")
            }
        }
    }

    /**
     * Displays a rich Android system notification when an order status updates.
     */
    fun showOrderStatusNotification(
        context: Context,
        title: String,
        body: String,
        orderNumber: String,
        status: String,
        adminNotes: String = ""
    ) {
        // Ensure notification channels exist
        createNotificationChannels(context)

        // Check POST_NOTIFICATIONS permission on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted. System notification cannot be displayed.")
                return
            }
        }

        // Intent to launch MainActivity with My Orders tab pre-selected
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_NAVIGATE_TAB", "MY_ORDERS")
            putExtra("EXTRA_ORDER_NUMBER", orderNumber)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            orderNumber.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val expandedText = buildString {
            append(body)
            if (adminNotes.isNotBlank()) {
                append("\n\n📍 Note: ").append(adminNotes)
            }
        }

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_ORDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(expandedText)
                    .setBigContentTitle(title)
                    .setSummaryText("Order #$orderNumber • $status")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setColor(0xFF00ACC1.toInt()) // Cyan brand color
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 250, 150, 250))
            .setContentIntent(pendingIntent)

        val notificationId = (orderNumber.hashCode() and 0x7FFFFFFF)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId, notificationBuilder.build())
            Log.d(TAG, "Notification successfully posted for Order #$orderNumber ($status)")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while displaying notification", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error posting notification for order #$orderNumber", e)
        }
    }
}
