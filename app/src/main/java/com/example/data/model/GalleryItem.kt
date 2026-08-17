package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gallery_items")
data class GalleryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String, // e.g. "Planted Aquascapes", "Exotic Fish", "Nano Tanks", "Marine Reef", "Hardscapes"
    val description: String,
    val imageUrl: String, // Resource name or URL/URI
    val tankSpecs: String, // e.g. "90cm Optiwhite • Fluval 407 • Chihiros WRGB II • CO2 Pressurized"
    val floraFauna: String = "", // e.g. "Cardinal Tetras, Otocinclus, Rotala H'Ra, Monte Carlo"
    val likesCount: Int = 12,
    val isUserLiked: Boolean = false,
    val dateAdded: String = "Aug 2026",
    val timestamp: Long = System.currentTimeMillis()
)
