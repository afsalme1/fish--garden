package com.example.data.firestore

import android.util.Log
import com.example.data.model.FavoriteItem
import com.example.data.model.GalleryItem
import com.example.data.model.OrderEntity
import com.example.data.model.ProductItem
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Singleton helper object to manage read and write operations on Firebase Firestore
 * for Aquarium Gallery items, Product Inventory, and User Orders.
 */
object FirestoreHelper {

    private const val TAG = "FirestoreHelper"

    const val COLLECTION_GALLERY = "gallery_items"
    const val COLLECTION_PRODUCTS = "products"
    const val COLLECTION_ORDERS = "orders"
    const val COLLECTION_FAVORITES = "favorites"

    // Safely retrieve Firestore instance without crashing if FirebaseApp is not configured
    val firestore: FirebaseFirestore?
        get() = runCatching {
            if (!runCatching { com.google.firebase.FirebaseApp.getInstance() != null }.getOrDefault(false)) {
                null
            } else {
                FirebaseFirestore.getInstance()
            }
        }.onFailure {
            Log.w(TAG, "FirebaseFirestore instance unavailable (running in local offline mode): ${it.message}")
        }.getOrNull()

    private fun getGalleryCollection() = firestore?.collection(COLLECTION_GALLERY)
    private fun getProductsCollection() = firestore?.collection(COLLECTION_PRODUCTS)
    private fun getOrdersCollection() = firestore?.collection(COLLECTION_ORDERS)
    private fun getFavoritesCollection() = firestore?.collection(COLLECTION_FAVORITES)

    // =====================================================================
    // GALLERY CRUD OPERATIONS
    // =====================================================================

    /**
     * Saves or updates a gallery item in Firestore.
     * Uses the gallery item's ID as the document ID if > 0, otherwise auto-generates.
     */
    suspend fun saveGalleryItem(item: GalleryItem): Result<String> = runCatching {
        val col = getGalleryCollection() ?: return Result.failure(IllegalStateException("Firestore offline"))
        val docRef = if (item.id > 0) {
            col.document(item.id.toString())
        } else {
            col.document()
        }

        val data = galleryItemToMap(item)
        docRef.set(data, SetOptions.merge()).awaitTask()
        Log.d(TAG, "Gallery item saved successfully: ${item.title} (ID: ${docRef.id})")
        docRef.id
    }

    /**
     * Updates an existing gallery item.
     */
    suspend fun updateGalleryItem(item: GalleryItem): Result<Unit> = runCatching {
        val col = getGalleryCollection() ?: return Result.failure(IllegalStateException("Firestore offline"))
        val docId = item.id.toString()
        val data = galleryItemToMap(item)
        col.document(docId).set(data, SetOptions.merge()).awaitTask()
        Log.d(TAG, "Gallery item updated: ${item.title} (ID: $docId)")
    }

    /**
     * Updates like count and user like status for a gallery item.
     */
    suspend fun updateGalleryLikes(itemId: Long, likesCount: Int, isLiked: Boolean): Result<Unit> = runCatching {
        val col = getGalleryCollection() ?: return Result.failure(IllegalStateException("Firestore offline"))
        val docId = itemId.toString()
        col.document(docId).update(
            mapOf(
                "likesCount" to likesCount,
                "isUserLiked" to isLiked
            )
        ).awaitTask()
        Log.d(TAG, "Gallery likes updated for item #$itemId -> $likesCount")
    }

    /**
     * Deletes a gallery item from Firestore by its ID.
     */
    suspend fun deleteGalleryItem(itemId: Long): Result<Unit> = runCatching {
        val col = getGalleryCollection() ?: return Result.failure(IllegalStateException("Firestore offline"))
        col.document(itemId.toString()).delete().awaitTask()
        Log.d(TAG, "Gallery item deleted: #$itemId")
    }

