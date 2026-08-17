package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firestore.FirestoreHelper
import com.example.data.model.GalleryItem
import com.example.data.model.OrderEntity
import com.example.data.model.ProductItem
import com.example.ui.components.AquariumImage
import com.example.ui.components.CategoryFilterRow
import com.example.ui.theme.AdminCyan
import com.example.ui.theme.AdminSlate
import com.example.ui.theme.AdminSlateHeader
import com.example.ui.theme.AquaticGreen
import com.example.ui.theme.CoralOrange
import com.example.ui.viewmodel.AdminSubTab
import com.example.ui.viewmodel.FishGardenViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminWebDashboardScreen(
    viewModel: FishGardenViewModel,
    onExitAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSubTab by viewModel.currentAdminSubTab.collectAsState()
    val allOrders by viewModel.allOrders.collectAsState()
    val galleryItems by viewModel.galleryItems.collectAsState()
    val products by viewModel.products.collectAsState()
    val webhookLogs by viewModel.adminWebhookEvents.collectAsState()
    val fcmLogs by viewModel.fcmDeliveryLogs.collectAsState()
    val fcmToken by viewModel.fcmToken.collectAsState()
    val isGeneratingTestOrder by viewModel.isGeneratingTestOrder.collectAsState()

    val editingProduct by viewModel.editingProduct.collectAsState()
    val isAddProductDialogOpen by viewModel.isAddProductDialogOpen.collectAsState()
    val editingGalleryItem by viewModel.editingGalleryItem.collectAsState()
    val isAddGalleryDialogOpen by viewModel.isAddGalleryDialogOpen.collectAsState()
    val isAdminChangePasswordDialogOpen by viewModel.isAdminChangePasswordDialogOpen.collectAsState()

    val pendingCount = allOrders.count { it.orderStatus.equals("Pending", ignoreCase = true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AdminSlateHeader)
            .testTag("admin_web_dashboard")
    ) {
        // Top Admin Web Console Bar
        AdminConsoleTopBar(
            pendingCount = pendingCount,
            totalOrders = allOrders.size,
            onExitAdmin = onExitAdmin,
            onChangePasswordClick = { viewModel.openChangeAdminPasswordDialog() }
        )

        // Sub Navigation Tabs
        ScrollableTabRow(
            selectedTabIndex = currentSubTab.ordinal,
            containerColor = AdminSlate,
            contentColor = Color.White,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[currentSubTab.ordinal]),
                    color = AdminCyan
                )
            },
            edgePadding = 12.dp
        ) {
            AdminSubTab.values().forEach { tab ->
                Tab(
                    selected = currentSubTab == tab,
                    onClick = { viewModel.currentAdminSubTab.value = tab },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = tab.title,
                                fontWeight = if (currentSubTab == tab) FontWeight.Bold else FontWeight.Normal,
                                color = if (currentSubTab == tab) AdminCyan else Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp
                            )
                            if (tab == AdminSubTab.LIVE_ORDERS && pendingCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = CoralOrange,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "$pendingCount",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }

        // SubTab Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B111E))
        ) {
            when (currentSubTab) {
                AdminSubTab.LIVE_ORDERS -> AdminLiveOrdersTab(
                    orders = allOrders,
                    isGeneratingTestOrder = isGeneratingTestOrder,
                    onSendTestOrder = { viewModel.sendTestOrderToFirestore() },
                    onUpdateStatus = { order, newStatus, notes ->
                        viewModel.updateOrder(order, newStatus, notes)
                    },
                    onDeleteOrder = { order ->
                        viewModel.deleteOrderEntity(order)
                    }
                )

                AdminSubTab.WEB_SYNC -> AdminWebSyncTab(
                    webhookLogs = webhookLogs,
                    fcmLogs = fcmLogs,
                    fcmToken = fcmToken,
                    orders = allOrders,
                    onSendTestFcm = { title, body ->
                        viewModel.sendTestFcmPush(title, body)
                    }
                )

                AdminSubTab.GALLERY_MANAGER -> AdminGalleryManagerTab(
                    galleryItems = galleryItems,
                    onAddNew = { viewModel.openAddGalleryDialog() },
                    onEdit = { viewModel.openEditGalleryDialog(it) },
                    onDelete = { viewModel.deleteGalleryItem(it) }
                )

                AdminSubTab.INVENTORY -> AdminInventoryTab(
                    products = products,
                    onAddNew = { viewModel.openAddProductDialog() },
                    onEdit = { viewModel.openEditProductDialog(it) },
                    onDelete = { viewModel.deleteProduct(it) },
                    onStockChange = { product, newStock -> viewModel.updateProductStock(product, newStock) }
                )
            }
        }
    }

    // Add / Edit Product Dialog in Admin
    if (isAddProductDialogOpen) {
        AddEditProductDialog(
            existingProduct = editingProduct,
            onDismiss = { viewModel.closeProductDialog() },
            onSave = { name, scientificName, category, price, originalPrice, stockQuantity, careLevel, waterParameters, description, imageUrl, badge ->
                viewModel.saveProduct(
                    name = name,
                    scientificName = scientificName,
                    category = category,
                    price = price,
                    originalPrice = originalPrice,
                    stockQuantity = stockQuantity,
                    careLevel = careLevel,
                    waterParameters = waterParameters,
                    description = description,
                    imageUrl = imageUrl,
                    badge = badge
                )
            }
        )
    }

    // Add / Edit Gallery Dialog in Admin
    if (isAddGalleryDialogOpen) {
        AddEditGalleryDialog(
            existingItem = editingGalleryItem,
            onDismiss = { viewModel.closeGalleryDialog() },
            onSave = { title, category, description, tankSpecs, floraFauna, imageUrl ->
                viewModel.saveGalleryItem(title, category, description, tankSpecs, floraFauna, imageUrl)
            }
        )
    }

    // Change Admin Passcode Dialog
    if (isAdminChangePasswordDialogOpen) {
        ChangeAdminPasswordDialog(
            onDismiss = { viewModel.closeChangeAdminPasswordDialog() },
            onSave = { currentPin, newPin ->
                viewModel.changeAdminPassword(currentPin, newPin)
            },
            onResetDefault = {
                viewModel.resetAdminPasswordToDefault()
            }
        )
    }
}

