package com.example.data.model

/**
 * Model representing an aquarium product or gallery showcase saved to User Favorites in Firestore.
 */
data class FavoriteItem(
    val id: String = "", // Firestore doc ID (e.g. "PRODUCT_1_5553429100")
    val userPhone: String = "",
    val itemId: Long = 0L,
    val itemType: String = "PRODUCT", // "PRODUCT" or "GALLERY"
    val title: String = "",
    val category: String = "",
    val price: Double? = null,
    val imageUrl: String = "",
    val description: String = "",
    val tag: String = "", // e.g. "Bestseller", "Rare", "Planted Aquascapes"
    val careLevel: String = "",
    val waterParameters: String = "",
    val tankSpecs: String = "",
    val floraFauna: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
