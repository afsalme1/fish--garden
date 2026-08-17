package com.example.data.model

data class CartItem(
    val product: ProductItem,
    val quantity: Int = 1
) {
    val totalPrice: Double
        get() = product.price * quantity
}

data class UserProfile(
    val phoneNumber: String = "",
    val fullName: String = "",
    val defaultAddress: String = "",
    val landmark: String = "",
    val isLoggedIn: Boolean = false,
    val isAdminMode: Boolean = false
)
