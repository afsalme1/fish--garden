package com.example.data.model

data class PushNotificationItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val orderNumber: String,
    val title: String,
    val body: String,
    val status: String,
    val adminNotes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
