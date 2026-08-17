package com.example.data.repository

import com.example.data.firestore.FirestoreHelper
import com.example.data.model.FavoriteItem
import com.example.data.model.GalleryItem
import com.example.data.model.OrderEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface and implementation for Firebase Firestore integration,
 * responsible for managing live Fish Gallery data, real-time Order Tracking,
 * and Cloud Customer Favorites.
 */
interface IFirestoreAquariumRepository {
    // --- Fish Gallery Operations ---
    val galleryItemsFlow: Flow<List<GalleryItem>>
    suspend fun getGalleryItemById(itemId: Long): GalleryItem?
    suspend fun saveGalleryItem(item: GalleryItem): Result<String>
    suspend fun updateGalleryItem(item: GalleryItem): Result<Unit>
    suspend fun updateGalleryLikes(itemId: Long, likesCount: Int, isLiked: Boolean): Result<Unit>
    suspend fun deleteGalleryItem(itemId: Long): Result<Unit>

    // --- Order Tracking & Management Operations ---
    val allOrdersFlow: Flow<List<OrderEntity>>
    fun getCustomerOrdersFlow(customerPhone: String): Flow<List<OrderEntity>>
    fun trackOrderFlow(orderNumber: String): Flow<OrderEntity?>
    suspend fun getOrderByNumber(orderNumber: String): Result<OrderEntity?>
    suspend fun saveOrder(order: OrderEntity): Result<String>
    suspend fun updateOrderStatus(orderNumber: String, newStatus: String, adminNotes: String = ""): Result<Unit>
    suspend fun updateOrderStatusById(orderId: Long, newStatus: String, adminNotes: String = ""): Result<Unit>
    suspend fun deleteOrder(orderNumber: String): Result<Unit>
    suspend fun createTestOrder(): Result<String>

    // --- Customer Favorites Operations ---
    fun getFavoritesFlow(userPhone: String): Flow<List<FavoriteItem>>
    suspend fun saveFavorite(item: FavoriteItem): Result<String>
    suspend fun removeFavorite(userPhone: String, itemType: String, itemId: Long): Result<Unit>
    suspend fun removeFavoriteById(docId: String): Result<Unit>
    suspend fun clearAllFavorites(userPhone: String): Result<Unit>
    suspend fun fetchFavoritesByPhone(userPhone: String): Result<List<FavoriteItem>>
}

/**
 * Concrete implementation of [IFirestoreAquariumRepository] delegating to [FirestoreHelper].
 */
