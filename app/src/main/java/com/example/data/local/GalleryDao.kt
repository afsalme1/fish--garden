package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.GalleryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface GalleryDao {
    @Query("SELECT * FROM gallery_items ORDER BY id DESC")
    fun getAllGalleryItems(): Flow<List<GalleryItem>>

    @Query("SELECT * FROM gallery_items WHERE id = :id LIMIT 1")
    suspend fun getGalleryItemById(id: Long): GalleryItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGalleryItem(item: GalleryItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<GalleryItem>)

    @Update
    suspend fun updateGalleryItem(item: GalleryItem)

    @Delete
    suspend fun deleteGalleryItem(item: GalleryItem)

    @Query("DELETE FROM gallery_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE gallery_items SET likesCount = :newCount, isUserLiked = :liked WHERE id = :id")
    suspend fun updateLike(id: Long, newCount: Int, liked: Boolean)

    @Query("SELECT COUNT(*) FROM gallery_items")
    suspend fun getCount(): Int
}
