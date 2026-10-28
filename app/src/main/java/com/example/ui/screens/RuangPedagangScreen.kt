package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MarketCategories
import com.example.data.model.Pedagang
import com.example.data.model.Pesanan
import com.example.data.model.Produk
import com.example.data.model.formatRupiah
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatusBadge
import com.example.ui.viewmodel.PasarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuangPedagangScreen(
    viewModel: PasarViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentPedagang by viewModel.repository.currentPedagang.collectAsState()
    val allPedagang by viewModel.repository.pedagangList.collectAsState()
    val allOrders by viewModel.repository.pesananList.collectAsState()
    val allProducts by viewModel.repository.produkList.collectAsState()

    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: Pesanan Masuk, 1: Kelola Produk
    var showAddProductDialog by remember { mutableStateOf(false) }

    // If no merchant is logged in, show login form
    if (currentPedagang == null) {
        PedagangLoginView(
            allPedagang = allPedagang,
            viewModel = viewModel
        )
    } else {
        val ped = currentPedagang!!
        val myOrders = allOrders.filter { it.pedagangId == ped.id }
        val myProducts = allProducts.filter { it.pedagangId == ped.id }
        val pendingCount = myOrders.count { it.status == Pesanan.STATUS_MENUNGGU }

        Column(
            modifier = modifier
                .fillMaxSize()
                .testTag("ruang_pedagang_screen")
        ) {
            // Header Pedagang
            Card(
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Store,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = ped.namaKios,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "${ped.nama} • ${ped.nomorKios}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.logoutPedagang() },
                        modifier = Modifier.testTag("btn_logout_pedagang")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Keluar Akun",
                            tint = Color.White
                        )
                    }
                }
            }

            // Sub Tab Row
            TabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔔 Pesanan Masuk", fontWeight = FontWeight.Bold)
                            if (pendingCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.error,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "$pendingCount",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    text = {
                        Text("📦 Kelola Produk (${myProducts.size})", fontWeight = FontWeight.Bold)
                    }
                )
            }

            if (selectedSubTab == 0) {
                // PESANAN MASUK
                var filterStatus by remember { mutableStateOf("Semua") }
                val filteredMyOrders = if (filterStatus == "Semua") myOrders else myOrders.filter { it.status == filterStatus }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Filter Chips
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val statuses = listOf("Semua", Pesanan.STATUS_MENUNGGU, Pesanan.STATUS_DIPROSES, Pesanan.STATUS_SIAP, Pesanan.STATUS_SELESAI)
                            items(statuses) { st ->
                                FilterChip(
                                    selected = filterStatus == st,
                                    onClick = { filterStatus = st },
                                    label = { Text(st, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }

                    if (filteredMyOrders.isEmpty()) {
                        item {
                            EmptyStateView(
                                icon = "📭",
                                title = "Belum Ada Pesanan Masuk",
                                description = "Pesanan pembeli via WhatsApp untuk ${ped.namaKios} akan tampil di sini secara realtime."
                            )
                        }
                    } else {
                        items(filteredMyOrders, key = { it.id }) { order ->
                            OrderCardForMerchant(
                                order = order,
                                onUpdateStatus = { newStatus ->
                                    viewModel.updateOrderStatus(order.id, newStatus)
                                },
                                onSendUpdateToBuyer = {
                                    val note = when (order.status) {
                                        Pesanan.STATUS_DIPROSES -> "Barang pesanan Anda sedang kami siapkan di kios."
                                        Pesanan.STATUS_SIAP -> "Barang siap diambil di ${order.nomorKios} Pasar Lumbung Kauh atau sedang dalam pengantaran."
                                        Pesanan.STATUS_SELESAI -> "Pesanan telah selesai. Matur suksma!"
                                        else -> "Pesanan telah kami terima."
                                    }
                                    viewModel.sendOrderUpdateToBuyer(context, order, note)
                                }
                            )
                        }
                    }
                }
            } else {
                // KELOLA PRODUK
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Button(
                            onClick = { showAddProductDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_tambah_produk_modal")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tambah Produk Baru ke Kios", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (myProducts.isEmpty()) {
                        item {
                            EmptyStateView(
                                icon = "🧺",
                                title = "Belum Ada Produk di Kios Ini",
                                description = "Klik tombol di atas untuk menambahkan sayur, buah, atau barang dagangan Anda."
                            )
                        }
                    } else {
                        items(myProducts, key = { it.id }) { prod ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth().testTag("merchant_product_${prod.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = prod.iconEmoji.ifBlank { "🥬" },
                                        fontSize = 32.sp,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = prod.nama,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${formatRupiah(prod.harga)} / ${prod.satuan} • ${prod.kategori}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (prod.deskripsi.isNotBlank()) {
                                            Text(
                                                text = prod.deskripsi,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (prod.stokTersedia) "Tersedia" else "Habis",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (prod.stokTersedia) MaterialTheme.colorScheme.primary else Color.Red,
                                                modifier = Modifier.padding(end = 4.dp)
                                            )
                                            Switch(
                                                checked = prod.stokTersedia,
                                                onCheckedChange = { viewModel.toggleProductStock(prod) },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = MaterialTheme.colorScheme.primary
                                                )
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.deleteProduct(prod.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Hapus Produk",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddProductDialog) {
        AddProductDialog(
            onDismiss = { showAddProductDialog = false },
            onAddProduct = { nama, kategori, harga, satuan, deskripsi, emoji ->
                viewModel.addNewProduct(nama, kategori, harga, satuan, deskripsi, emoji)
                showAddProductDialog = false
            }
        )
    }
}

@Composable
fun OrderCardForMerchant(
    order: Pesanan,
    onUpdateStatus: (String) -> Unit,
    onSendUpdateToBuyer: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth().testTag("order_card_${order.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Pesanan #${order.id}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = order.waktuFormat,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(status = order.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Buyer details
            Text(
                text = "Pembeli: ${order.namaPembeli} (${order.whatsappPembeli.ifBlank { "No WA -" }})",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "Metode: ${order.tipePengiriman}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            if (order.alamatPengantaran.isNotBlank()) {
                Text(
                    text = "Alamat: ${order.alamatPengantaran}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (order.catatan.isNotBlank()) {
                Text(
                    text = "Catatan: \"${order.catatan}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            Text(
                text = "Barang: ${order.itemsSummary}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Total Belanja: ${formatRupiah(order.totalHarga)}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Status Actions & WhatsApp Quick Reply
            Text(
                text = "Ubah Status Pesanan:",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilledTonalButton(
                    onClick = { onUpdateStatus(Pesanan.STATUS_DIPROSES) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp).weight(1f)
                ) {
                    Text("Siapkan", fontSize = 11.sp)
                }

                FilledTonalButton(
                    onClick = { onUpdateStatus(Pesanan.STATUS_SIAP) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp).weight(1f)
                ) {
                    Text("Siap", fontSize = 11.sp)
                }

                FilledTonalButton(
                    onClick = { onUpdateStatus(Pesanan.STATUS_SELESAI) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp).weight(1f)
                ) {
                    Text("Selesai", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onSendUpdateToBuyer,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(38.dp)
            ) {
                Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Kirim Update Status via WhatsApp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedagangLoginView(
    allPedagang: List<Pedagang>,
    viewModel: PasarViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedPedagang by remember { mutableStateOf<Pedagang?>(allPedagang.firstOrNull()) }
    var pin by remember { mutableStateOf("") }
    val errorMsg by viewModel.pedagangLoginError.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth().testTag("pedagang_login_card")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Store,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Ruang Pedagang",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Pasar Desa Lumbung Kauh",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Dropdown Kios
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedPedagang?.let { "${it.namaKios} (${it.nomorKios})" } ?: "Pilih Kios Anda",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pilih Kios Pedagang") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        allPedagang.forEach { ped ->
                            DropdownMenuItem(
                                text = { Text("${ped.namaKios} - ${ped.nama} (${ped.nomorKios})") },
                                onClick = {
                                    selectedPedagang = ped
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("PIN Keamanan Pedagang") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("input_pin_pedagang")
                )

                if (errorMsg != null) {
                    Text(
                        text = errorMsg ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💡 Petunjuk: PIN default adalah 1234 (atau PIN yang dibuat Admin saat mendaftarkan akun).",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val ped = selectedPedagang
                        if (ped != null) {
                            viewModel.loginPedagang(ped, pin)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_login_pedagang_submit")
                ) {
                    Text("Masuk ke Kios Saya", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductDialog(
    onDismiss: () -> Unit,
    onAddProduct: (String, String, Double, String, String, String) -> Unit
) {
    var nama by remember { mutableStateOf("") }
    var kategori by remember { mutableStateOf(MarketCategories.ALL[1]) }
    var hargaStr by remember { mutableStateOf("") }
    var satuan by remember { mutableStateOf("kg") }
    var deskripsi by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("🥬") }

    val emojis = listOf("🥬", "🌶️", "🍅", "🧅", "🍌", "🍊", "🍈", "🌾", "🥥", "🍗", "🥚", "🍛", "🥞")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Produk Baru ke Kios", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = nama,
                        onValueChange = { nama = it },
                        label = { Text("Nama Produk (cth: Kangkung Segar)") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("Pilih Ikon Emoji:", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(emojis) { em ->
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (selectedEmoji == em) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable { selectedEmoji = em },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(em, fontSize = 20.sp)
                            }
                        }
                    }
                }

                item {
                    var expandedCat by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedCat,
                        onExpandedChange = { expandedCat = !expandedCat },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = kategori,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Kategori Produk") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCat) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCat,
                            onDismissRequest = { expandedCat = false }
                        ) {
                            MarketCategories.ALL.filter { it != "Semua" }.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        kategori = cat
                                        expandedCat = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = hargaStr,
                            onValueChange = { hargaStr = it },
                            label = { Text("Harga (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.2f)
                        )

                        OutlinedTextField(
                            value = satuan,
                            onValueChange = { satuan = it },
                            label = { Text("Satuan (kg/ikat)") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = deskripsi,
                        onValueChange = { deskripsi = it },
                        label = { Text("Deskripsi Singkat") },
                        maxLines = 2,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val harga = hargaStr.toDoubleOrNull() ?: 0.0
                    if (nama.isNotBlank() && harga > 0) {
                        onAddProduct(nama, kategori, harga, satuan, deskripsi, selectedEmoji)
                    }
                },
                enabled = nama.isNotBlank() && hargaStr.isNotBlank()
            ) {
                Text("Simpan Produk")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
