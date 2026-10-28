package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MarketTopBar
import com.example.ui.components.OrderAlertBanner
import com.example.ui.screens.KeranjangScreen
import com.example.ui.screens.KiosPedagangScreen
import com.example.ui.screens.PublikKatalogScreen
import com.example.ui.screens.RuangAdminScreen
import com.example.ui.screens.RuangPedagangScreen
import com.example.ui.theme.PasarDesaTheme
import com.example.ui.viewmodel.PasarViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: PasarViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PasarDesaTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: PasarViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val isSyncing by viewModel.repository.isSyncing.collectAsState()
    val activeAlert by viewModel.activeOrderAlert.collectAsState()
    val cartTotalCount by viewModel.cartTotalCount.collectAsState()
    val allOrders by viewModel.repository.pesananList.collectAsState()
    val currentPedagang by viewModel.repository.currentPedagang.collectAsState()

    // Calculate pending orders badge for pedagang
    val pedagangPendingCount = if (currentPedagang != null) {
        allOrders.count { it.pedagangId == currentPedagang!!.id && it.status == com.example.data.model.Pesanan.STATUS_MENUNGGU }
    } else {
        allOrders.count { it.status == com.example.data.model.Pesanan.STATUS_MENUNGGU }
    }

    // Handle back button: if not on Tab 0, go back to Tab 0
    BackHandler(enabled = currentTab != 0) {
        viewModel.currentTab.value = 0
    }

    Scaffold(
        topBar = {
            MarketTopBar(
                title = when (currentTab) {
                    0 -> "Pasar Lumbung Kauh"
                    1 -> "Daftar Kios Pasar"
                    2 -> "Keranjang & Pesanan"
                    3 -> if (currentPedagang != null) currentPedagang!!.namaKios else "Ruang Pedagang"
                    else -> "Pengelola Admin"
                },
                subtitle = when (currentTab) {
                    0 -> "Belanja Segar & Kuliner Desa"
                    1 -> "Kios Resmi Warga Desa"
                    2 -> "Transaksi Langsung via WhatsApp"
                    3 -> "Kelola Kios & Pesanan Masuk"
                    else -> "Database Google Sheet & Akun"
                },
                isSyncing = isSyncing,
                onSyncClick = { viewModel.triggerSync() }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_navigation_bar")
            ) {
                // 1. Katalog
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { viewModel.currentTab.value = 0 },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 0) Icons.Filled.ShoppingBag else Icons.Outlined.ShoppingBag,
                            contentDescription = "Katalog Produk"
                        )
                    },
                    label = { Text("Katalog", fontSize = 11.sp, fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("nav_tab_katalog")
                )

                // 2. Kios Stand
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { viewModel.currentTab.value = 1 },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 1) Icons.Filled.Storefront else Icons.Outlined.Storefront,
                            contentDescription = "Kios Pedagang"
                        )
                    },
                    label = { Text("Kios", fontSize = 11.sp, fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("nav_tab_kios")
                )

                // 3. Keranjang
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { viewModel.currentTab.value = 2 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (cartTotalCount > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                                        Text("$cartTotalCount", color = Color.White)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (currentTab == 2) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart,
                                contentDescription = "Keranjang"
                            )
                        }
                    },
                    label = { Text("Keranjang", fontSize = 11.sp, fontWeight = if (currentTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("nav_tab_cart")
                )

                // 4. Ruang Pedagang
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { viewModel.currentTab.value = 3 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (pedagangPendingCount > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                                        Text("$pedagangPendingCount", color = Color.White)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (currentTab == 3) Icons.Filled.Person else Icons.Outlined.Person,
                                contentDescription = "Pedagang"
                            )
                        }
                    },
                    label = { Text("Pedagang", fontSize = 11.sp, fontWeight = if (currentTab == 3) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("nav_tab_pedagang")
                )

                // 5. Ruang Admin
                NavigationBarItem(
                    selected = currentTab == 4,
                    onClick = { viewModel.currentTab.value = 4 },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 4) Icons.Filled.AdminPanelSettings else Icons.Outlined.AdminPanelSettings,
                            contentDescription = "Admin"
                        )
                    },
                    label = { Text("Admin", fontSize = 11.sp, fontWeight = if (currentTab == 4) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.tertiaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.tertiary,
                        selectedTextColor = MaterialTheme.colorScheme.tertiary
                    ),
                    modifier = Modifier.testTag("nav_tab_admin")
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Realtime Order Alert Banner for new incoming orders
            OrderAlertBanner(
                order = activeAlert,
                onDismiss = { viewModel.dismissAlert() },
                onViewOrder = {
                    viewModel.dismissAlert()
                    viewModel.currentTab.value = 3 // Navigate to Pedagang space
                }
            )

            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "TabContentTransition"
                ) { targetTab ->
                    when (targetTab) {
                        0 -> PublikKatalogScreen(
                            viewModel = viewModel,
                            onNavigateToCart = { viewModel.currentTab.value = 2 }
                        )
                        1 -> KiosPedagangScreen(
                            viewModel = viewModel,
                            onNavigateToCatalogWithFilter = { pedagangId ->
                                viewModel.selectedPedagangFilter.value = pedagangId
                                viewModel.currentTab.value = 0
                            }
                        )
                        2 -> KeranjangScreen(
                            viewModel = viewModel,
                            onNavigateToCatalog = { viewModel.currentTab.value = 0 }
                        )
                        3 -> RuangPedagangScreen(
                            viewModel = viewModel
                        )
                        4 -> RuangAdminScreen(
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}