class FirestoreAquariumRepository(
    private val firestoreHelper: FirestoreHelper = FirestoreHelper
) : IFirestoreAquariumRepository {

    // =========================================================================
    // Fish Gallery Data Management
    // =========================================================================

    /**
     * Real-time stream of all public fish gallery aquascapes, biotope setups, and planted displays.
     */
    override val galleryItemsFlow: Flow<List<GalleryItem>> = firestoreHelper.getGalleryItemsFlow()

    /**
     * Retrieves a single gallery setup by its unique identifier.
     */
    override suspend fun getGalleryItemById(itemId: Long): GalleryItem? {
        return firestoreHelper.getGalleryItemById(itemId)
    }

    /**
     * Saves or publishes a new fish gallery entry to the Firestore 'gallery_items' collection.
     */
    override suspend fun saveGalleryItem(item: GalleryItem): Result<String> {
        return firestoreHelper.saveGalleryItem(item)
    }

    /**
     * Updates details (tank specs, flora/fauna, description) for an existing gallery item.
     */
    override suspend fun updateGalleryItem(item: GalleryItem): Result<Unit> {
        return firestoreHelper.updateGalleryItem(item)
    }

    /**
     * Updates like count and user like state for real-time community engagement.
     */
    override suspend fun updateGalleryLikes(
        itemId: Long,
        likesCount: Int,
        isLiked: Boolean
    ): Result<Unit> {
        return firestoreHelper.updateGalleryLikes(itemId, likesCount, isLiked)
    }

    /**
     * Deletes a gallery item document from Firestore.
     */
    override suspend fun deleteGalleryItem(itemId: Long): Result<Unit> {
        return firestoreHelper.deleteGalleryItem(itemId)
    }

    // =========================================================================
    // Real-Time Order Tracking & Management
    // =========================================================================

    /**
     * Real-time stream of all aquarium fish and gear orders for the administrator dashboard.
     */
    override val allOrdersFlow: Flow<List<OrderEntity>> = firestoreHelper.getAllOrdersFlow()

    /**
     * Real-time stream of orders placed by a specific customer phone number.
     */
    override fun getCustomerOrdersFlow(customerPhone: String): Flow<List<OrderEntity>> {
        return firestoreHelper.getOrdersByCustomerPhoneFlow(customerPhone)
    }

    /**
     * Real-time live status tracking stream for a single customer order.
     */
    override fun trackOrderFlow(orderNumber: String): Flow<OrderEntity?> {
        return firestoreHelper.trackOrderFlow(orderNumber)
    }

    /**
     * Fetches snapshot details of an order for tracking.
     */
    override suspend fun getOrderByNumber(orderNumber: String): Result<OrderEntity?> {
        return firestoreHelper.getOrder(orderNumber)
    }

    /**
     * Saves a new order into the Firestore 'orders' collection with live climate packaging details.
     */
    override suspend fun saveOrder(order: OrderEntity): Result<String> {
        return firestoreHelper.saveOrder(order)
    }

    /**
     * Updates tracking lifecycle status ("Pending", "Confirmed", "Preparing", "Dispatched", "Delivered")
     * and admin delivery notes for an order by its orderNumber.
     */
    override suspend fun updateOrderStatus(
        orderNumber: String,
        newStatus: String,
        adminNotes: String
    ): Result<Unit> {
        return firestoreHelper.updateOrderStatus(orderNumber, newStatus, adminNotes)
    }

    /**
     * Updates order status by local database ID.
     */
    override suspend fun updateOrderStatusById(
        orderId: Long,
        newStatus: String,
        adminNotes: String
    ): Result<Unit> {
        return firestoreHelper.updateOrderStatusById(orderId, newStatus, adminNotes)
    }

    /**
     * Removes an order from Firestore records.
     */
    override suspend fun deleteOrder(orderNumber: String): Result<Unit> {
        return firestoreHelper.deleteOrder(orderNumber)
    }

    /**
     * Creates a simulated real-time order in Firestore for testing tracking workflows.
     */
    override suspend fun createTestOrder(): Result<String> {
        return firestoreHelper.createTestOrderInFirestore()
    }

    // =========================================================================
    // Cloud Favorites Synchronization
    // =========================================================================

    /**
     * Real-time flow of customer wishlists and saved species.
     */
    override fun getFavoritesFlow(userPhone: String): Flow<List<FavoriteItem>> {
        return firestoreHelper.getFavoritesFlow(userPhone)
    }

    /**
     * Saves a favorite species or tank item to Firestore.
     */
    override suspend fun saveFavorite(item: FavoriteItem): Result<String> {
        return firestoreHelper.saveFavorite(item)
    }

    /**
     * Removes a specific favorite item by type and ID.
     */
    override suspend fun removeFavorite(
        userPhone: String,
        itemType: String,
        itemId: Long
    ): Result<Unit> {
        return firestoreHelper.removeFavorite(userPhone, itemType, itemId)
    }

    /**
     * Deletes a favorite document by its Firestore document ID.
     */
    override suspend fun removeFavoriteById(docId: String): Result<Unit> {
        return firestoreHelper.removeFavoriteById(docId)
    }

    /**
     * Clears all favorites for a user in Firestore.
     */
    override suspend fun clearAllFavorites(userPhone: String): Result<Unit> {
        return firestoreHelper.clearAllFavorites(userPhone)
    }

    /**
     * Performs a one-time fetch of user favorites from Firestore.
     */
    override suspend fun fetchFavoritesByPhone(userPhone: String): Result<List<FavoriteItem>> {
        return firestoreHelper.fetchFavoritesByPhone(userPhone)
    }
}
