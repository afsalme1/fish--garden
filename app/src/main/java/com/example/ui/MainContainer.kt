package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.WaterDamage
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.PushNotificationsDialog
import com.example.ui.screens.AdminPasscodeDialog
import com.example.ui.screens.AdminWebDashboardScreen
import com.example.ui.screens.CartCheckoutScreen
import com.example.ui.screens.CustomerOrdersScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.PhoneLoginDialog
import com.example.ui.screens.ShopScreen
import com.example.ui.theme.AdminSlate
import com.example.ui.theme.AquaPrimaryLight
import com.example.ui.theme.AquaticGreen
import com.example.ui.theme.CoralOrange
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.FishGardenViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainer(
    viewModel: FishGardenViewModel
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val userProfile by viewModel.currentUser.collectAsState()
    val cartCount by viewModel.cartTotalCount.collectAsState()
    val favoritesCount by viewModel.totalFavoritesCount.collectAsState()
    val isPhoneLoginOpen by viewModel.isPhoneLoginDialogOpen.collectAsState()
    val isAdminPinOpen by viewModel.isAdminPinDialogOpen.collectAsState()
    val isNotificationsOpen by viewModel.isNotificationsDialogOpen.collectAsState()
    val recentNotifications by viewModel.recentPushNotifications.collectAsState()
    val fcmToken by viewModel.fcmToken.collectAsState()

    val unreadCount = recentNotifications.count { !it.isRead }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackBarMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (currentTab != AppTab.ADMIN) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_app_icon),
                                    contentDescription = "Fish Garden Logo",
                                    modifier = Modifier.size(34.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = "Fish Garden",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Aquarium Store & Scape Gallery",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    actions = {
                        // Customer Phone Status Pill
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .clickable { viewModel.isPhoneLoginDialogOpen.value = true }
                                .padding(end = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (userProfile.isLoggedIn) Icons.Default.Phone else Icons.Default.Person,
                                    contentDescription = "Phone Login",
                                    tint = if (userProfile.isLoggedIn) AquaticGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (userProfile.isLoggedIn) userProfile.phoneNumber.takeLast(9) else "Login",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Push Notifications Bell with Badge
                        IconButton(
                            onClick = {
                                viewModel.markPushNotificationsAsRead()
                                viewModel.isNotificationsDialogOpen.value = true
                            }
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadCount > 0) {
                                        Badge(containerColor = CoralOrange) {
                                            Text("$unreadCount", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Push Notifications",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Web Admin Portal Switcher Action
                        IconButton(
                            onClick = {
                                if (userProfile.isAdminMode) {
                                    viewModel.currentTab.value = AppTab.ADMIN
                                } else {
                                    viewModel.isAdminPinDialogOpen.value = true
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Website Admin",
                                tint = CoralOrange
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                // 1. Gallery
                NavigationBarItem(
                    selected = currentTab == AppTab.GALLERY,
                    onClick = { viewModel.currentTab.value = AppTab.GALLERY },
                    icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery") },
                    label = { Text("Gallery", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_gallery")
                )

                // 2. Shop
                NavigationBarItem(
                    selected = currentTab == AppTab.SHOP,
                    onClick = { viewModel.currentTab.value = AppTab.SHOP },
                    icon = { Icon(Icons.Default.Store, contentDescription = "Shop") },
                    label = { Text("Shop", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_shop")
                )

                // 3. Favorites with Badge
                NavigationBarItem(
                    selected = currentTab == AppTab.FAVORITES,
                    onClick = { viewModel.currentTab.value = AppTab.FAVORITES },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (favoritesCount > 0) {
                                    Badge(containerColor = Color(0xFFFF4B4B)) {
                                        Text("$favoritesCount", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = "Favorites")
                        }
                    },
                    label = { Text("Favorites", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_favorites")
                )

                // 4. Cart with Badge
                NavigationBarItem(
                    selected = currentTab == AppTab.CART,
                    onClick = { viewModel.currentTab.value = AppTab.CART },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (cartCount > 0) {
                                    Badge(containerColor = CoralOrange) {
                                        Text("$cartCount", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
                        }
                    },
                    label = { Text("Cart", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_cart")
                )

                // 5. My Orders
                NavigationBarItem(
                    selected = currentTab == AppTab.MY_ORDERS,
                    onClick = { viewModel.currentTab.value = AppTab.MY_ORDERS },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "My Orders") },
                    label = { Text("My Orders", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_my_orders")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.GALLERY -> GalleryScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )

                AppTab.SHOP -> ShopScreen(
                    viewModel = viewModel,
                    onNavigateToCart = { viewModel.currentTab.value = AppTab.CART },
                    modifier = Modifier.fillMaxSize()
                )

                AppTab.FAVORITES -> FavoritesScreen(
                    viewModel = viewModel,
                    onExploreShop = { viewModel.currentTab.value = AppTab.SHOP },
                    onExploreGallery = { viewModel.currentTab.value = AppTab.GALLERY },
                    modifier = Modifier.fillMaxSize()
                )

                AppTab.CART -> CartCheckoutScreen(
                    viewModel = viewModel,
                    onExploreShop = { viewModel.currentTab.value = AppTab.SHOP },
                    modifier = Modifier.fillMaxSize()
                )

                AppTab.MY_ORDERS -> CustomerOrdersScreen(
                    viewModel = viewModel,
                    onExploreShop = { viewModel.currentTab.value = AppTab.SHOP },
                    modifier = Modifier.fillMaxSize()
                )

                AppTab.ADMIN -> AdminWebDashboardScreen(
                    viewModel = viewModel,
                    onExitAdmin = {
                        viewModel.setAdminMode(false)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // Phone Login Dialog
    if (isPhoneLoginOpen) {
        PhoneLoginDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.isPhoneLoginDialogOpen.value = false }
        )
    }

    // Admin PIN dialog
    if (isAdminPinOpen) {
        AdminPasscodeDialog(
            onDismiss = { viewModel.isAdminPinDialogOpen.value = false },
            onSuccess = {
                viewModel.isAdminPinDialogOpen.value = false
                viewModel.setAdminMode(true)
            }
        )
    }

    // Push Notifications Hub Dialog
    if (isNotificationsOpen) {
        PushNotificationsDialog(
            notifications = recentNotifications,
            fcmToken = fcmToken,
            onDismiss = { viewModel.isNotificationsDialogOpen.value = false },
            onSendTestPush = {
                viewModel.sendTestFcmPush(
                    title = "🐠 Fish Garden: Live Order Dispatched!",
                    body = "Your live aquatic order #FG-8821 is packed in thermal oxygen bags and in transit with courier.",
                    orderNumber = "FG-8821"
                )
            }
        )
    }
}
