package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customer_orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderNumber: String, // e.g., "FG-8921"
    val customerPhone: String,
    val customerName: String,
    val deliveryAddress: String,
    val itemsSummary: String, // e.g., "Neon Tetra x5, ADA Soil x1"
    val itemsJson: String, // Detailed JSON breakdown
    val subtotal: Double,
    val packingFee: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val totalAmount: Double,
    val paymentMethod: String = "Cash on Delivery (Live Fish Safe)",
    val orderStatus: String = "Pending", // "Pending", "Confirmed", "Packing", "Out for Delivery", "Delivered", "Cancelled"
    val adminNotes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val syncedToWebAdmin: Boolean = true
)
