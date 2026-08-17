package com.example.data.repository

import android.content.Context
import com.example.data.firestore.FirestoreHelper
import com.example.data.local.GalleryDao
import com.example.data.local.OrderDao
import com.example.data.local.ProductDao
import com.example.data.model.CartItem
import com.example.data.model.FavoriteItem
import com.example.data.model.GalleryItem
import com.example.data.model.OrderEntity
import com.example.data.model.ProductItem
import com.example.data.model.PushNotificationItem
import com.example.data.model.UserProfile
import com.example.data.notifications.FcmNotificationManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

class FishGardenRepository(
    private val context: Context,
    private val galleryDao: GalleryDao,
    private val productDao: ProductDao,
    private val orderDao: OrderDao,
    val firestoreRepository: IFirestoreAquariumRepository = FirestoreAquariumRepository()
) {
    // Database flows
    val allGalleryItems: Flow<List<GalleryItem>> = galleryDao.getAllGalleryItems()
    val allProducts: Flow<List<ProductItem>> = productDao.getAllProducts()
    
    // Live Firestore orders flow listening to 'orders' collection
    val firestoreOrders: Flow<List<OrderEntity>> = firestoreRepository.allOrdersFlow

    // FCM Notification streams
    val fcmToken: StateFlow<String?> = FcmNotificationManager.fcmToken
    val recentPushNotifications: StateFlow<List<PushNotificationItem>> = FcmNotificationManager.recentNotifications
    val fcmDeliveryLogs: StateFlow<List<String>> = FcmNotificationManager.fcmDeliveryLogs

    // Unified live orders stream for Admin Dashboard
    val allOrders: Flow<List<OrderEntity>> = kotlinx.coroutines.flow.combine(
        orderDao.getAllOrders(),
        firestoreOrders
    ) { roomOrders, cloudOrders ->
        val mergedMap = LinkedHashMap<String, OrderEntity>()
        // Cloud orders from Firestore 'orders' collection take live precedence
        cloudOrders.forEach { order ->
            val key = if (order.orderNumber.isNotBlank()) order.orderNumber else order.id.toString()
            mergedMap[key] = order
        }
        // Include local Room orders if not already in cloud stream
        roomOrders.forEach { order ->
            val key = if (order.orderNumber.isNotBlank()) order.orderNumber else order.id.toString()
            if (!mergedMap.containsKey(key)) {
                mergedMap[key] = order
            }
        }
        mergedMap.values.sortedByDescending { it.timestamp }
    }

    fun getOrdersForCustomer(phone: String): Flow<List<OrderEntity>> {
        return kotlinx.coroutines.flow.combine(
            orderDao.getOrdersByPhone(phone),
            firestoreRepository.getCustomerOrdersFlow(phone)
        ) { roomOrders, cloudOrders ->
            val mergedMap = LinkedHashMap<String, OrderEntity>()
            // Firestore orders take live precedence
            cloudOrders.forEach { order ->
                val key = if (order.orderNumber.isNotBlank()) order.orderNumber else order.id.toString()
                mergedMap[key] = order
            }
            // Room local orders
            roomOrders.forEach { order ->
                val key = if (order.orderNumber.isNotBlank()) order.orderNumber else order.id.toString()
                if (!mergedMap.containsKey(key)) {
                    mergedMap[key] = order
                }
            }
            mergedMap.values.sortedByDescending { it.timestamp }
        }
    }

    companion object {
        private const val PREFS_NAME = "fish_garden_auth_prefs"
        private const val KEY_ADMIN_PIN = "admin_passcode"
        const val DEFAULT_ADMIN_PIN = "1234"
    }

    private val sharedPrefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // In-memory Cart state
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    // Admin passcode state
    private val _adminPassword = MutableStateFlow(
        sharedPrefs.getString(KEY_ADMIN_PIN, DEFAULT_ADMIN_PIN) ?: DEFAULT_ADMIN_PIN
    )
    val adminPassword: StateFlow<String> = _adminPassword.asStateFlow()

    // Current logged-in user profile
    private val _currentUser = MutableStateFlow(
        UserProfile(
            phoneNumber = "+1 (555) 342-9100",
            fullName = "Alex Rivera",
            defaultAddress = "452 Coral Reef Way, Aqua City",
            landmark = "Near Harbor Marina",
            isLoggedIn = true,
            isAdminMode = false
        )
    )
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    // Webhook simulation log for Website Admin synchronization
    private val _adminWebhookEvents = MutableStateFlow<List<String>>(
        listOf(
            "System: Webhook listener active on https://fishgarden-aquarium.com/api/admin/sync",
            "Synced: Initial database connection established"
        )
    )
    val adminWebhookEvents: StateFlow<List<String>> = _adminWebhookEvents.asStateFlow()

    // --- Gallery CRUD ---
    suspend fun addGalleryItem(item: GalleryItem): Long {
        val id = galleryDao.insertGalleryItem(item)
        val savedItem = item.copy(id = id)
        runCatching { firestoreRepository.saveGalleryItem(savedItem) }
        logWebhook("New Gallery Item Published: '${item.title}' [ID: #$id] (Synced to Firestore & Web)")
        return id
    }

    suspend fun updateGalleryItem(item: GalleryItem) {
        galleryDao.updateGalleryItem(item)
        runCatching { firestoreRepository.updateGalleryItem(item) }
        logWebhook("Gallery Item Updated: '${item.title}' [ID: #${item.id}] (Synced to Firestore)")
    }

    suspend fun deleteGalleryItem(id: Long, title: String) {
        galleryDao.deleteById(id)
        runCatching { firestoreRepository.deleteGalleryItem(id) }
        logWebhook("Gallery Item Removed: '$title' [ID: #$id] (Removed from Firestore)")
    }

    suspend fun toggleGalleryLike(item: GalleryItem) {
        val newLiked = !item.isUserLiked
        val newCount = if (newLiked) item.likesCount + 1 else (item.likesCount - 1).coerceAtLeast(0)
        galleryDao.updateLike(item.id, newCount, newLiked)
        runCatching { firestoreRepository.updateGalleryLikes(item.id, newCount, newLiked) }
    }

    // --- Product CRUD & Stock ---
    suspend fun addProduct(product: ProductItem): Long {
        val id = productDao.insertProduct(product)
        val saved = product.copy(id = id)
        runCatching { FirestoreHelper.saveProduct(saved) }
        logWebhook("Product Added: '${product.name}' [ID: #$id, ₹${product.price}] (Synced to Inventory & Firestore)")
        return id
    }

    suspend fun updateProduct(product: ProductItem) {
        productDao.updateProduct(product)
        runCatching { FirestoreHelper.updateProduct(product) }
        logWebhook("Product Updated: '${product.name}' [ID: #${product.id}, Stock: ${product.stockQuantity}] (Synced to Firestore)")
    }

    suspend fun updateProductStock(product: ProductItem, newStock: Int) {
        val updated = product.copy(stockQuantity = newStock.coerceAtLeast(0), isAvailable = newStock > 0)
        productDao.updateProduct(updated)
        runCatching { FirestoreHelper.updateProductStock(product.id, newStock.coerceAtLeast(0)) }
        logWebhook("Stock Adjusted: '${product.name}' -> $newStock units (Synced to Firestore)")
    }

    suspend fun deleteProduct(product: ProductItem) {
        productDao.deleteProduct(product)
        runCatching { FirestoreHelper.deleteProduct(product.id) }
        logWebhook("Product Removed: '${product.name}' [ID: #${product.id}] (Removed from Firestore)")
    }

    // --- Favorites Management (Firestore) ---
    fun getFavoritesForCustomer(phone: String): Flow<List<FavoriteItem>> {
        return firestoreRepository.getFavoritesFlow(phone)
    }

    suspend fun saveFavoriteToFirestore(item: FavoriteItem): Result<String> {
        val result = firestoreRepository.saveFavorite(item)
        if (result.isSuccess) {
            logWebhook("❤️ [FAVORITES SYNC] Saved '${item.title}' (${item.itemType}) to Firestore for ${item.userPhone}")
        }
        return result
    }

    suspend fun removeFavoriteFromFirestore(userPhone: String, itemType: String, itemId: Long): Result<Unit> {
        val result = firestoreRepository.removeFavorite(userPhone, itemType, itemId)
        if (result.isSuccess) {
            logWebhook("💔 [FAVORITES SYNC] Removed ${itemType} #$itemId from Firestore")
        }
        return result
    }

    suspend fun removeFavoriteById(docId: String): Result<Unit> {
        val result = firestoreRepository.removeFavoriteById(docId)
        if (result.isSuccess) {
            logWebhook("💔 [FAVORITES SYNC] Removed favorite doc '$docId' from Firestore")
        }
        return result
    }

    suspend fun clearAllFavoritesFromFirestore(userPhone: String): Result<Unit> {
        val result = firestoreRepository.clearAllFavorites(userPhone)
        if (result.isSuccess) {
            logWebhook("🧹 [FAVORITES SYNC] Cleared all favorites for $userPhone in Firestore")
        }
        return result
    }

    suspend fun fetchFavoritesFromFirestore(userPhone: String): Result<List<FavoriteItem>> {
        return firestoreRepository.fetchFavoritesByPhone(userPhone)
    }

    // --- Cart Management ---
    fun addToCart(product: ProductItem) {
        _cartItems.update { current ->
            val existingIndex = current.indexOfFirst { it.product.id == product.id }
            if (existingIndex >= 0) {
                current.mapIndexed { index, item ->
                    if (index == existingIndex) item.copy(quantity = item.quantity + 1) else item
                }
            } else {
                current + CartItem(product = product, quantity = 1)
            }
        }
    }

    fun updateCartQuantity(productId: Long, delta: Int) {
        _cartItems.update { current ->
            current.mapNotNull { item ->
                if (item.product.id == productId) {
                    val newQty = item.quantity + delta
                    if (newQty > 0) item.copy(quantity = newQty) else null
                } else {
                    item
                }
            }
        }
    }

    fun removeCartItem(productId: Long) {
        _cartItems.update { current ->
            current.filterNot { it.product.id == productId }
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    // --- Order Placement ---
    suspend fun placeOrder(
        customerName: String,
        customerPhone: String,
        deliveryAddress: String,
        paymentMethod: String,
        orderNotes: String,
        customPackingFee: Double = 2.50,
        customDeliveryFee: Double? = null
    ): OrderEntity {
        val currentCart = _cartItems.value
        require(currentCart.isNotEmpty()) { "Cart is empty" }

        val subtotal = currentCart.sumOf { it.totalPrice }
        val packingFee = customPackingFee
        val deliveryFee = customDeliveryFee ?: if (subtotal >= 60.0) 0.0 else 4.99
        val total = subtotal + packingFee + deliveryFee

        val itemsSummary = currentCart.joinToString(", ") { "${it.product.name} (x${it.quantity})" }
        val itemsJson = currentCart.joinToString(prefix = "[", postfix = "]", separator = ",") {
            "{\"id\":${it.product.id},\"name\":\"${it.product.name.replace("\"", "\\\"")}\",\"price\":${it.product.price},\"quantity\":${it.quantity},\"category\":\"${it.product.category}\",\"imageUrl\":\"${it.product.imageUrl}\"}"
        }

        val orderNum = "FG-${Random.nextInt(1000, 9999)}"
        val newOrder = OrderEntity(
            orderNumber = orderNum,
            customerPhone = customerPhone,
            customerName = customerName,
            deliveryAddress = deliveryAddress,
            itemsSummary = itemsSummary,
            itemsJson = itemsJson,
            subtotal = subtotal,
            packingFee = packingFee,
            deliveryFee = deliveryFee,
            totalAmount = total,
            paymentMethod = paymentMethod,
            orderStatus = "Pending",
            adminNotes = orderNotes,
            timestamp = System.currentTimeMillis(),
            syncedToWebAdmin = true
        )

        val insertedId = orderDao.insertOrder(newOrder)
        val savedOrder = newOrder.copy(id = insertedId)

        // Sync directly to Firestore
        val firestoreResult = runCatching { firestoreRepository.saveOrder(savedOrder) }
        val syncStatusText = if (firestoreResult.isSuccess) "Synced to Firestore" else "Saved locally (Firestore will retry)"

        // Subscribe to FCM topic for this order & customer phone
        FcmNotificationManager.subscribeToOrderTopic(savedOrder.orderNumber)
        FcmNotificationManager.subscribeToCustomerTopic(savedOrder.customerPhone)

        // Clear cart
        clearCart()

        // Push event to Admin Webhook simulator and FCM logs
        logWebhook("⚡ [REAL-TIME ORDER] New customer order received: $orderNum from $customerName ($customerPhone). Total: ${String.format("%.2f", total)}. $syncStatusText & Web Admin Portal.")
        FcmNotificationManager.logFcmEvent("FCM: Customer subscribed to updates for Order #$orderNum")

        return savedOrder
    }

    suspend fun updateOrderStatus(orderId: Long, newStatus: String, notes: String) {
        orderDao.updateOrderStatus(orderId, newStatus, notes)
        runCatching { firestoreRepository.updateOrderStatusById(orderId, newStatus, notes) }
        logWebhook("Order Status Changed: Order #$orderId is now '$newStatus' (Synced to Firestore)")

        // Dispatch FCM push notification
        val existingOrder = orderDao.getOrderById(orderId)
        if (existingOrder != null) {
            FcmNotificationManager.dispatchOrderStatusNotification(
                context = context,
                order = existingOrder.copy(orderStatus = newStatus, adminNotes = notes),
                newStatus = newStatus,
                adminNotes = notes
            )
        }
    }

    suspend fun updateOrderStatusByEntity(order: OrderEntity, newStatus: String, notes: String) {
        if (order.id > 0) {
            orderDao.updateOrderStatus(order.id, newStatus, notes)
        }
        runCatching {
            if (order.orderNumber.isNotBlank()) {
                firestoreRepository.updateOrderStatus(order.orderNumber, newStatus, notes)
            } else if (order.id > 0) {
                firestoreRepository.updateOrderStatusById(order.id, newStatus, notes)
            }
        }
        logWebhook("Order Status Changed: Order #${order.orderNumber} is now '$newStatus' (Synced to Firestore 'orders' collection)")

        // Dispatch FCM Push Notification to Customer
        val updatedOrder = order.copy(orderStatus = newStatus, adminNotes = notes)
        FcmNotificationManager.dispatchOrderStatusNotification(
            context = context,
            order = updatedOrder,
            newStatus = newStatus,
            adminNotes = notes
        )
    }

    suspend fun deleteOrder(orderId: Long) {
        orderDao.deleteOrderById(orderId)
        runCatching { firestoreRepository.deleteOrder(orderId.toString()) }
        logWebhook("Order #$orderId deleted from Admin Records & Firestore")
    }

    suspend fun deleteOrderByEntity(order: OrderEntity) {
        if (order.id > 0) {
            orderDao.deleteOrderById(order.id)
        }
        runCatching {
            val key = if (order.orderNumber.isNotBlank()) order.orderNumber else order.id.toString()
            firestoreRepository.deleteOrder(key)
        }
        logWebhook("Order #${order.orderNumber} removed from Admin Records & Firestore 'orders'")
    }

    suspend fun sendTestOrderToFirestore(): Result<String> {
        val res = firestoreRepository.createTestOrder()
        if (res.isSuccess) {
            logWebhook("⚡ [FIRESTORE SIMULATOR] Test order ${res.getOrNull()} sent to 'orders' collection")
        }
        return res
    }

    // --- Authentication & User Profile ---
    fun loginWithPhone(phoneNumber: String, fullName: String, address: String) {
        _currentUser.value = UserProfile(
            phoneNumber = phoneNumber,
            fullName = fullName.ifBlank { "Aquarium Enthusiast" },
            defaultAddress = address.ifBlank { "Delivery Address" },
            landmark = "",
            isLoggedIn = true,
            isAdminMode = false
        )
        logWebhook("User signed in with phone: $phoneNumber ($fullName)")
    }

    fun updateUserProfile(name: String, address: String, landmark: String) {
        _currentUser.update {
            it.copy(
                fullName = name,
                defaultAddress = address,
                landmark = landmark
            )
        }
    }

    fun logout() {
        _currentUser.value = UserProfile(isLoggedIn = false, isAdminMode = false)
    }

    fun toggleAdminMode(enabled: Boolean) {
        _currentUser.update { it.copy(isAdminMode = enabled) }
        logWebhook(if (enabled) "Admin Panel Authenticated" else "Exited Admin Mode")
    }

    // --- Admin Password Security Management ---
    fun verifyAdminPassword(enteredPin: String): Boolean {
        val current = _adminPassword.value.trim()
        val entered = enteredPin.trim()
        return if (current.isEmpty()) {
            entered.isEmpty() || entered == DEFAULT_ADMIN_PIN
        } else {
            entered == current
        }
    }

    fun changeAdminPassword(currentPin: String, newPin: String): Result<Unit> {
        val currentSaved = _adminPassword.value.trim()
        val enteredCurrent = currentPin.trim()
        val cleanNew = newPin.trim()

        if (currentSaved.isNotEmpty() && enteredCurrent != currentSaved) {
            return Result.failure(IllegalArgumentException("Current passcode does not match."))
        }

        if (cleanNew.length < 4) {
            return Result.failure(IllegalArgumentException("New passcode must be at least 4 characters."))
        }

        sharedPrefs.edit().putString(KEY_ADMIN_PIN, cleanNew).apply()
        _adminPassword.value = cleanNew
        logWebhook("Admin Security: Admin passcode updated successfully.")
        return Result.success(Unit)
    }

    fun resetAdminPasswordToDefault() {
        sharedPrefs.edit().putString(KEY_ADMIN_PIN, DEFAULT_ADMIN_PIN).apply()
        _adminPassword.value = DEFAULT_ADMIN_PIN
        logWebhook("Admin Security: Admin passcode reset to default (1234).")
    }

    private fun logWebhook(message: String) {
        _adminWebhookEvents.update { current ->
            (listOf(message) + current).take(30)
        }
    }
}
