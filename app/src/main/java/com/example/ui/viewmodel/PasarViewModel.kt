package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CartItem
import com.example.data.model.Pedagang
import com.example.data.model.Pesanan
import com.example.data.model.Produk
import com.example.data.model.formatRupiah
import com.example.data.model.normalizeWhatsapp
import com.example.data.repository.PasarRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class PasarViewModel(application: Application) : AndroidViewModel(application) {
    val repository = PasarRepository(application.applicationContext)

    // Navigation current tab
    val currentTab = MutableStateFlow(0) // 0: Publik Katalog, 1: Kios Pedagang, 2: Keranjang, 3: Ruang Pedagang, 4: Admin

    // Search and filter
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("Semua")
    val selectedPedagangFilter = MutableStateFlow<String?>(null)

    // Cart: Map from product ID to CartItem
    private val _cart = MutableStateFlow<Map<String, CartItem>>(emptyMap())
    val cart: StateFlow<Map<String, CartItem>> = _cart.asStateFlow()

    // Admin authentication state
    val isAdminLoggedIn = MutableStateFlow(false)
    val adminPasswordInput = MutableStateFlow("")
    val adminErrorMsg = MutableStateFlow<String?>(null)

    // Pedagang authentication state
    val selectedPedagangForLogin = MutableStateFlow<Pedagang?>(null)
    val pedagangPinInput = MutableStateFlow("")
    val pedagangLoginError = MutableStateFlow<String?>(null)

    // In-app order alert banner
    val activeOrderAlert = MutableStateFlow<Pesanan?>(null)

    // Combined filtered products
    val filteredProducts: StateFlow<List<Produk>> = combine(
        repository.produkList,
        searchQuery,
        selectedCategory,
        selectedPedagangFilter
    ) { products, query, category, pedagangId ->
        products.filter { prod ->
            val matchQuery = query.isBlank() ||
                    prod.nama.contains(query, ignoreCase = true) ||
                    prod.namaPedagang.contains(query, ignoreCase = true) ||
                    prod.deskripsi.contains(query, ignoreCase = true)
            val matchCategory = category == "Semua" || prod.kategori == category
            val matchPedagang = pedagangId == null || prod.pedagangId == pedagangId
            matchQuery && matchCategory && matchPedagang
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cart calculations
    val cartTotal: StateFlow<Double> = _cart.combine(_cart) { cartMap, _ ->
        cartMap.values.sumOf { it.subtotal }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartTotalCount: StateFlow<Int> = _cart.combine(_cart) { cartMap, _ ->
        cartMap.values.sumOf { it.jumlah }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        // Listen for new order events to trigger sound & vibration
        viewModelScope.launch {
            repository.newOrderNotification.collect { order ->
                triggerNotificationEffects(order)
            }
        }

        // Periodic background poll if Google Sheets URL is set (every 30 seconds)
        viewModelScope.launch {
            while (isActive) {
                delay(30000)
                if (repository.webAppUrl.value.isNotBlank()) {
                    repository.syncFromGoogleSheets()
                }
            }
        }
    }

    private fun triggerNotificationEffects(order: Pesanan) {
        activeOrderAlert.value = order

        try {
            // Audio tone alert
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 500)
        } catch (_: Exception) {}

        try {
            // Vibration
            val context = getApplication<Application>().applicationContext
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 300), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 300), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 300, 200, 300), -1)
                }
            }
        } catch (_: Exception) {}
    }

    fun dismissAlert() {
        activeOrderAlert.value = null
    }

    // CART ACTIONS
    fun addToCart(produk: Produk) {
        val current = _cart.value.toMutableMap()
        val existing = current[produk.id]
        if (existing != null) {
            current[produk.id] = existing.copy(jumlah = existing.jumlah + 1)
        } else {
            current[produk.id] = CartItem(
                produkId = produk.id,
                namaProduk = produk.nama,
                pedagangId = produk.pedagangId,
                namaPedagang = produk.namaPedagang,
                nomorKios = produk.nomorKios,
                whatsappPedagang = produk.whatsappPedagang,
                harga = produk.harga,
                satuan = produk.satuan,
                iconEmoji = produk.iconEmoji,
                jumlah = 1
            )
        }
        _cart.value = current
    }

    fun decreaseCartItem(produkId: String) {
        val current = _cart.value.toMutableMap()
        val existing = current[produkId] ?: return
        if (existing.jumlah > 1) {
            current[produkId] = existing.copy(jumlah = existing.jumlah - 1)
        } else {
            current.remove(produkId)
        }
        _cart.value = current
    }

    fun removeCartItem(produkId: String) {
        val current = _cart.value.toMutableMap()
        current.remove(produkId)
        _cart.value = current
    }

    fun clearCart() {
        _cart.value = emptyMap()
    }

    // CHECKOUT & WHATSAPP TRANSACTION
    fun checkoutViaWhatsapp(
        context: Context,
        targetPedagang: Pedagang,
        itemsForPedagang: List<CartItem>,
        pembeliNama: String,
        pembeliWa: String,
        alamat: String,
        tipePengiriman: String,
        catatan: String
    ) {
        val total = itemsForPedagang.sumOf { it.subtotal }
        val order = repository.createOrder(
            pedagang = targetPedagang,
            pembeliNama = pembeliNama,
            pembeliWa = pembeliWa,
            alamat = alamat,
            tipePengiriman = tipePengiriman,
            catatan = catatan,
            items = itemsForPedagang,
            totalHarga = total
        )

        // Remove ordered items from cart
        val remainingCart = _cart.value.toMutableMap()
        itemsForPedagang.forEach { remainingCart.remove(it.produkId) }
        _cart.value = remainingCart

        // Build WhatsApp Message
        val messageBuilder = StringBuilder()
        messageBuilder.append("🌾 *PESANAN BARU - PASAR DESA LUMBUNG KAUH* 🌾\n")
        messageBuilder.append("Nomor Order: *${order.id}*\n")
        messageBuilder.append("Waktu: ${order.waktuFormat}\n\n")
        messageBuilder.append("Yth. *${targetPedagang.nama}*\n")
        messageBuilder.append("${targetPedagang.namaKios} (${targetPedagang.nomorKios})\n")
        messageBuilder.append("━━━━━━━━━━━━━━━━━━━━\n")
        messageBuilder.append("*DAFTAR BELANJA:*\n")
        itemsForPedagang.forEachIndexed { idx, item ->
            messageBuilder.append("${idx + 1}. ${item.namaProduk} (${item.jumlah} ${item.satuan}) - ${formatRupiah(item.subtotal)}\n")
        }
        messageBuilder.append("━━━━━━━━━━━━━━━━━━━━\n")
        messageBuilder.append("*TOTAL HARGA:* *${formatRupiah(total)}*\n\n")
        messageBuilder.append("*DATA PEMBELI:*\n")
        messageBuilder.append("Nama: ${pembeliNama.ifBlank { "Warga Lumbung Kauh" }}\n")
        messageBuilder.append("No. WhatsApp: ${pembeliWa.ifBlank { "-" }}\n")
        messageBuilder.append("Metode: *${tipePengiriman}*\n")
        if (alamat.isNotBlank()) {
            messageBuilder.append("Alamat/Tujuan: $alamat\n")
        }
        if (catatan.isNotBlank()) {
            messageBuilder.append("Catatan: $catatan\n")
        }
        messageBuilder.append("\n_Pesanan ini tercatat otomatis di Google Sheets Pasar Desa Lumbung Kauh._\n")
        messageBuilder.append("Mohon konfirmasi pesanan ini nggih, suksma! 🙏")

        val phone = normalizeWhatsapp(targetPedagang.whatsapp)
        sendWhatsappMessage(context, phone, messageBuilder.toString())
    }

    // PEDAGANG ACTIONS
    fun loginPedagang(pedagang: Pedagang, pin: String): Boolean {
        if (pedagang.pin == pin.trim()) {
            repository.setCurrentPedagang(pedagang)
            pedagangLoginError.value = null
            pedagangPinInput.value = ""
            return true
        } else {
            pedagangLoginError.value = "PIN salah! Silakan periksa kembali."
            return false
        }
    }

    fun logoutPedagang() {
        repository.setCurrentPedagang(null)
        selectedPedagangForLogin.value = null
        pedagangPinInput.value = ""
    }

    fun updateOrderStatus(orderId: String, newStatus: String) {
        repository.updateOrderStatus(orderId, newStatus)
    }

    fun sendOrderUpdateToBuyer(context: Context, order: Pesanan, statusNote: String) {
        val buyerPhone = normalizeWhatsapp(order.whatsappPembeli)
        if (buyerPhone.isBlank()) {
            Toast.makeText(context, "Nomor WhatsApp pembeli tidak tersedia", Toast.LENGTH_SHORT).show()
            return
        }

        val msg = """
Halo *${order.namaPembeli}*,
Update pesanan Anda *#${order.id}* di *${order.namaPedagang} (${order.nomorKios})* - Pasar Desa Lumbung Kauh:

Status: *${order.status}*
$statusNote

Detail Belanja:
${order.itemsSummary}
Total: ${formatRupiah(order.totalHarga)}

Suksma telah berbelanja di Pasar Desa Lumbung Kauh! 🙏
        """.trimIndent()

        sendWhatsappMessage(context, buyerPhone, msg)
    }

    fun addNewProduct(
        nama: String,
        kategori: String,
        harga: Double,
        satuan: String,
        deskripsi: String,
        emoji: String
    ) {
        val currentPed = repository.currentPedagang.value ?: return
        val newProd = Produk(
            id = "prod_${System.currentTimeMillis() % 100000}",
            pedagangId = currentPed.id,
            namaPedagang = currentPed.nama,
            namaKios = currentPed.namaKios,
            nomorKios = currentPed.nomorKios,
            whatsappPedagang = currentPed.whatsapp,
            nama = nama,
            kategori = kategori,
            harga = harga,
            satuan = satuan,
            stokTersedia = true,
            deskripsi = deskripsi,
            iconEmoji = emoji
        )
        repository.addProduk(newProd)
    }

    fun toggleProductStock(product: Produk) {
        repository.updateProduk(product.copy(stokTersedia = !product.stokTersedia))
    }

    fun deleteProduct(productId: String) {
        repository.deleteProduk(productId)
    }

    // ADMIN ACTIONS
    fun loginAdmin(password: String): Boolean {
        if (password == repository.adminPassword.value) {
            isAdminLoggedIn.value = true
            adminErrorMsg.value = null
            adminPasswordInput.value = ""
            return true
        } else {
            adminErrorMsg.value = "Password Admin salah!"
            return false
        }
    }

    fun logoutAdmin() {
        isAdminLoggedIn.value = false
        adminPasswordInput.value = ""
    }

    fun addNewPedagang(
        nama: String,
        namaKios: String,
        nomorKios: String,
        whatsapp: String,
        pin: String,
        kategoriUtama: String,
        jamBuka: String,
        catatan: String
    ) {
        val newPed = Pedagang(
            id = "ped_${System.currentTimeMillis() % 100000}",
            nama = nama,
            namaKios = namaKios,
            nomorKios = nomorKios,
            whatsapp = whatsapp,
            pin = pin.ifBlank { "1234" },
            kategoriUtama = kategoriUtama,
            isActive = true,
            jamBuka = jamBuka.ifBlank { "05:00 - 13:00 WITA" },
            catatan = catatan
        )
        repository.addPedagang(newPed)
    }

    fun togglePedagangActive(pedagang: Pedagang) {
        repository.updatePedagang(pedagang.copy(isActive = !pedagang.isActive))
    }

    fun deletePedagang(pedagangId: String) {
        repository.deletePedagang(pedagangId)
    }

    fun saveWebAppUrl(url: String) {
        repository.setWebAppUrl(url)
    }

    fun triggerSync() {
        repository.syncFromGoogleSheets()
    }

    fun testConnection(onResult: (Boolean, String) -> Unit) {
        repository.testConnection(onResult)
    }

    private fun sendWhatsappMessage(context: Context, cleanPhone: String, message: String) {
        try {
            val encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.toString())
            val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "WhatsApp tidak terpasang atau gagal membuka link", Toast.LENGTH_LONG).show()
        }
    }
}