    /**
     * Real-time flow of all gallery items ordered by timestamp descending.
     */
    fun getGalleryItemsFlow(): Flow<List<GalleryItem>> = callbackFlow {
        val col = getGalleryCollection()
        if (col == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val listenerRegistration = runCatching {
            col.orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to gallery items", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val items = snapshot.documents.mapNotNull { doc ->
                            mapDocumentToGalleryItem(doc)
                        }
                        trySend(items)
                    }
                }
        }.getOrNull()

        awaitClose {
            listenerRegistration?.remove()
        }
    }

    /**
     * Fetch a single gallery item by its ID.
     */
    suspend fun getGalleryItemById(itemId: Long): GalleryItem? = runCatching {
        val col = getGalleryCollection() ?: return@runCatching null
        val snapshot = col.document(itemId.toString()).get().awaitTask()
        if (snapshot.exists()) mapDocumentToGalleryItem(snapshot) else null
    }.getOrNull()

    // =====================================================================
    // PRODUCT INVENTORY CRUD OPERATIONS (FIRESTORE)
    // =====================================================================

    /**
     * Saves or updates a product in the Firestore products collection.
     */
    suspend fun saveProduct(product: ProductItem): Result<String> = runCatching {
        val col = getProductsCollection() ?: return Result.failure(IllegalStateException("Firestore offline"))
        val docRef = if (product.id > 0) {
            col.document(product.id.toString())
        } else {
            col.document()
        }

        val data = productItemToMap(product)
        docRef.set(data, SetOptions.merge()).awaitTask()
        Log.d(TAG, "Product saved successfully to Firestore: ${product.name} (ID: ${docRef.id})")
        docRef.id
    }

    /**
     * Updates an existing product in Firestore.
     */
    suspend fun updateProduct(product: ProductItem): Result<Unit> = runCatching {
        val col = getProductsCollection() ?: return Result.failure(IllegalStateException("Firestore offline"))
        val docId = product.id.toString()
        val data = productItemToMap(product)
        col.document(docId).set(data, SetOptions.merge()).awaitTask()
        Log.d(TAG, "Product updated in Firestore: ${product.name} (ID: $docId)")
    }

    /**
     * Updates only the stock quantity for a product in Firestore.
     */
    suspend fun updateProductStock(productId: Long, newStock: Int): Result<Unit> = runCatching {
        val col = getProductsCollection() ?: return Result.failure(IllegalStateException("Firestore offline"))
        col.document(productId.toString()).update(
            mapOf(
                "stockQuantity" to newStock,
                "isAvailable" to (newStock > 0)
            )
        ).awaitTask()
        Log.d(TAG, "Product stock updated in Firestore: ID #$productId -> $newStock units")
    }

    /**
     * Deletes a product from Firestore by ID.
     */
    suspend fun deleteProduct(productId: Long): Result<Unit> = runCatching {
        val col = getProductsCollection() ?: return Result.failure(IllegalStateException("Firestore offline"))
        col.document(productId.toString()).delete().awaitTask()
        Log.d(TAG, "Product deleted from Firestore: ID #$productId")
    }

    /**
     * Real-time flow of all products from Firestore collection.
     */
    fun getProductsFlow(): Flow<List<ProductItem>> = callbackFlow {
        val col = getProductsCollection()
        if (col == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val listenerRegistration = runCatching {
            col.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to products collection in Firestore", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val products = snapshot.documents.mapNotNull { doc ->
                        mapDocumentToProductItem(doc)
                    }
                    trySend(products)
                }
            }
        }.getOrNull()

        awaitClose {
            listenerRegistration?.remove()
        }
    }

    /**
     * One-shot fetch of products from Firestore.
     */
    suspend fun fetchProducts(): Result<List<ProductItem>> = runCatching {
        val col = getProductsCollection() ?: return Result.success(emptyList())
        val snapshot = col.get().awaitTask()
        snapshot.documents.mapNotNull { doc -> mapDocumentToProductItem(doc) }
    }

    // =====================================================================
    // USER ORDERS CRUD OPERATIONS
    // =====================================================================

    /**
     * Saves a customer order into Firestore.
     * Uses orderNumber or database ID as document key.
     */
    suspend fun saveOrder(order: OrderEntity): Result<String> = runCatching {
        val col = getOrdersCollection() ?: return Result.failure(IllegalStateException("Firestore offline"))
        val docRef = if (order.orderNumber.isNotBlank()) {
            col.document(order.orderNumber)
        } else if (order.id > 0) {
            col.document(order.id.toString())
        } else {
            col.document()
        }

        val data = orderEntityToMap(order)
        docRef.set(data, SetOptions.merge()).awaitTask()
        Log.d(TAG, "Order saved to Firestore: ${order.orderNumber} (Total: $${order.totalAmount})")
        docRef.id
    }

    /**
     * Updates status and admin notes for a specific order by orderNumber.
     */
    suspend fun updateOrderStatus(
        orderNumber: String,
        newStatus: String,
        adminNotes: String = ""
    ): Result<Unit> = runCatching {
        val col = getOrdersCollection() ?: return Result.failure(IllegalStateException("Firestore offline"))
        val updates = mutableMapOf<String, Any>(
            "orderStatus" to newStatus,
            "syncedToWebAdmin" to true
        )
        if (adminNotes.isNotBlank()) {
            updates["adminNotes"] = adminNotes
        }

        col.document(orderNumber).update(updates).awaitTask()
        Log.d(TAG, "Order #$orderNumber status updated to $newStatus")
    }

    /**
     * Updates status and admin notes for a specific order by ID.
     */
    suspend fun updateOrderStatusById(
        orderId: Long,
        newStatus: String,
        adminNotes: String = ""
    ): Result<Unit> = runCatching {
        val col = getOrdersCollection() ?: return Result.failure(IllegalStateException("Firestore offline"))
        val querySnapshot = col.whereEqualTo("id", orderId).get().awaitTask()
        if (!querySnapshot.isEmpty) {
            for (doc in querySnapshot.documents) {
                val updates = mutableMapOf<String, Any>(
                    "orderStatus" to newStatus,
                    "syncedToWebAdmin" to true
                )
                if (adminNotes.isNotBlank()) {
                    updates["adminNotes"] = adminNotes
                }
                doc.reference.update(updates).awaitTask()
            }
        } else {
            col.document(orderId.toString()).update(
                mapOf(
                    "orderStatus" to newStatus,
                    "adminNotes" to adminNotes
                )
            ).awaitTask()
        }
        Log.d(TAG, "Order #$orderId status updated to $newStatus in Firestore")
    }

    /**
     * Deletes an order from Firestore by orderNumber or ID.
     */
    suspend fun deleteOrder(orderNumber: String): Result<Unit> = runCatching {
        val col = getOrdersCollection() ?: return Result.failure(IllegalStateException("Firestore offline"))
        col.document(orderNumber).delete().awaitTask()
        Log.d(TAG, "Order #$orderNumber deleted from Firestore")
    }

    /**
     * Real-time flow of all orders (for Admin Dashboard) listening directly to 'orders' collection in Firestore.
     */
    fun getAllOrdersFlow(): Flow<List<OrderEntity>> = callbackFlow {
        val col = getOrdersCollection()
        if (col == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val listenerRegistration = runCatching {
            col.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to 'orders' collection in Firestore", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val orders = snapshot.documents.mapNotNull { doc ->
                        mapDocumentToOrderEntity(doc)
                    }.sortedByDescending { it.timestamp }
                    trySend(orders)
                }
            }
        }.getOrNull()

        awaitClose {
            listenerRegistration?.remove()
        }
    }

    /**
     * Creates a new incoming test order directly in the Firestore 'orders' collection.
     */
    suspend fun createTestOrderInFirestore(): Result<String> = runCatching {
        val randomId = kotlin.random.Random.nextInt(1000, 9999)
        val orderNum = "FG-$randomId"
        val testOrder = OrderEntity(
            id = 0L,
            orderNumber = orderNum,
            customerName = listOf("Emma Watson", "David Miller", "Sophia Zhang", "Liam Rodriguez").random(),
            customerPhone = "+1 (555) ${kotlin.random.Random.nextInt(100, 999)}-${kotlin.random.Random.nextInt(1000, 9999)}",
            deliveryAddress = "${kotlin.random.Random.nextInt(100, 999)} Ocean Breeze Ave, Aqua Bay",
            itemsSummary = listOf("Crown Tail Betta (x1), Anubias Nana (x2)", "Discus Symphysodon (x2), Driftwood (x1)", "Cardinal Tetra School (x10), Eco-Gravel (x1)").random(),
            itemsJson = "[{\"name\":\"Live Aquarium Specimen\",\"price\":45.00,\"quantity\":1,\"category\":\"Live Fish\"}]",
            subtotal = 45.00,
            packingFee = 2.50,
            deliveryFee = 4.99,
            totalAmount = 52.49,
            paymentMethod = "Cash on Delivery (Live Fish Safe)",
            orderStatus = "Pending",
            adminNotes = "Keep insulated with thermal pack. Call 10 mins prior to arrival.",
            timestamp = System.currentTimeMillis(),
            syncedToWebAdmin = true
        )
        saveOrder(testOrder).getOrThrow()
    }

    /**
     * Real-time flow of customer orders filtered by customer phone number from Firestore.
     * Matches both exact phone string and sanitized numeric digits so varying formatting works reliably.
     */
    fun getOrdersByCustomerPhoneFlow(phone: String): Flow<List<OrderEntity>> = callbackFlow {
        val col = getOrdersCollection()
        if (col == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val cleanTargetPhone = phone.filter { it.isDigit() }
        
        val listenerRegistration = runCatching {
            col.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to customer orders for $phone", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val orders = snapshot.documents
                        .mapNotNull { doc -> mapDocumentToOrderEntity(doc) }
                        .filter { order ->
                            if (phone.isBlank()) return@filter false
                            val orderPhoneClean = order.customerPhone.filter { it.isDigit() }
                            order.customerPhone.equals(phone, ignoreCase = true) ||
                                    (cleanTargetPhone.isNotBlank() && (orderPhoneClean == cleanTargetPhone ||
                                            orderPhoneClean.endsWith(cleanTargetPhone) ||
                                            cleanTargetPhone.endsWith(orderPhoneClean)))
                        }
                        .sortedByDescending { it.timestamp }
                    trySend(orders)
                }
            }
        }.getOrNull()

        awaitClose {
            listenerRegistration?.remove()
        }
    }

    /**
     * Direct one-shot fetch of customer previous orders from Firestore by phone number.
     */
    suspend fun fetchOrdersByPhone(phone: String): Result<List<OrderEntity>> = runCatching {
        val col = getOrdersCollection() ?: return Result.success(emptyList())
        val cleanTargetPhone = phone.filter { it.isDigit() }
        val snapshot = col.get().awaitTask()
        snapshot.documents
            .mapNotNull { doc -> mapDocumentToOrderEntity(doc) }
            .filter { order ->
                if (phone.isBlank()) return@filter false
                val orderPhoneClean = order.customerPhone.filter { it.isDigit() }
                order.customerPhone.equals(phone, ignoreCase = true) ||
                        (cleanTargetPhone.isNotBlank() && (orderPhoneClean == cleanTargetPhone ||
                                orderPhoneClean.endsWith(cleanTargetPhone) ||
                                cleanTargetPhone.endsWith(orderPhoneClean)))
            }
            .sortedByDescending { it.timestamp }
    }

    /**
     * One-shot fetch of a single order by its orderNumber or ID from Firestore.
     */
    suspend fun getOrder(orderNumber: String): Result<OrderEntity?> = runCatching {
        val col = getOrdersCollection() ?: return Result.success(null)
        val doc = col.document(orderNumber).get().awaitTask()
        if (doc.exists()) {
            mapDocumentToOrderEntity(doc)
        } else {
            val querySnapshot = col.whereEqualTo("orderNumber", orderNumber).get().awaitTask()
            if (!querySnapshot.isEmpty) {
                mapDocumentToOrderEntity(querySnapshot.documents.first())
            } else {
                null
            }
        }
    }

    /**
     * Real-time live status tracking flow for a single order by orderNumber.
     */
    fun trackOrderFlow(orderNumber: String): Flow<OrderEntity?> = callbackFlow {
        val col = getOrdersCollection()
        if (col == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }

        val listenerRegistration = runCatching {
            col.document(orderNumber).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error tracking order $orderNumber", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    trySend(mapDocumentToOrderEntity(snapshot))
                } else {
                    // Fallback to searching whereEqualTo("orderNumber", orderNumber)
                    col.whereEqualTo("orderNumber", orderNumber)
                        .addSnapshotListener { querySnapshot, queryError ->
                            if (queryError == null && querySnapshot != null && !querySnapshot.isEmpty) {
                                trySend(mapDocumentToOrderEntity(querySnapshot.documents.first()))
                            } else {
                                trySend(null)
                            }
                        }
                }
            }
        }.getOrNull()

        awaitClose {
            listenerRegistration?.remove()
        }
    }

    // =====================================================================
    // FAVORITES CRUD OPERATIONS (FIRESTORE)
    // =====================================================================

    /**
     * Generates a deterministic document ID for a favorite item based on type, item ID and user phone.
     */
    fun getFavoriteDocId(userPhone: String, itemType: String, itemId: Long): String {
        val cleanPhone = userPhone.filter { it.isDigit() }.ifBlank { "guest" }
        return "${itemType.uppercase()}_${itemId}_$cleanPhone"
    }

    /**
     * Saves a favorite item to Firestore.
     */
    suspend fun saveFavorite(item: FavoriteItem): Result<String> = runCatching {
        val col = getFavoritesCollection() ?: return Result.failure(IllegalStateException("Firestore offline"))
        val docId = if (item.id.isNotBlank()) item.id else getFavoriteDocId(item.userPhone, item.itemType, item.itemId)
        val docRef = col.document(docId)
        val data = favoriteItemToMap(item.copy(id = docId))
        docRef.set(data, SetOptions.merge()).awaitTask()
        Log.d(TAG, "Favorite item saved to Firestore: ${item.title} (ID: $docId)")
        docId
    }

    /**
     * Removes a favorite item from Firestore by userPhone, itemType, and itemId.
     */
    suspend fun removeFavorite(userPhone: String, itemType: String, itemId: Long): Result<Unit> = runCatching {
        val col = getFavoritesCollection() ?: return Result.failure(IllegalStateException("Firestore offline"))
        val docId = getFavoriteDocId(userPhone, itemType, itemId)
        col.document(docId).delete().awaitTask()
        Log.d(TAG, "Favorite item removed from Firestore: $docId")
    }

    /**
     * Removes a favorite item by Firestore document ID.
     */
    suspend fun removeFavoriteById(docId: String): Result<Unit> = runCatching {
        val col = getFavoritesCollection() ?: return Result.failure(IllegalStateException("Firestore offline"))
        col.document(docId).delete().awaitTask()
        Log.d(TAG, "Favorite doc removed: $docId")
    }

    /**
     * Clears all favorites for a specific user phone number.
     */
    suspend fun clearAllFavorites(userPhone: String): Result<Unit> = runCatching {
        val col = getFavoritesCollection() ?: return Result.failure(IllegalStateException("Firestore offline"))
        val cleanTargetPhone = userPhone.filter { it.isDigit() }
        val snapshot = col.get().awaitTask()
        val matchingDocs = snapshot.documents.filter { doc ->
            val docPhone = doc.getString("userPhone") ?: ""
            val cleanDocPhone = docPhone.filter { it.isDigit() }
            docPhone.equals(userPhone, ignoreCase = true) ||
                    (cleanTargetPhone.isNotBlank() && cleanDocPhone == cleanTargetPhone)
        }
        for (doc in matchingDocs) {
            doc.reference.delete().awaitTask()
        }
        Log.d(TAG, "Cleared ${matchingDocs.size} favorites for $userPhone")
    }

    /**
     * Real-time flow of favorites for a customer phone number.
     */
    fun getFavoritesFlow(userPhone: String): Flow<List<FavoriteItem>> = callbackFlow {
        val col = getFavoritesCollection()
        if (col == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val cleanTargetPhone = userPhone.filter { it.isDigit() }
        val listenerRegistration = runCatching {
            col.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to favorites for $userPhone", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val favorites = snapshot.documents
                        .mapNotNull { doc -> mapDocumentToFavoriteItem(doc) }
                        .filter { item ->
                            if (userPhone.isBlank()) return@filter true
                            val itemPhoneClean = item.userPhone.filter { it.isDigit() }
                            item.userPhone.equals(userPhone, ignoreCase = true) ||
                                    (cleanTargetPhone.isNotBlank() && (itemPhoneClean == cleanTargetPhone ||
                                            itemPhoneClean.endsWith(cleanTargetPhone) ||
                                            cleanTargetPhone.endsWith(itemPhoneClean))) ||
                                    item.userPhone.isBlank()
                        }
                        .sortedByDescending { it.timestamp }
                    trySend(favorites)
                }
            }
        }.getOrNull()

        awaitClose {
            listenerRegistration?.remove()
        }
    }

    /**
     * One-shot fetch of favorites from Firestore by phone number.
     */
    suspend fun fetchFavoritesByPhone(userPhone: String): Result<List<FavoriteItem>> = runCatching {
        val col = getFavoritesCollection() ?: return Result.success(emptyList())
        val cleanTargetPhone = userPhone.filter { it.isDigit() }
        val snapshot = col.get().awaitTask()
        snapshot.documents
            .mapNotNull { doc -> mapDocumentToFavoriteItem(doc) }
            .filter { item ->
                if (userPhone.isBlank()) return@filter true
                val itemPhoneClean = item.userPhone.filter { it.isDigit() }
                item.userPhone.equals(userPhone, ignoreCase = true) ||
                        (cleanTargetPhone.isNotBlank() && (itemPhoneClean == cleanTargetPhone ||
                                itemPhoneClean.endsWith(cleanTargetPhone) ||
                                cleanTargetPhone.endsWith(itemPhoneClean))) ||
                        item.userPhone.isBlank()
            }
            .sortedByDescending { it.timestamp }
    }

    // =====================================================================
    // CONVERTER & MAPPER HELPERS
    // =====================================================================

    private fun productItemToMap(product: ProductItem): Map<String, Any?> {
        return mapOf(
            "id" to product.id,
            "name" to product.name,
            "scientificName" to product.scientificName,
            "category" to product.category,
            "price" to product.price,
            "originalPrice" to product.originalPrice,
            "stockQuantity" to product.stockQuantity,
            "description" to product.description,
            "careLevel" to product.careLevel,
            "waterParameters" to product.waterParameters,
            "imageUrl" to product.imageUrl,
            "badge" to product.badge,
            "isAvailable" to product.isAvailable
        )
    }

    private fun mapDocumentToProductItem(doc: DocumentSnapshot): ProductItem? {
        return try {
            val id = doc.getLong("id") ?: (doc.id.toLongOrNull() ?: 0L)
            val name = doc.getString("name") ?: return null
            val scientificName = doc.getString("scientificName") ?: ""
            val category = doc.getString("category") ?: "Fishes"
            val price = doc.getDouble("price") ?: 0.0
            val originalPrice = doc.getDouble("originalPrice")
            val stockQuantity = doc.getLong("stockQuantity")?.toInt() ?: 10
            val description = doc.getString("description") ?: ""
            val careLevel = doc.getString("careLevel") ?: "Moderate"
            val waterParameters = doc.getString("waterParameters") ?: "Temp: 24-28°C • pH: 6.5-7.5"
            val imageUrl = doc.getString("imageUrl") ?: "img_hero_aquarium"
            val badge = doc.getString("badge") ?: ""
            val isAvailable = doc.getBoolean("isAvailable") ?: (stockQuantity > 0)

            ProductItem(
                id = id,
                name = name,
                scientificName = scientificName,
                category = category,
                price = price,
                originalPrice = originalPrice,
                stockQuantity = stockQuantity,
                description = description,
                careLevel = careLevel,
                waterParameters = waterParameters,
                imageUrl = imageUrl,
                badge = badge,
                isAvailable = isAvailable
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error mapping document to ProductItem: ${doc.id}", e)
            null
        }
    }

    private fun galleryItemToMap(item: GalleryItem): Map<String, Any> {
        return mapOf(
            "id" to item.id,
            "title" to item.title,
            "category" to item.category,
            "description" to item.description,
            "imageUrl" to item.imageUrl,
            "tankSpecs" to item.tankSpecs,
            "floraFauna" to item.floraFauna,
            "likesCount" to item.likesCount,
            "isUserLiked" to item.isUserLiked,
            "dateAdded" to item.dateAdded,
            "timestamp" to item.timestamp
        )
    }

    private fun mapDocumentToGalleryItem(doc: DocumentSnapshot): GalleryItem? {
        return try {
            val id = doc.getLong("id") ?: (doc.id.toLongOrNull() ?: 0L)
            val title = doc.getString("title") ?: return null
            val category = doc.getString("category") ?: "Planted Aquascapes"
            val description = doc.getString("description") ?: ""
            val imageUrl = doc.getString("imageUrl") ?: "img_hero_aquarium"
            val tankSpecs = doc.getString("tankSpecs") ?: ""
            val floraFauna = doc.getString("floraFauna") ?: ""
            val likesCount = doc.getLong("likesCount")?.toInt() ?: 0
            val isUserLiked = doc.getBoolean("isUserLiked") ?: false
            val dateAdded = doc.getString("dateAdded") ?: ""
            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

            GalleryItem(
                id = id,
                title = title,
                category = category,
                description = description,
                imageUrl = imageUrl,
                tankSpecs = tankSpecs,
                floraFauna = floraFauna,
                likesCount = likesCount,
                isUserLiked = isUserLiked,
                dateAdded = dateAdded,
                timestamp = timestamp
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error mapping document to GalleryItem: ${doc.id}", e)
            null
        }
    }

    private fun orderEntityToMap(order: OrderEntity): Map<String, Any> {
        val dateStr = java.text.SimpleDateFormat("MMM dd, yyyy • hh:mm a", java.util.Locale.getDefault())
            .format(java.util.Date(order.timestamp))

        // Parse items json into structured list of maps for web console
        val structuredItems = try {
            val jsonArray = org.json.JSONArray(order.itemsJson)
            val list = mutableListOf<Map<String, Any>>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    mapOf(
                        "name" to obj.optString("name", "Aquarium Item"),
                        "price" to obj.optDouble("price", 0.0),
                        "quantity" to obj.optInt("quantity", 1),
                        "category" to obj.optString("category", "General"),
                        "imageUrl" to obj.optString("imageUrl", "img_hero_aquarium")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList<Map<String, Any>>()
        }

        return mapOf(
            "id" to order.id,
            "orderNumber" to order.orderNumber,
            "customerPhone" to order.customerPhone,
            "customerName" to order.customerName,
            "deliveryAddress" to order.deliveryAddress,
            "itemsSummary" to order.itemsSummary,
            "itemsJson" to order.itemsJson,
            "items" to structuredItems,
            "subtotal" to order.subtotal,
            "packingFee" to order.packingFee,
            "deliveryFee" to order.deliveryFee,
            "totalAmount" to order.totalAmount,
            "paymentMethod" to order.paymentMethod,
            "orderStatus" to order.orderStatus,
            "adminNotes" to order.adminNotes,
            "customerNotes" to order.adminNotes,
            "timestamp" to order.timestamp,
            "formattedDate" to dateStr,
            "syncedToWebAdmin" to order.syncedToWebAdmin,
            "platform" to "Android App",
            "source" to "Fish Garden Mobile Checkout",
            "statusTimeline" to listOf(
                mapOf(
                    "status" to order.orderStatus,
                    "timestamp" to order.timestamp,
                    "note" to "Order created and submitted to Fish Garden Admin Portal"
                )
            )
        )
    }

    private fun mapDocumentToOrderEntity(doc: DocumentSnapshot): OrderEntity? {
        return try {
            val id = doc.getLong("id") ?: 0L
            val orderNumber = doc.getString("orderNumber") ?: doc.id
            val customerPhone = doc.getString("customerPhone") ?: ""
            val customerName = doc.getString("customerName") ?: ""
            val deliveryAddress = doc.getString("deliveryAddress") ?: ""
            val itemsSummary = doc.getString("itemsSummary") ?: ""
            val itemsJson = doc.getString("itemsJson") ?: "[]"
            val subtotal = doc.getDouble("subtotal") ?: 0.0
            val packingFee = doc.getDouble("packingFee") ?: 0.0
            val deliveryFee = doc.getDouble("deliveryFee") ?: 0.0
            val totalAmount = doc.getDouble("totalAmount") ?: 0.0
            val paymentMethod = doc.getString("paymentMethod") ?: "Cash on Delivery (Live Fish Safe)"
            val orderStatus = doc.getString("orderStatus") ?: "Pending"
            val adminNotes = doc.getString("adminNotes") ?: ""
            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
            val syncedToWebAdmin = doc.getBoolean("syncedToWebAdmin") ?: true

            OrderEntity(
                id = id,
                orderNumber = orderNumber,
                customerPhone = customerPhone,
                customerName = customerName,
                deliveryAddress = deliveryAddress,
                itemsSummary = itemsSummary,
                itemsJson = itemsJson,
                subtotal = subtotal,
                packingFee = packingFee,
                deliveryFee = deliveryFee,
                totalAmount = totalAmount,
                paymentMethod = paymentMethod,
                orderStatus = orderStatus,
                adminNotes = adminNotes,
                timestamp = timestamp,
                syncedToWebAdmin = syncedToWebAdmin
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error mapping document to OrderEntity: ${doc.id}", e)
            null
        }
    }

    private fun favoriteItemToMap(item: FavoriteItem): Map<String, Any?> {
        return mapOf(
            "id" to item.id,
            "userPhone" to item.userPhone,
            "itemId" to item.itemId,
            "itemType" to item.itemType,
            "title" to item.title,
            "category" to item.category,
            "price" to item.price,
            "imageUrl" to item.imageUrl,
            "description" to item.description,
            "tag" to item.tag,
            "careLevel" to item.careLevel,
            "waterParameters" to item.waterParameters,
            "tankSpecs" to item.tankSpecs,
            "floraFauna" to item.floraFauna,
            "timestamp" to item.timestamp
        )
    }

    private fun mapDocumentToFavoriteItem(doc: DocumentSnapshot): FavoriteItem? {
        return try {
            val id = doc.getString("id") ?: doc.id
            val userPhone = doc.getString("userPhone") ?: ""
            val itemId = doc.getLong("itemId") ?: 0L
            val itemType = doc.getString("itemType") ?: "PRODUCT"
            val title = doc.getString("title") ?: ""
            val category = doc.getString("category") ?: ""
            val price = doc.getDouble("price")
            val imageUrl = doc.getString("imageUrl") ?: "img_hero_aquarium"
            val description = doc.getString("description") ?: ""
            val tag = doc.getString("tag") ?: ""
            val careLevel = doc.getString("careLevel") ?: ""
            val waterParameters = doc.getString("waterParameters") ?: ""
            val tankSpecs = doc.getString("tankSpecs") ?: ""
            val floraFauna = doc.getString("floraFauna") ?: ""
            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

            FavoriteItem(
                id = id,
                userPhone = userPhone,
                itemId = itemId,
                itemType = itemType,
                title = title,
                category = category,
                price = price,
                imageUrl = imageUrl,
                description = description,
                tag = tag,
                careLevel = careLevel,
                waterParameters = waterParameters,
                tankSpecs = tankSpecs,
                floraFauna = floraFauna,
                timestamp = timestamp
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error mapping document to FavoriteItem: ${doc.id}", e)
            null
        }
    }
}

/**
 * Extension helper to await Task with coroutine cancellation support.
 */
private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            continuation.resume(result)
        }
        addOnFailureListener { exception ->
            continuation.resumeWith(Result.failure(exception))
        }
        addOnCanceledListener {
            continuation.cancel()
        }
    }
