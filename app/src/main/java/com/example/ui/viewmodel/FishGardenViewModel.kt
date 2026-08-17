package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FishGardenDatabase
import com.example.data.model.CartItem
import com.example.data.model.FavoriteItem
import com.example.data.model.GalleryItem
import com.example.data.model.OrderEntity
import com.example.data.model.ProductItem
import com.example.data.model.UserProfile
import com.example.data.repository.FishGardenRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab(val title: String) {
    GALLERY("Gallery"),
    SHOP("Shop"),
    FAVORITES("Favorites"),
    CART("Cart"),
    MY_ORDERS("My Orders"),
    ADMIN("Website Admin")
}

enum class AdminSubTab(val title: String) {
    LIVE_ORDERS("Incoming Orders"),
    WEB_SYNC("Web Server & Webhook"),
    GALLERY_MANAGER("Gallery Manager"),
    INVENTORY("Inventory")
}

class FishGardenViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FishGardenRepository

    init {
        val db = FishGardenDatabase.getDatabase(application, viewModelScope)
        repository = FishGardenRepository(
            context = application.applicationContext,
            galleryDao = db.galleryDao(),
            productDao = db.productDao(),
            orderDao = db.orderDao()
        )
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                FishGardenDatabase.populateInitialData(db)
            }
        }
    }

    // Active Navigation
    val currentTab = MutableStateFlow(AppTab.GALLERY)
    val currentAdminSubTab = MutableStateFlow(AdminSubTab.LIVE_ORDERS)

    // User session
    val currentUser: StateFlow<UserProfile> = repository.currentUser

    // FCM Notification streams
    val fcmToken: StateFlow<String?> = repository.fcmToken
    val recentPushNotifications = repository.recentPushNotifications
    val fcmDeliveryLogs = repository.fcmDeliveryLogs

    // Cart
    val cartItems: StateFlow<List<CartItem>> = repository.cartItems

    val cartTotalCount: StateFlow<Int> = repository.cartItems.combine(MutableStateFlow(0)) { items, _ ->
        items.sumOf { it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val cartSubtotal: StateFlow<Double> = repository.cartItems.combine(MutableStateFlow(0)) { items, _ ->
        items.sumOf { it.totalPrice }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Gallery state
    val selectedGalleryCategory = MutableStateFlow("All")
    val gallerySearchQuery = MutableStateFlow("")
    private val rawGalleryItems = repository.allGalleryItems

    val galleryItems: StateFlow<List<GalleryItem>> = combine(
        rawGalleryItems,
        selectedGalleryCategory,
        gallerySearchQuery
    ) { items, cat, query ->
        val trimmedQuery = query.trim()
        val queryTokens = if (trimmedQuery.isBlank()) emptyList() else trimmedQuery.split(Regex("[\\s,]+")).filter { it.isNotBlank() }

        items.filter { item ->
            val matchCategory = when (cat) {
                "All" -> true
                "Freshwater" -> item.category.equals("Freshwater", ignoreCase = true) ||
                        item.category.contains("Planted", ignoreCase = true) ||
                        item.category.contains("Exotic", ignoreCase = true) ||
                        item.category.contains("Nano", ignoreCase = true) ||
                        item.description.contains("freshwater", ignoreCase = true)
                "Saltwater" -> item.category.equals("Saltwater", ignoreCase = true) ||
                        item.category.contains("Marine", ignoreCase = true) ||
                        item.category.contains("Reef", ignoreCase = true) ||
                        item.description.contains("marine", ignoreCase = true) ||
                        item.description.contains("saltwater", ignoreCase = true) ||
                        item.tankSpecs.contains("marine", ignoreCase = true)
                "Accessories" -> item.category.equals("Accessories", ignoreCase = true) ||
                        item.category.contains("Hardware", ignoreCase = true) ||
                        item.category.contains("Equipment", ignoreCase = true) ||
                        item.category.contains("Hardscape", ignoreCase = true) ||
                        item.tankSpecs.contains("Filter", ignoreCase = true) ||
                        item.description.contains("equipment", ignoreCase = true) ||
                        item.description.contains("hardware", ignoreCase = true)
                else -> item.category.equals(cat, ignoreCase = true)
            }

            val matchQuery = if (queryTokens.isEmpty()) {
                true
            } else {
                val searchableCombinedText = "${item.title} ${item.floraFauna} ${item.description} ${item.category} ${item.tankSpecs}"
                queryTokens.all { token ->
                    searchableCombinedText.contains(token, ignoreCase = true)
                }
            }

            matchCategory && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setGallerySearchQuery(query: String) {
        gallerySearchQuery.value = query
    }

    fun setGalleryCategory(category: String) {
        selectedGalleryCategory.value = category
    }

    fun clearGalleryFilters() {
        selectedGalleryCategory.value = "All"
        gallerySearchQuery.value = ""
    }

    // Shop state
    val selectedShopCategory = MutableStateFlow("All")
    val shopSearchQuery = MutableStateFlow("")
    private val rawProducts = repository.allProducts

    val products: StateFlow<List<ProductItem>> = combine(
        rawProducts,
        selectedShopCategory,
        shopSearchQuery
    ) { items, category, query ->
        val trimmedQuery = query.trim()
        items.filter { product ->
            val matchCategory = (category == "All" || product.category.equals(category, ignoreCase = true))
            val matchQuery = trimmedQuery.isBlank() ||
                    product.name.contains(trimmedQuery, ignoreCase = true) ||
                    product.category.contains(trimmedQuery, ignoreCase = true) ||
                    product.description.contains(trimmedQuery, ignoreCase = true) ||
                    product.scientificName.contains(trimmedQuery, ignoreCase = true) ||
                    product.badge.contains(trimmedQuery, ignoreCase = true) ||
                    product.careLevel.contains(trimmedQuery, ignoreCase = true) ||
                    (trimmedQuery.equals("supplies", ignoreCase = true) && !product.category.equals("Fishes", ignoreCase = true)) ||
                    (trimmedQuery.equals("fish", ignoreCase = true) && product.category.equals("Fishes", ignoreCase = true))

            matchCategory && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All orders for Admin Dashboard
    val allOrders: StateFlow<List<OrderEntity>> = repository.allOrders.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Filtered orders for logged in customer
    val customerOrderFilter = MutableStateFlow("All")
    val customerOrderSearch = MutableStateFlow("")
    val isRefreshingCustomerOrders = MutableStateFlow(false)

    val myCustomerOrders: StateFlow<List<OrderEntity>> = currentUser.flatMapLatest { user ->
        repository.getOrdersForCustomer(user.phoneNumber)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredCustomerOrders: StateFlow<List<OrderEntity>> = combine(
        myCustomerOrders,
        customerOrderFilter,
        customerOrderSearch
    ) { orders, filter, search ->
        orders.filter { order ->
            val matchesFilter = when (filter) {
                "Active" -> !order.orderStatus.equals("Delivered", ignoreCase = true) && !order.orderStatus.equals("Cancelled", ignoreCase = true)
                "Delivered" -> order.orderStatus.equals("Delivered", ignoreCase = true)
                "Cancelled" -> order.orderStatus.equals("Cancelled", ignoreCase = true)
                else -> true
            }
            val matchesSearch = search.isBlank() ||
                    order.orderNumber.contains(search, ignoreCase = true) ||
                    order.itemsSummary.contains(search, ignoreCase = true) ||
                    order.deliveryAddress.contains(search, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun refreshOrdersFromFirestore() {
        viewModelScope.launch {
            isRefreshingCustomerOrders.value = true
            val phone = currentUser.value.phoneNumber
            val result = com.example.data.firestore.FirestoreHelper.fetchOrdersByPhone(phone)
            isRefreshingCustomerOrders.value = false
            if (result.isSuccess) {
                val fetched = result.getOrNull() ?: emptyList()
                _snackBarMessage.emit("✓ Synced with Firestore: ${fetched.size} orders found for $phone")
            } else {
                _snackBarMessage.emit("Firestore check completed: listening for real-time updates")
            }
        }
    }

    // Webhook events for Website Admin Sync page
    val adminWebhookEvents: StateFlow<List<String>> = repository.adminWebhookEvents

    // --- Favorites State (Firestore) ---
    val favoriteCategoryFilter = MutableStateFlow("All")
    val favoriteSearchQuery = MutableStateFlow("")
    val isRefreshingFavorites = MutableStateFlow(false)

    private val rawFavorites = currentUser.flatMapLatest { user ->
        repository.getFavoritesForCustomer(user.phoneNumber)
    }

    val favoriteItems: StateFlow<List<FavoriteItem>> = rawFavorites.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val favoriteItemKeys: StateFlow<Set<String>> = favoriteItems.combine(MutableStateFlow(0)) { items, _ ->
        items.map { "${it.itemType.uppercase()}_${it.itemId}" }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val totalFavoritesCount: StateFlow<Int> = favoriteItems.combine(MutableStateFlow(0)) { items, _ ->
        items.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val filteredFavorites: StateFlow<List<FavoriteItem>> = combine(
        favoriteItems,
        favoriteCategoryFilter,
        favoriteSearchQuery
    ) { items, filter, query ->
        items.filter { item ->
            val matchesFilter = when (filter) {
                "All" -> true
                "Fishes & Products" -> item.itemType.equals("PRODUCT", ignoreCase = true)
                "Aquascapes & Tanks" -> item.itemType.equals("GALLERY", ignoreCase = true)
                else -> item.category.contains(filter, ignoreCase = true)
            }

            val matchesQuery = query.isBlank() ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.description.contains(query, ignoreCase = true) ||
                    item.category.contains(query, ignoreCase = true) ||
                    item.tag.contains(query, ignoreCase = true) ||
                    item.tankSpecs.contains(query, ignoreCase = true) ||
                    item.floraFauna.contains(query, ignoreCase = true)

            matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Modals / Dialogs UI state
    val viewingGalleryItem = MutableStateFlow<GalleryItem?>(null)
    val editingGalleryItem = MutableStateFlow<GalleryItem?>(null)
    val isAddGalleryDialogOpen = MutableStateFlow(false)
    val viewingProduct = MutableStateFlow<ProductItem?>(null)
    val editingProduct = MutableStateFlow<ProductItem?>(null)
    val isAddProductDialogOpen = MutableStateFlow(false)

    val isPhoneLoginDialogOpen = MutableStateFlow(false)
    val isAdminPinDialogOpen = MutableStateFlow(false)
    val isAdminChangePasswordDialogOpen = MutableStateFlow(false)
    val adminPassword: StateFlow<String> = repository.adminPassword
    val orderSuccessOrder = MutableStateFlow<OrderEntity?>(null)
    val isPlacingOrder = MutableStateFlow(false)
    val orderPlacementError = MutableStateFlow<String?>(null)

    private val _snackBarMessage = MutableSharedFlow<String>()
    val snackBarMessage: SharedFlow<String> = _snackBarMessage.asSharedFlow()

    // --- Gallery Actions ---
    fun openAddGalleryDialog() {
        editingGalleryItem.value = null
        isAddGalleryDialogOpen.value = true
    }

    fun openEditGalleryDialog(item: GalleryItem) {
        editingGalleryItem.value = item
        isAddGalleryDialogOpen.value = true
    }

    fun closeGalleryDialog() {
        isAddGalleryDialogOpen.value = false
        editingGalleryItem.value = null
    }

    fun saveGalleryItem(
        title: String,
        category: String,
        description: String,
        tankSpecs: String,
        floraFauna: String,
        imageUrl: String
    ) {
        viewModelScope.launch {
            val existing = editingGalleryItem.value
            if (existing != null) {
                val updated = existing.copy(
                    title = title,
                    category = category,
                    description = description,
                    tankSpecs = tankSpecs,
                    floraFauna = floraFauna,
                    imageUrl = imageUrl.ifBlank { existing.imageUrl }
                )
                repository.updateGalleryItem(updated)
                _snackBarMessage.emit("Gallery item '${title}' updated successfully!")
            } else {
                val newItem = GalleryItem(
                    title = title,
                    category = category,
                    description = description,
                    tankSpecs = tankSpecs,
                    floraFauna = floraFauna,
                    imageUrl = imageUrl.ifBlank { "img_hero_aquarium" }
                )
                repository.addGalleryItem(newItem)
                _snackBarMessage.emit("New aquascape '${title}' added to gallery!")
            }
            closeGalleryDialog()
        }
    }

    fun deleteGalleryItem(item: GalleryItem) {
        viewModelScope.launch {
            repository.deleteGalleryItem(item.id, item.title)
            if (viewingGalleryItem.value?.id == item.id) {
                viewingGalleryItem.value = null
            }
            _snackBarMessage.emit("Removed '${item.title}' from gallery.")
        }
    }

    fun toggleGalleryLike(item: GalleryItem) {
        viewModelScope.launch {
            repository.toggleGalleryLike(item)
            // Also toggle in favorites
            toggleGalleryFavorite(item)
        }
    }

    // --- Product Inventory CRUD Actions ---
    fun openAddProductDialog() {
        editingProduct.value = null
        isAddProductDialogOpen.value = true
    }

    fun openEditProductDialog(product: ProductItem) {
        editingProduct.value = product
        isAddProductDialogOpen.value = true
    }

    fun closeProductDialog() {
        isAddProductDialogOpen.value = false
        editingProduct.value = null
    }

    fun saveProduct(
        name: String,
        scientificName: String,
        category: String,
        price: Double,
        originalPrice: Double?,
        stockQuantity: Int,
        careLevel: String,
        waterParameters: String,
        description: String,
        imageUrl: String,
        badge: String
    ) {
        viewModelScope.launch {
            val existing = editingProduct.value
            if (existing != null) {
                val updated = existing.copy(
                    name = name,
                    scientificName = scientificName,
                    category = category,
                    price = price,
                    originalPrice = originalPrice,
                    stockQuantity = stockQuantity,
                    careLevel = careLevel,
                    waterParameters = waterParameters,
                    description = description,
                    imageUrl = imageUrl.ifBlank { existing.imageUrl },
                    badge = badge
                )
                repository.updateProduct(updated)
                _snackBarMessage.emit("Product '${name}' updated in inventory!")
            } else {
                val newProduct = ProductItem(
                    name = name,
                    scientificName = scientificName,
                    category = category,
                    price = price,
                    originalPrice = originalPrice,
                    stockQuantity = stockQuantity,
                    careLevel = careLevel,
                    waterParameters = waterParameters,
                    description = description,
                    imageUrl = imageUrl.ifBlank { "img_hero_aquarium" },
                    badge = badge
                )
                repository.addProduct(newProduct)
                _snackBarMessage.emit("New product '${name}' added to inventory!")
            }
            closeProductDialog()
        }
    }

    fun deleteProduct(product: ProductItem) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            if (viewingProduct.value?.id == product.id) {
                viewingProduct.value = null
            }
            _snackBarMessage.emit("Removed '${product.name}' from inventory.")
        }
    }

    fun updateProductStock(product: ProductItem, newStock: Int) {
        viewModelScope.launch {
            val validStock = newStock.coerceAtLeast(0)
            repository.updateProductStock(product, validStock)
            _snackBarMessage.emit("Stock for '${product.name}' updated to $validStock")
        }
    }

    // --- Favorites Actions (Firestore) ---
    fun isProductFavorite(productId: Long): Boolean {
        return favoriteItemKeys.value.contains("PRODUCT_$productId")
    }

    fun isGalleryFavorite(galleryId: Long): Boolean {
        return favoriteItemKeys.value.contains("GALLERY_$galleryId")
    }

    fun toggleProductFavorite(product: ProductItem) {
        viewModelScope.launch {
            val phone = currentUser.value.phoneNumber
            val isFav = isProductFavorite(product.id)
            if (isFav) {
                repository.removeFavoriteFromFirestore(phone, "PRODUCT", product.id)
                _snackBarMessage.emit("Removed '${product.name}' from Favorites")
            } else {
                val favItem = FavoriteItem(
                    userPhone = phone,
                    itemId = product.id,
                    itemType = "PRODUCT",
                    title = product.name,
                    category = product.category,
                    price = product.price,
                    imageUrl = product.imageUrl,
                    description = product.description,
                    tag = product.badge,
                    careLevel = product.careLevel,
                    waterParameters = product.waterParameters,
                    timestamp = System.currentTimeMillis()
                )
                repository.saveFavoriteToFirestore(favItem)
                _snackBarMessage.emit("❤️ Saved '${product.name}' to My Favorites (Firestore)")
            }
        }
    }

    fun toggleGalleryFavorite(galleryItem: GalleryItem) {
        viewModelScope.launch {
            val phone = currentUser.value.phoneNumber
            val isFav = isGalleryFavorite(galleryItem.id)
            if (isFav) {
                repository.removeFavoriteFromFirestore(phone, "GALLERY", galleryItem.id)
                _snackBarMessage.emit("Removed '${galleryItem.title}' from Favorites")
            } else {
                val favItem = FavoriteItem(
                    userPhone = phone,
                    itemId = galleryItem.id,
                    itemType = "GALLERY",
                    title = galleryItem.title,
                    category = galleryItem.category,
                    price = null,
                    imageUrl = galleryItem.imageUrl,
                    description = galleryItem.description,
                    tag = "Aquascape Showcase",
                    tankSpecs = galleryItem.tankSpecs,
                    floraFauna = galleryItem.floraFauna,
                    timestamp = System.currentTimeMillis()
                )
                repository.saveFavoriteToFirestore(favItem)
                _snackBarMessage.emit("❤️ Added '${galleryItem.title}' to My Favorites (Firestore)")
            }
        }
    }

    fun removeFavorite(item: FavoriteItem) {
        viewModelScope.launch {
            val phone = currentUser.value.phoneNumber
            if (item.id.isNotBlank()) {
                repository.removeFavoriteById(item.id)
            } else {
                repository.removeFavoriteFromFirestore(phone, item.itemType, item.itemId)
            }
            _snackBarMessage.emit("Removed '${item.title}' from Favorites")
        }
    }

    fun clearAllFavorites() {
        viewModelScope.launch {
            val phone = currentUser.value.phoneNumber
            repository.clearAllFavoritesFromFirestore(phone)
            _snackBarMessage.emit("🧹 Cleared all Favorites from Firestore")
        }
    }

    fun refreshFavoritesFromFirestore() {
        viewModelScope.launch {
            isRefreshingFavorites.value = true
            val phone = currentUser.value.phoneNumber
            val result = repository.fetchFavoritesFromFirestore(phone)
            isRefreshingFavorites.value = false
            if (result.isSuccess) {
                val list = result.getOrNull() ?: emptyList()
                _snackBarMessage.emit("✓ Synced: ${list.size} favorites retrieved from Firestore")
            } else {
                _snackBarMessage.emit("Favorites synced with Firestore live stream")
            }
        }
    }

    // --- Shop & Cart Actions ---
    fun addToCart(product: ProductItem) {
        repository.addToCart(product)
        viewModelScope.launch {
            _snackBarMessage.emit("Added ${product.name} to Cart")
        }
    }

    fun updateCartQuantity(productId: Long, delta: Int) {
        repository.updateCartQuantity(productId, delta)
    }

    fun removeCartItem(productId: Long) {
        repository.removeCartItem(productId)
    }

    fun clearCart() {
        repository.clearCart()
    }

    // --- Order Checkout ---
    fun placeOrder(
        customerName: String,
        customerPhone: String,
        deliveryAddress: String,
        paymentMethod: String,
        orderNotes: String,
        packingFee: Double = 2.50,
        deliveryFee: Double? = null
    ) {
        viewModelScope.launch {
            isPlacingOrder.value = true
            orderPlacementError.value = null
            try {
                val created = repository.placeOrder(
                    customerName = customerName,
                    customerPhone = customerPhone,
                    deliveryAddress = deliveryAddress,
                    paymentMethod = paymentMethod,
                    orderNotes = orderNotes,
                    customPackingFee = packingFee,
                    customDeliveryFee = deliveryFee
                )
                orderSuccessOrder.value = created
                _snackBarMessage.emit("🎉 Order ${created.orderNumber} placed and synced to Firestore!")
            } catch (e: Exception) {
                orderPlacementError.value = e.message ?: "Failed to place order"
                _snackBarMessage.emit("Order failed: ${e.message}")
            } finally {
                isPlacingOrder.value = false
            }
        }
    }

    val isGeneratingTestOrder = MutableStateFlow(false)
    val isNotificationsDialogOpen = MutableStateFlow(false)

    // --- FCM Push Notification Actions ---
    fun sendTestFcmPush(title: String, body: String, orderNumber: String = "FG-TEST") {
        viewModelScope.launch {
            com.example.data.notifications.FcmNotificationManager.sendTestPushNotification(
                context = getApplication<Application>().applicationContext,
                title = title,
                message = body,
                orderNumber = orderNumber
            )
            _snackBarMessage.emit("🚀 Test FCM Push Notification posted to system bar!")
        }
    }

    fun markPushNotificationsAsRead() {
        com.example.data.notifications.FcmNotificationManager.markNotificationsAsRead()
    }

    // --- Admin Operations ---
    fun updateOrderStatus(orderId: Long, newStatus: String, notes: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, newStatus, notes)
            _snackBarMessage.emit("Order #$orderId updated to $newStatus")
        }
    }

    fun updateOrder(order: OrderEntity, newStatus: String, notes: String) {
        viewModelScope.launch {
            repository.updateOrderStatusByEntity(order, newStatus, notes)
            _snackBarMessage.emit("Order #${order.orderNumber} updated to $newStatus (Synced to Firestore)")
        }
    }

    fun deleteOrder(orderId: Long) {
        viewModelScope.launch {
            repository.deleteOrder(orderId)
            _snackBarMessage.emit("Order #$orderId archived")
        }
    }

    fun deleteOrderEntity(order: OrderEntity) {
        viewModelScope.launch {
            repository.deleteOrderByEntity(order)
            _snackBarMessage.emit("Order #${order.orderNumber} removed from Firestore")
        }
    }

    fun sendTestOrderToFirestore() {
        viewModelScope.launch {
            isGeneratingTestOrder.value = true
            val res = repository.sendTestOrderToFirestore()
            isGeneratingTestOrder.value = false
            if (res.isSuccess) {
                _snackBarMessage.emit("🚀 New order ${res.getOrNull()} sent to Firestore 'orders' collection!")
            } else {
                _snackBarMessage.emit("Failed to create test order: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    // --- Phone Login & User Management ---
    fun loginWithPhone(phoneNumber: String, fullName: String, address: String) {
        repository.loginWithPhone(phoneNumber, fullName, address)
        isPhoneLoginDialogOpen.value = false
        viewModelScope.launch {
            _snackBarMessage.emit("Welcome back, ${fullName.ifBlank { "Aquarist" }}! ($phoneNumber)")
        }
    }

    fun logout() {
        repository.logout()
        viewModelScope.launch {
            _snackBarMessage.emit("Logged out successfully.")
        }
    }

    fun setAdminMode(enabled: Boolean) {
        repository.toggleAdminMode(enabled)
        if (enabled) {
            currentTab.value = AppTab.ADMIN
        } else {
            currentTab.value = AppTab.GALLERY
        }
    }

    // --- Admin Password Security Operations ---
    fun verifyAdminPassword(pin: String): Boolean {
        return repository.verifyAdminPassword(pin)
    }

    fun changeAdminPassword(currentPin: String, newPin: String): Result<Unit> {
        val result = repository.changeAdminPassword(currentPin, newPin)
        if (result.isSuccess) {
            isAdminChangePasswordDialogOpen.value = false
            viewModelScope.launch {
                _snackBarMessage.emit("🔒 Admin passcode updated successfully!")
            }
        }
        return result
    }

    fun resetAdminPasswordToDefault() {
        repository.resetAdminPasswordToDefault()
        viewModelScope.launch {
            _snackBarMessage.emit("Admin passcode reset to default (1234).")
        }
    }

    fun openChangeAdminPasswordDialog() {
        isAdminChangePasswordDialogOpen.value = true
    }

    fun closeChangeAdminPasswordDialog() {
        isAdminChangePasswordDialogOpen.value = false
    }
}
