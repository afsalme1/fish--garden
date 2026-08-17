package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val scientificName: String = "",
    val category: String, // "Fishes", "Plants", "Aquarium Tanks", "Food & Nutrition", "Filters & Gear", "Decor & Scaping"
    val price: Double,
    val originalPrice: Double? = null,
    val stockQuantity: Int,
    val description: String,
    val careLevel: String = "Moderate", // "Easy", "Moderate", "Expert"
    val waterParameters: String = "Temp: 24-28°C • pH: 6.5-7.5",
    val imageUrl: String,
    val badge: String = "", // "Hot", "Bestseller", "New", "Rare"
    val isAvailable: Boolean = true
)
