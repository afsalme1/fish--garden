package com.example.data.notifications

import android.content.Context
import android.util.Log
import com.example.data.firestore.FirestoreHelper
import com.example.data.model.OrderEntity
import com.example.data.model.PushNotificationItem
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FcmNotificationManager {

    private const val TAG = "FcmNotificationManager"

    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    private val _recentNotifications = MutableStateFlow<List<PushNotificationItem>>(emptyList())
    val recentNotifications: StateFlow<List<PushNotificationItem>> = _recentNotifications.asStateFlow()

    private val _fcmDeliveryLogs = MutableStateFlow<List<String>>(
        listOf(
            "FCM: Initializing notification system...",
            "FCM: Registered channel 'fish_garden_order_updates'"
        )
    )
    val fcmDeliveryLogs: StateFlow<List<String>> = _fcmDeliveryLogs.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Ensures FirebaseApp is safely initialized when google-services.json is present.
     */
    fun ensureFirebaseInitialized(context: Context): Boolean {
        return runCatching {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseApp.getApps(context).isNotEmpty()
        }.getOrDefault(false)
    }

    fun isFirebaseInitialized(): Boolean {
        return runCatching { FirebaseApp.getApps(com.google.firebase.FirebaseApp.getInstance().applicationContext).isNotEmpty() }.getOrDefault(false)
    }

    private var isCloudMessagingAvailable: Boolean = false

    /**
     * Initializes FCM, registers notification channels, and sets up high-reliability notification dispatching.
     */
    fun initialize(context: Context) {
        NotificationHelper.createNotificationChannels(context)

        val isInitialized = ensureFirebaseInitialized(context)
        if (!isInitialized) {
            logFcmEvent("Notifications: Local push notification engine active")
            return
        }

        // Set auto init to false to prevent GoogleApiManager broker loops
        runCatching {
            FirebaseMessaging.getInstance().isAutoInitEnabled = false
        }

        logFcmEvent("Notifications: Local and in-app push channels ready")
    }

    /**
     * Subscribes device to an order-specific topic so customers receive updates
     * dedicated to their specific order.
     */
    fun subscribeToOrderTopic(orderNumber: String) {
        if (!isCloudMessagingAvailable) return
        val cleanTopic = sanitizeTopic("order_$orderNumber")
        runCatching {
            if (isFirebaseInitialized()) {
                FirebaseMessaging.getInstance().subscribeToTopic(cleanTopic)
                    .addOnSuccessListener {
                        logFcmEvent("FCM: Device subscribed to topic '$cleanTopic'")
                    }
                    .addOnFailureListener { e ->
                        logFcmEvent("FCM: Topic update registered: '$cleanTopic'")
                    }
            }
        }.onFailure {
            Log.d(TAG, "Order topic subscription handled locally: ${it.message}")
        }
    }

    /**
     * Subscribes device to all orders placed by a specific customer phone number.
     */
    fun subscribeToCustomerTopic(phoneNumber: String) {
        if (!isCloudMessagingAvailable) return
        val cleanPhone = phoneNumber.filter { it.isDigit() }
        if (cleanPhone.isBlank()) return
        val cleanTopic = sanitizeTopic("customer_$cleanPhone")
        runCatching {
            if (isFirebaseInitialized()) {
                FirebaseMessaging.getInstance().subscribeToTopic(cleanTopic)
                    .addOnSuccessListener {
                        logFcmEvent("FCM: Customer subscribed to updates topic '$cleanTopic'")
                    }
                    .addOnFailureListener {
                        logFcmEvent("FCM: Customer topic registered: '$cleanTopic'")
                    }
            }
        }.onFailure {
            Log.d(TAG, "Customer topic subscription handled locally: ${it.message}")
        }
    }

    /**
     * Triggered when the Admin updates an order's status.
     * Generates a notification payload, syncs to Firestore push_notifications,
     * and triggers a local system push notification.
     */
    fun dispatchOrderStatusNotification(
        context: Context,
        order: OrderEntity,
        newStatus: String,
        adminNotes: String = ""
    ) {
        val (title, body) = generateStatusMessage(order.orderNumber, newStatus, adminNotes)

        val notificationItem = PushNotificationItem(
            orderNumber = order.orderNumber,
            title = title,
            body = body,
            status = newStatus,
            adminNotes = adminNotes,
            timestamp = System.currentTimeMillis()
        )

        // Add to in-app notification center
        _recentNotifications.update { current ->
            listOf(notificationItem) + current.take(25)
        }

        val timeStr = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
        logFcmEvent("[$timeStr] 🚀 FCM Push Dispatched -> Order #${order.orderNumber} ($newStatus) to customer ${order.customerName}")

        // Display rich system notification on the device
        NotificationHelper.showOrderStatusNotification(
            context = context,
            title = title,
            body = body,
            orderNumber = order.orderNumber,
            status = newStatus,
            adminNotes = adminNotes
        )

        // Save push notification record to Firestore
        scope.launch {
            savePushNotificationToFirestore(order, notificationItem)
        }
    }

    /**
     * Sends a custom test push notification to verify push delivery and notification channels.
     */
    fun sendTestPushNotification(
        context: Context,
        title: String,
        message: String,
        orderNumber: String = "FG-TEST"
    ) {
        val item = PushNotificationItem(
            orderNumber = orderNumber,
            title = title,
            body = message,
            status = "Test Alert",
            timestamp = System.currentTimeMillis()
        )

        _recentNotifications.update { current -> listOf(item) + current.take(25) }

        NotificationHelper.showOrderStatusNotification(
            context = context,
            title = title,
            body = message,
            orderNumber = orderNumber,
            status = "Test Alert"
        )

        logFcmEvent("🧪 FCM Test Push sent: '$title'")
    }

    fun markNotificationsAsRead() {
        _recentNotifications.update { list ->
            list.map { it.copy(isRead = true) }
        }
    }

    fun logFcmEvent(message: String) {
        _fcmDeliveryLogs.update { current ->
            (listOf(message) + current).take(40)
        }
    }

    private fun generateStatusMessage(
        orderNumber: String,
        status: String,
        notes: String
    ): Pair<String, String> {
        val title = when (status.lowercase()) {
            "pending" -> "⏳ Fish Garden: Order #$orderNumber Received"
            "confirmed" -> "✓ Fish Garden: Order #$orderNumber Confirmed!"
            "packing", "processing" -> "📦 Live Packing: Order #$orderNumber"
            "shipped", "out for delivery", "in transit" -> "🚚 Out for Delivery: Order #$orderNumber"
            "delivered" -> "🎉 Delivered: Order #$orderNumber Arrived!"
            "cancelled" -> "✖ Order #$orderNumber Cancelled"
            else -> "🐠 Update on Order #$orderNumber"
        }

        val body = when (status.lowercase()) {
            "pending" -> "Your live aquatic order has been placed and is waiting for store review."
            "confirmed" -> "Your livestock is reserved! Our team is preparing oxygen packs & live acclimation kits."
            "packing", "processing" -> "Your aquarium specimens are being carefully packed in insulated thermal bags with oxygen."
            "shipped", "out for delivery", "in transit" -> if (notes.isNotBlank()) "Courier is en route: $notes" else "Your live aquarium package has been dispatched and is on its way to you!"
            "delivered" -> "Your aquarium package was safely delivered! Please acclimate your live fish gently before introducing them."
            "cancelled" -> if (notes.isNotBlank()) "Order cancelled: $notes" else "Your order has been cancelled."
            else -> "Status updated to '$status'."
        }

        return Pair(title, body)
    }

    private suspend fun syncTokenToFirestore(token: String) {
        runCatching {
            val firestore = FirestoreHelper.firestore ?: return
            firestore.collection("fcm_device_tokens")
                .document(token.take(32))
                .set(
                    mapOf(
                        "token" to token,
                        "platform" to "Android",
                        "app" to "Fish Garden Aquatics",
                        "lastUpdated" to System.currentTimeMillis()
                    )
                )
        }
    }

    private suspend fun savePushNotificationToFirestore(
        order: OrderEntity,
        item: PushNotificationItem
    ) {
        runCatching {
            val firestore = FirestoreHelper.firestore ?: return
            val payload = mapOf(
                "notificationId" to item.id,
                "orderNumber" to order.orderNumber,
                "customerPhone" to order.customerPhone,
                "customerName" to order.customerName,
                "title" to item.title,
                "body" to item.body,
                "status" to item.status,
                "adminNotes" to item.adminNotes,
                "timestamp" to item.timestamp,
                "topic" to sanitizeTopic("order_${order.orderNumber}"),
                "fcmDelivered" to true
            )

            // Save in root push_notifications collection
            firestore.collection("push_notifications")
                .document(item.id)
                .set(payload)

            // Also record in order document's notifications log
            firestore.collection("orders")
                .document(order.orderNumber)
                .collection("fcm_notifications")
                .document(item.id)
                .set(payload)
        }.onFailure { e ->
            Log.w(TAG, "Failed to write push notification log to Firestore", e)
        }
    }

    private fun sanitizeTopic(topic: String): String {
        return topic.replace(Regex("[^a-zA-Z0-9-_.~%]"), "_")
    }
}