@Composable
fun AdminConsoleTopBar(
    pendingCount: Int,
    totalOrders: Int,
    onExitAdmin: () -> Unit,
    onChangePasswordClick: () -> Unit
) {
    Surface(
        color = AdminSlateHeader,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(AquaticGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FISH GARDEN ADMIN",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = "Live Firestore Cloud Sync • Orders & Inventory",
                    color = AdminCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable(onClick = onChangePasswordClick)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Admin Passcode",
                            tint = AdminCyan,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Passcode",
                            color = AdminCyan,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable(onClick = onExitAdmin)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Exit Admin",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Storefront",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminLiveOrdersTab(
    orders: List<OrderEntity>,
    isGeneratingTestOrder: Boolean,
    onSendTestOrder: () -> Unit,
    onUpdateStatus: (order: OrderEntity, newStatus: String, notes: String) -> Unit,
    onDeleteOrder: (order: OrderEntity) -> Unit
) {
    var statusFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var orderToDelete by remember { mutableStateOf<OrderEntity?>(null) }
    var orderToInspectJson by remember { mutableStateOf<OrderEntity?>(null) }
    var orderToUpdateStatus by remember { mutableStateOf<OrderEntity?>(null) }
    val context = LocalContext.current

    val filteredOrders = orders.filter { order ->
        val matchStatus = when (statusFilter) {
            "All" -> true
            "Shipped" -> order.orderStatus.equals("Shipped", ignoreCase = true) || order.orderStatus.equals("Out for Delivery", ignoreCase = true)
            "Packing" -> order.orderStatus.equals("Packing", ignoreCase = true) || order.orderStatus.equals("Processing", ignoreCase = true)
            else -> order.orderStatus.equals(statusFilter, ignoreCase = true)
        }
        val matchSearch = searchQuery.isBlank() ||
                order.orderNumber.contains(searchQuery, ignoreCase = true) ||
                order.customerName.contains(searchQuery, ignoreCase = true) ||
                order.customerPhone.contains(searchQuery, ignoreCase = true) ||
                order.deliveryAddress.contains(searchQuery, ignoreCase = true) ||
                order.itemsSummary.contains(searchQuery, ignoreCase = true)
        matchStatus && matchSearch
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Live Firestore Collection Status & Listener Info
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = AdminSlate),
                border = androidx.compose.foundation.BorderStroke(1.dp, AdminCyan.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Sensors,
                                contentDescription = null,
                                tint = AquaticGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Firestore Real-time Orders Listener",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Surface(
                            color = AquaticGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "🟢 LIVE STREAM ACTIVE",
                                color = AquaticGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Subscribed to Firestore collection: /${FirestoreHelper.COLLECTION_ORDERS}",
                        color = AdminCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Order status updates made here synchronize immediately to the customer's mobile app and Firestore in real time.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onSendTestOrder,
                            enabled = !isGeneratingTestOrder,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AdminCyan),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            if (isGeneratingTestOrder) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = AdminSlateHeader,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sending...", color = AdminSlateHeader, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = AdminSlateHeader,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Simulate Incoming Firestore Order",
                                    color = AdminSlateHeader,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // KPI Summary Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdminStatCard(
                    title = "Total Orders",
                    value = "${orders.size}",
                    color = AdminCyan,
                    modifier = Modifier.weight(1f)
                )
                AdminStatCard(
                    title = "Pending Action",
                    value = "${orders.count { it.orderStatus.equals("Pending", ignoreCase = true) }}",
                    color = CoralOrange,
                    modifier = Modifier.weight(1f)
                )
                AdminStatCard(
                    title = "Total Sales",
                    value = "$${String.format("%.0f", orders.filterNot { it.orderStatus.equals("Cancelled", ignoreCase = true) }.sumOf { it.totalAmount })}",
                    color = AquaticGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Search by Order #, Customer, Phone, or Item...",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = AdminCyan,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Clear search",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = AdminSlate,
                    unfocusedContainerColor = AdminSlate,
                    focusedBorderColor = AdminCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                ),
                singleLine = true
            )
        }

        // Status Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Pending", "Confirmed", "Packing", "Shipped", "Delivered", "Cancelled").forEach { status ->
                    val isSelected = statusFilter == status
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) AdminCyan else AdminSlate,
                        modifier = Modifier.clickable { statusFilter = status }
                    ) {
                        Text(
                            text = status,
                            color = if (isSelected) AdminSlateHeader else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Order Requests List
        if (filteredOrders.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No order requests found for '$statusFilter'",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Listening for customer orders in Firestore 'orders' collection...",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            items(filteredOrders, key = { it.orderNumber.ifBlank { it.id.toString() } }) { order ->
                AdminOrderCard(
                    order = order,
                    onQuickStatusSelected = { newStatus ->
                        onUpdateStatus(order, newStatus, order.adminNotes)
                    },
                    onOpenStatusDialog = { orderToUpdateStatus = order },
                    onInspectJson = { orderToInspectJson = order },
                    onCallCustomer = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.customerPhone}"))
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {}
                    },
                    onDelete = { orderToDelete = order }
                )
            }
        }
    }

    // Interactive Order Status & Dispatch Notes Update Dialog
    if (orderToUpdateStatus != null) {
        val targetOrder = orderToUpdateStatus!!
        var selectedStatus by remember(targetOrder) { mutableStateOf(targetOrder.orderStatus) }
        var notesInput by remember(targetOrder) { mutableStateOf(targetOrder.adminNotes) }

        AlertDialog(
            onDismissRequest = { orderToUpdateStatus = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = AdminCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Update Order Status & Dispatch", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Order #${targetOrder.orderNumber} • ${targetOrder.customerName}",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Select Order Status:",
                        color = AdminCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Status Choices
                    val statusOptions = listOf(
                        Triple("Pending", CoralOrange, "⏳ Order Placed (Awaiting Review)"),
                        Triple("Confirmed", Color(0xFF0284C7), "✓ Order Confirmed & Livestock Reserved"),
                        Triple("Packing", Color(0xFF7E22CE), "📦 Live Thermal Packaging with Oxygen"),
                        Triple("Shipped", Color(0xFF00838F), "🚚 Shipped / In Transit with Courier"),
                        Triple("Delivered", AquaticGreen, "🎉 Safely Delivered to Customer"),
                        Triple("Cancelled", Color(0xFFDC2626), "✖ Cancelled & Stock Returned")
                    )

                    statusOptions.forEach { (statusName, statusColor, desc) ->
                        val isChosen = selectedStatus.equals(statusName, ignoreCase = true)
                        Surface(
                            color = if (isChosen) statusColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(8.dp),
                            border = if (isChosen) androidx.compose.foundation.BorderStroke(1.5.dp, statusColor) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable { selectedStatus = statusName }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(statusColor, shape = CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = statusName,
                                        color = if (isChosen) statusColor else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = desc,
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Courier & Dispatch Note (Synced to Customer):",
                        color = AdminCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        placeholder = { Text("e.g. Dispatched via FedEx Live Express #98124, ETA 2:30 PM", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AdminCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateStatus(targetOrder, selectedStatus, notesInput)
                        orderToUpdateStatus = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminCyan)
                ) {
                    Text("Save & Sync to Firestore", color = AdminSlateHeader, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { orderToUpdateStatus = null }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (orderToDelete != null) {
        AlertDialog(
            onDismissRequest = { orderToDelete = null },
            title = { Text("Archive Order Record?") },
            text = { Text("Are you sure you want to remove Order #${orderToDelete?.orderNumber} from Firestore 'orders' collection and admin records?") },
            confirmButton = {
                Button(
                    onClick = {
                        orderToDelete?.let { onDeleteOrder(it) }
                        orderToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Archive / Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { orderToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // Raw Firestore Document JSON Inspector Modal
    if (orderToInspectJson != null) {
        val o = orderToInspectJson!!
        val docJson = """{
  "collection": "${FirestoreHelper.COLLECTION_ORDERS}",
  "documentId": "${o.orderNumber}",
  "fields": {
    "orderNumber": "${o.orderNumber}",
    "customerName": "${o.customerName}",
    "customerPhone": "${o.customerPhone}",
    "deliveryAddress": "${o.deliveryAddress}",
    "itemsSummary": "${o.itemsSummary}",
    "subtotal": ${o.subtotal},
    "packingFee": ${o.packingFee},
    "deliveryFee": ${o.deliveryFee},
    "totalAmount": ${o.totalAmount},
    "paymentMethod": "${o.paymentMethod}",
    "orderStatus": "${o.orderStatus}",
    "adminNotes": "${o.adminNotes}",
    "timestamp": ${o.timestamp},
    "syncedToWebAdmin": ${o.syncedToWebAdmin}
  }
}"""
        AlertDialog(
            onDismissRequest = { orderToInspectJson = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Code, contentDescription = null, tint = AdminCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Firestore Document Data", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Document path: /${FirestoreHelper.COLLECTION_ORDERS}/${o.orderNumber}",
                        color = AdminCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFF030712),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = docJson,
                            color = Color(0xFF93C5FD),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { orderToInspectJson = null },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminCyan)
                ) {
                    Text("Close", color = AdminSlateHeader, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun AdminStatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AdminSlate)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = color,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun AdminOrderCard(
    order: OrderEntity,
    onQuickStatusSelected: (String) -> Unit,
    onOpenStatusDialog: () -> Unit,
    onInspectJson: () -> Unit,
    onCallCustomer: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var statusMenuOpen by remember { mutableStateOf(false) }
    val dateStr = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(order.timestamp))

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AdminSlate),
        border = if (order.orderStatus.equals("Pending", ignoreCase = true))
            androidx.compose.foundation.BorderStroke(1.dp, CoralOrange.copy(alpha = 0.7f)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ORDER #${order.orderNumber}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = AquaticGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "🟢 FIRESTORE /orders",
                                color = AquaticGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                // Interactive Status Badge / Selector
                Box {
                    Surface(
                        color = when (order.orderStatus.lowercase()) {
                            "pending" -> CoralOrange
                            "confirmed" -> Color(0xFF0284C7)
                            "packing", "processing" -> Color(0xFF7E22CE)
                            "shipped", "out for delivery", "in transit" -> Color(0xFF00838F)
                            "delivered" -> AquaticGreen
                            "cancelled" -> Color(0xFFDC2626)
                            else -> Color.Gray
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { statusMenuOpen = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "${order.orderStatus} ▾",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = statusMenuOpen,
                        onDismissRequest = { statusMenuOpen = false }
                    ) {
                        listOf("Pending", "Confirmed", "Packing", "Shipped", "Delivered", "Cancelled").forEach { status ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(
                                                    when (status.lowercase()) {
                                                        "pending" -> CoralOrange
                                                        "confirmed" -> Color(0xFF0284C7)
                                                        "packing" -> Color(0xFF7E22CE)
                                                        "shipped" -> Color(0xFF00838F)
                                                        "delivered" -> AquaticGreen
                                                        "cancelled" -> Color(0xFFDC2626)
                                                        else -> Color.Gray
                                                    },
                                                    shape = CircleShape
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(status, fontWeight = if (order.orderStatus.equals(status, ignoreCase = true)) FontWeight.Bold else FontWeight.Normal)
                                    }
                                },
                                onClick = {
                                    statusMenuOpen = false
                                    onQuickStatusSelected(status)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Order Status Pipeline Visualizer
            AdminStatusPipelineVisualizer(status = order.orderStatus)

            Spacer(modifier = Modifier.height(12.dp))

            // Customer Contact Box
            Surface(
                color = Color(0xFF131D2D),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = order.customerName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "📱 ${order.customerPhone}",
                                color = AdminCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = onCallCustomer,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AquaticGreen),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Call", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "📍 ${order.deliveryAddress}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )

                    if (order.adminNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    tint = AdminCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Dispatch Note: ${order.adminNotes}",
                                    color = Color(0xFF93C5FD),
                                    fontSize = 11.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Items Summary
            Text(
                text = "Ordered Items & Packaging:",
                color = AdminCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = order.itemsSummary,
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )

            // Packaging & Pricing Breakdown
            Surface(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal:", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                        Text("₹${String.format("%.2f", order.subtotal)}", color = Color.White, fontSize = 11.sp)
                    }
                    if (order.packingFee > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Live Climate Packaging:", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            Text("₹${String.format("%.2f", order.packingFee)}", color = Color.White, fontSize = 11.sp)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Delivery Fee:", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                        Text("₹${String.format("%.2f", order.deliveryFee)}", color = Color.White, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Status Transition Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                when (order.orderStatus.lowercase()) {
                    "pending" -> {
                        Button(
                            onClick = { onQuickStatusSelected("Confirmed") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Confirm Order", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { onQuickStatusSelected("Cancelled") },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Cancel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    "confirmed" -> {
                        Button(
                            onClick = { onQuickStatusSelected("Packing") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE)),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Start Packing", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onQuickStatusSelected("Shipped") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00838F)),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mark Shipped", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    "packing", "processing" -> {
                        Button(
                            onClick = { onQuickStatusSelected("Shipped") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00838F)),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mark Shipped", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    "shipped", "out for delivery", "in transit" -> {
                        Button(
                            onClick = { onQuickStatusSelected("Delivered") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AquaticGreen),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mark Delivered", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    "delivered" -> {
                        Surface(
                            color = AquaticGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AquaticGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Order Completed & Delivered", color = AquaticGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    "cancelled" -> {
                        Surface(
                            color = Color(0xFFEF4444).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Order Cancelled", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(8.dp))

            // Footer Total & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Payment: ${order.paymentMethod}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Total: ₹${String.format("%.2f", order.totalAmount)}",
                        color = AdminCyan,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onOpenStatusDialog) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Status & Notes",
                            tint = AdminCyan,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Update", color = AdminCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    TextButton(onClick = onInspectJson) {
                        Icon(
                            Icons.Default.Code,
                            contentDescription = "Inspect Firestore Document",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("JSON", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    }

                    TextButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Archive", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminStatusPipelineVisualizer(
    status: String,
    modifier: Modifier = Modifier
) {
    val steps = listOf("Pending", "Confirmed", "Packing", "Shipped", "Delivered")
    val currentStepIndex = when (status.lowercase()) {
        "pending" -> 0
        "confirmed" -> 1
        "packing", "processing" -> 2
        "shipped", "out for delivery", "in transit" -> 3
        "delivered" -> 4
        "cancelled" -> -1
        else -> 0
    }

    if (currentStepIndex == -1) {
        Surface(
            color = Color(0xFFEF4444).copy(alpha = 0.15f),
            shape = RoundedCornerShape(6.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            Text(
                text = "✖ Order marked as Cancelled",
                color = Color(0xFFF87171),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.04f), shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        steps.forEachIndexed { index, label ->
            val isCompleted = index <= currentStepIndex
            val isCurrent = index == currentStepIndex
            val stepColor = when (index) {
                0 -> CoralOrange
                1 -> Color(0xFF0284C7)
                2 -> Color(0xFF7E22CE)
                3 -> Color(0xFF00838F)
                4 -> AquaticGreen
                else -> Color.Gray
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            color = if (isCompleted) stepColor else Color.White.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = label,
                    fontSize = 8.5.sp,
                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Normal,
                    color = if (isCompleted) Color.White else Color.White.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun AdminWebSyncTab(
    webhookLogs: List<String>,
    fcmLogs: List<String>,
    fcmToken: String?,
    orders: List<OrderEntity>,
    onSendTestFcm: (String, String) -> Unit
) {
    var testPushTitle by remember { mutableStateOf("🐠 Order Update: Fish Garden") }
    var testPushBody by remember { mutableStateOf("Your aquatic livestock order #FG-8821 has been packed in insulated oxygen containers!") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Firestore & FCM Header Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = AdminSlate)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Sensors,
                            contentDescription = null,
                            tint = AquaticGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Website Admin & FCM Push Integration",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Firestore Collection: /${FirestoreHelper.COLLECTION_ORDERS}",
                        color = AdminCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Firestore Status: 🟢 ACTIVE (Real-time snapshot listener on 'orders')",
                        color = AquaticGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "FCM Service: 🔔 ACTIVE (FishGardenMessagingService / Android 13+ channels ready)",
                        color = AdminCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (fcmToken != null) "FCM Device Token: ${fcmToken.take(24)}...${fcmToken.takeLast(8)}" else "FCM Registration: Generating device token...",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Test FCM Push Notification Dispatcher
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = AdminSlate),
                border = androidx.compose.foundation.BorderStroke(1.dp, AdminCyan.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = AdminCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Test FCM Push Notification Dispatch",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = testPushTitle,
                        onValueChange = { testPushTitle = it },
                        label = { Text("Push Notification Title", color = AdminCyan, fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AdminCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = testPushBody,
                        onValueChange = { testPushBody = it },
                        label = { Text("Push Notification Body", color = AdminCyan, fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AdminCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onSendTestFcm(testPushTitle, testPushBody) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AdminCyan),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = AdminSlateHeader, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send Test Push to Customer App", color = AdminSlateHeader, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Live Event Stream (Firestore + FCM)
        item {
            Text(
                text = "Live FCM & Firestore Event Stream",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        item {
            val allLogs = (fcmLogs + webhookLogs).distinct()

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF060911)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (allLogs.isEmpty()) {
                        Text(
                            text = "Waiting for customer orders or FCM push triggers...",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        allLogs.take(15).forEach { log ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    text = if (log.contains("FCM")) "🔔" else "⚡",
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = log,
                                    color = when {
                                        log.contains("FCM") -> AdminCyan
                                        log.contains("REAL-TIME") || log.contains("FIRESTORE") -> AquaticGreen
                                        else -> Color(0xFFCBD5E1)
                                    },
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Sample Order & FCM JSON Payload",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        item {
            val sampleJson = if (orders.isNotEmpty()) {
                val o = orders.first()
                """{
  "orderNumber": "${o.orderNumber}",
  "customerName": "${o.customerName}",
  "customerPhone": "${o.customerPhone}",
  "deliveryAddress": "${o.deliveryAddress}",
  "itemsSummary": "${o.itemsSummary}",
  "totalAmount": ${o.totalAmount},
  "orderStatus": "${o.orderStatus}",
  "fcmNotification": {
    "title": "🐠 Order ${o.orderNumber}: ${o.orderStatus}",
    "channelId": "fish_garden_order_updates",
    "priority": "HIGH",
    "data": { "orderNumber": "${o.orderNumber}", "status": "${o.orderStatus}" }
  }
}"""
            } else {
                """{ "message": "No active orders in collection" }"""
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF030712))
            ) {
                Text(
                    text = sampleJson,
                    color = AdminCyan,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}

@Composable
fun AdminGalleryManagerTab(
    galleryItems: List<GalleryItem>,
    onAddNew: () -> Unit,
    onEdit: (GalleryItem) -> Unit,
    onDelete: (GalleryItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Website Gallery Showcase Manager",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Add, edit, or remove showcase aquascapes in Firestore",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = onAddNew,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AdminCyan)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = AdminSlateHeader)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Aquascape", color = AdminSlateHeader, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        items(galleryItems, key = { it.id }) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AdminSlate)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AquariumImage(
                            imageNameOrUrl = item.imageUrl,
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = item.category,
                            color = AdminCyan,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${item.likesCount} Likes • ${item.tankSpecs.take(30)}...",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }

                    IconButton(onClick = { onEdit(item) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AdminCyan)
                    }

                    IconButton(onClick = { onDelete(item) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminInventoryTab(
    products: List<ProductItem>,
    onAddNew: () -> Unit,
    onEdit: (ProductItem) -> Unit,
    onDelete: (ProductItem) -> Unit,
    onStockChange: (ProductItem, Int) -> Unit
) {
    var productToDelete by remember { mutableStateOf<ProductItem?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Aquarium Stock & Inventory",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Manage live species, pricing in ₹, and stock levels in Firestore",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = onAddNew,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AdminCyan)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = AdminSlateHeader)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Product", color = AdminSlateHeader, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        items(products, key = { it.id }) { product ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AdminSlate)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            AquariumImage(
                                imageNameOrUrl = product.imageUrl,
                                contentDescription = product.name,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = product.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if (!product.badge.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = CoralOrange.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = product.badge,
                                            color = CoralOrange,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }

                            if (product.scientificName.isNotBlank()) {
                                Text(
                                    text = product.scientificName,
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = "₹${String.format("%.2f", product.price)}",
                                    color = AdminCyan,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                                if (product.originalPrice != null && product.originalPrice > product.price) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "₹${String.format("%.2f", product.originalPrice)}",
                                        color = Color.White.copy(alpha = 0.4f),
                                        fontSize = 10.5.sp,
                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "• ${product.category}",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(onClick = { onEdit(product) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Product", tint = AdminCyan)
                        }

                        IconButton(onClick = { productToDelete = product }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Product", tint = MaterialTheme.colorScheme.error)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Stock Control Stepper Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Stock: ",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${product.stockQuantity} units in shop",
                                color = when {
                                    product.stockQuantity == 0 -> Color(0xFFEF4444)
                                    product.stockQuantity < 10 -> CoralOrange
                                    else -> AquaticGreen
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (product.stockQuantity > 0) {
                                        onStockChange(product, product.stockQuantity - 1)
                                    }
                                },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Decrease Stock",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            Text(
                                text = "${product.stockQuantity}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )

                            IconButton(
                                onClick = {
                                    onStockChange(product, product.stockQuantity + 1)
                                },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(AdminCyan.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increase Stock",
                                    tint = AdminCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (productToDelete != null) {
        val prod = productToDelete!!
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = {
                Text(
                    text = "Delete Product?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently remove \"${prod.name}\" from the catalog and Firestore inventory?",
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(prod)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Item")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AddEditProductDialog(
    existingProduct: ProductItem?,
    onDismiss: () -> Unit,
    onSave: (
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
    ) -> Unit
) {
    val isEditMode = existingProduct != null

    var name by remember { mutableStateOf(existingProduct?.name ?: "") }
    var scientificName by remember { mutableStateOf(existingProduct?.scientificName ?: "") }
    var category by remember { mutableStateOf(existingProduct?.category ?: "Fishes") }
    var priceText by remember { mutableStateOf(existingProduct?.price?.toString() ?: "15.00") }
    var originalPriceText by remember { mutableStateOf(existingProduct?.originalPrice?.toString() ?: "") }
    var stockQuantityText by remember { mutableStateOf(existingProduct?.stockQuantity?.toString() ?: "20") }
    var description by remember { mutableStateOf(existingProduct?.description ?: "") }
    var careLevel by remember { mutableStateOf(existingProduct?.careLevel ?: "Moderate") }
    var waterParameters by remember { mutableStateOf(existingProduct?.waterParameters ?: "Temp: 24-28°C • pH: 6.5-7.5") }
    var badge by remember { mutableStateOf(existingProduct?.badge ?: "") }
    var selectedImageName by remember { mutableStateOf(existingProduct?.imageUrl ?: "img_betta_fish") }

    val presetImages = listOf(
        "img_betta_fish" to "Betta Fish",
        "img_hero_aquarium" to "Planted Tank",
        "img_aquascape_setup" to "Aquascape Setup",
        "img_app_icon" to "Aquarium Icon"
    )

    val categoryOptions = listOf(
        "Fishes",
        "Plants",
        "Aquarium Tanks",
        "Food & Nutrition",
        "Filters & Gear"
    )

    val careOptions = listOf("Easy", "Moderate", "Expert", "All Levels")
    val badgeOptions = listOf("None", "HOT", "BESTSELLER", "RARE", "NEW", "SALE")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditMode) "Edit Product" else "Add New Product",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name *") },
                    placeholder = { Text("e.g., Halfmoon Royal Blue Betta") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = scientificName,
                    onValueChange = { scientificName = it },
                    label = { Text("Scientific / Species Name (Optional)") },
                    placeholder = { Text("e.g., Betta splendens") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Chips
                Text(
                    text = "Category:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                CategoryFilterRow(
                    categories = categoryOptions,
                    selectedCategory = category,
                    onCategorySelected = { category = it }
                )

                // Price and Original Price in Rupee (₹)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Price (₹) *") },
                        placeholder = { Text("e.g. 150.00") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = originalPriceText,
                        onValueChange = { originalPriceText = it },
                        label = { Text("Original Price (₹)") },
                        placeholder = { Text("e.g. 180.00") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Stock Quantity
                OutlinedTextField(
                    value = stockQuantityText,
                    onValueChange = { stockQuantityText = it },
                    label = { Text("Stock Quantity (Units) *") },
                    placeholder = { Text("e.g. 25") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Care Level Chips
                Text(
                    text = "Care Level:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                CategoryFilterRow(
                    categories = careOptions,
                    selectedCategory = careLevel,
                    onCategorySelected = { careLevel = it }
                )

                OutlinedTextField(
                    value = waterParameters,
                    onValueChange = { waterParameters = it },
                    label = { Text("Water Parameters / Hardware Specs") },
                    placeholder = { Text("e.g., Temp: 24-28°C • pH: 6.5-7.5") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Badge selector
                Text(
                    text = "Product Badge / Tag:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                CategoryFilterRow(
                    categories = badgeOptions,
                    selectedCategory = if (badge.isBlank()) "None" else badge,
                    onCategorySelected = { badge = if (it == "None") "" else it }
                )

                // Preset Image Selector
                Text(
                    text = "Product Image:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetImages.forEach { (imgKey, label) ->
                        val isSelected = selectedImageName == imgKey
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .clickable { selectedImageName = imgKey }
                        ) {
                            Box {
                                AquariumImage(
                                    imageNameOrUrl = imgKey,
                                    contentDescription = label,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Surface(
                                    color = Color.Black.copy(alpha = 0.55f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                ) {
                                    Text(
                                        text = label,
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(2.dp),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description & Care Tips *") },
                    placeholder = { Text("Details on temperament, feeding, and tank size...") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceText.toDoubleOrNull() ?: 0.0
                    val originalPrice = originalPriceText.toDoubleOrNull()
                    val stock = stockQuantityText.toIntOrNull() ?: 10

                    onSave(
                        name.trim(),
                        scientificName.trim(),
                        category,
                        price,
                        originalPrice,
                        stock,
                        careLevel,
                        waterParameters.trim(),
                        description.trim(),
                        selectedImageName,
                        badge
                    )
                },
                enabled = name.isNotBlank() && (priceText.toDoubleOrNull() != null)
            ) {
                Text(if (isEditMode) "Save Changes" else "Add to Inventory")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddEditGalleryDialog(
    existingItem: GalleryItem?,
    onDismiss: () -> Unit,
    onSave: (title: String, category: String, description: String, tankSpecs: String, floraFauna: String, imageUrl: String) -> Unit
) {
    val isEditMode = existingItem != null

    var title by remember { mutableStateOf(existingItem?.title ?: "") }
    var category by remember { mutableStateOf(existingItem?.category ?: "Planted Aquascapes") }
    var description by remember { mutableStateOf(existingItem?.description ?: "") }
    var tankSpecs by remember { mutableStateOf(existingItem?.tankSpecs ?: "") }
    var floraFauna by remember { mutableStateOf(existingItem?.floraFauna ?: "") }
    var selectedImageName by remember { mutableStateOf(existingItem?.imageUrl ?: "img_hero_aquarium") }

    val presetImages = listOf(
        "img_hero_aquarium" to "Planted Tank",
        "img_betta_fish" to "Betta Fish",
        "img_aquascape_setup" to "Nature Aquascape",
        "img_app_icon" to "Emblem Logo"
    )

    val categoryOptions = listOf(
        "Planted Aquascapes",
        "Freshwater",
        "Saltwater",
        "Exotic Fish",
        "Hardscapes",
        "Nano Tanks",
        "Marine Reef",
        "Accessories"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditMode) "Edit Aquascape Entry" else "Add New Aquascape",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Aquascape / Specimen Title *") },
                    placeholder = { Text("e.g., Mountain Stream Iwagumi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category selection chips
                Text(
                    text = "Select Category:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                CategoryFilterRow(
                    categories = categoryOptions,
                    selectedCategory = category,
                    onCategorySelected = { category = it }
                )

                // Photo Selector
                Text(
                    text = "Showcase Image / URL:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetImages.forEach { (imgKey, label) ->
                        val isSelected = selectedImageName == imgKey
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .clickable { selectedImageName = imgKey }
                        ) {
                            Box {
                                AquariumImage(
                                    imageNameOrUrl = imgKey,
                                    contentDescription = label,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Surface(
                                    color = Color.Black.copy(alpha = 0.55f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                ) {
                                    Text(
                                        text = label,
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(2.dp),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = selectedImageName,
                    onValueChange = { selectedImageName = it },
                    label = { Text("Image Asset Name or Web URL") },
                    placeholder = { Text("e.g. img_hero_aquarium or https://...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Story & Description *") },
                    placeholder = { Text("Describe the aquascape styling, light duration, etc.") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = tankSpecs,
                    onValueChange = { tankSpecs = it },
                    label = { Text("Tank Specs & Equipment") },
                    placeholder = { Text("e.g., 90x45x45cm • ADA Amazonia • CO2 • Oase Filter") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = floraFauna,
                    onValueChange = { floraFauna = it },
                    label = { Text("Living Plants & Fish Species") },
                    placeholder = { Text("e.g., Cardinal Tetras, Monte Carlo carpet, Rotala") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title.trim(), category, description.trim(), tankSpecs.trim(), floraFauna.trim(), selectedImageName.trim())
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text(if (isEditMode) "Update Entry" else "Publish to Gallery")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ChangeAdminPasswordDialog(
    onDismiss: () -> Unit,
    onSave: (currentPin: String, newPin: String) -> Result<Unit>,
    onResetDefault: () -> Unit
) {
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCurrentPin by remember { mutableStateOf(false) }
    var showNewPin by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = AdminCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Change Admin Passcode", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Update the 4-digit security PIN used to access the Fish Garden Admin Console.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = currentPin,
                    onValueChange = {
                        if (it.length <= 8) currentPin = it
                        errorMessage = null
                    },
                    label = { Text("Current PIN *") },
                    placeholder = { Text("Enter current PIN (default: 1234)") },
                    singleLine = true,
                    visualTransformation = if (showCurrentPin) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showCurrentPin = !showCurrentPin }) {
                            Icon(
                                imageVector = if (showCurrentPin) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (showCurrentPin) "Hide PIN" else "Show PIN"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newPin,
                    onValueChange = {
                        if (it.length <= 8) newPin = it
                        errorMessage = null
                    },
                    label = { Text("New PIN (min 4 digits) *") },
                    placeholder = { Text("e.g. 8888") },
                    singleLine = true,
                    visualTransformation = if (showNewPin) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showNewPin = !showNewPin }) {
                            Icon(
                                imageVector = if (showNewPin) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (showNewPin) "Hide PIN" else "Show PIN"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = {
                        if (it.length <= 8) confirmPin = it
                        errorMessage = null
                    },
                    label = { Text("Confirm New PIN *") },
                    placeholder = { Text("Re-enter new PIN") },
                    singleLine = true,
                    visualTransformation = if (showNewPin) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                TextButton(
                    onClick = {
                        onResetDefault()
                        onDismiss()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = Icons.Default.LockReset,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset to Default (1234)", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (currentPin.isBlank()) {
                        errorMessage = "Please enter your current PIN"
                        return@Button
                    }
                    if (newPin.length < 4) {
                        errorMessage = "New PIN must be at least 4 digits/characters"
                        return@Button
                    }
                    if (newPin != confirmPin) {
                        errorMessage = "New PIN and confirmation do not match"
                        return@Button
                    }
                    val result = onSave(currentPin, newPin)
                    if (result.isFailure) {
                        errorMessage = result.exceptionOrNull()?.message ?: "Incorrect current PIN"
                    }
                },
                enabled = currentPin.isNotBlank() && newPin.isNotBlank() && confirmPin.isNotBlank()
            ) {
                Text("Update Passcode")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

