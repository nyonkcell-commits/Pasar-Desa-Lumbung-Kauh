package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.CartItem
import com.example.data.model.Pedagang
import com.example.data.model.Pesanan
import com.example.data.model.Produk
import com.example.data.model.formatWaktuIndo
import com.example.data.network.GoogleSheetsApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class PasarRepository(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pasar_lumbung_kauh_prefs", Context.MODE_PRIVATE)
    private val api = GoogleSheetsApi()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _pedagangList = MutableStateFlow<List<Pedagang>>(emptyList())
    val pedagangList: StateFlow<List<Pedagang>> = _pedagangList.asStateFlow()

    private val _produkList = MutableStateFlow<List<Produk>>(emptyList())
    val produkList: StateFlow<List<Produk>> = _produkList.asStateFlow()

    private val _pesananList = MutableStateFlow<List<Pesanan>>(emptyList())
    val pesananList: StateFlow<List<Pesanan>> = _pesananList.asStateFlow()

    private val _webAppUrl = MutableStateFlow(prefs.getString("web_app_url", "") ?: "")
    val webAppUrl: StateFlow<String> = _webAppUrl.asStateFlow()

    private val _adminPassword = MutableStateFlow(prefs.getString("admin_password", "admin123") ?: "admin123")
    val adminPassword: StateFlow<String> = _adminPassword.asStateFlow()

    private val _currentPedagang = MutableStateFlow<Pedagang?>(null)
    val currentPedagang: StateFlow<Pedagang?> = _currentPedagang.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncStatus = MutableStateFlow("")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    // Event bus for new order audio & visual notification
    private val _newOrderNotification = MutableSharedFlow<Pesanan>(extraBufferCapacity = 10)
    val newOrderNotification: SharedFlow<Pesanan> = _newOrderNotification.asSharedFlow()

    init {
        loadLocalData()
        // Restore pedagang session if saved
        val savedPedId = prefs.getString("active_pedagang_id", null)
        if (!savedPedId.isNullOrEmpty()) {
            _currentPedagang.value = _pedagangList.value.find { it.id == savedPedId }
        }
    }

    private fun loadLocalData() {
        val savedPed = prefs.getString("local_pedagang_json", null)
        val savedProd = prefs.getString("local_produk_json", null)
        val savedPes = prefs.getString("local_pesanan_json", null)

        if (savedPed.isNullOrEmpty() || savedProd.isNullOrEmpty()) {
            seedDefaultMarketData()
        } else {
            try {
                _pedagangList.value = parsePedagangJson(savedPed)
                _produkList.value = parseProdukJson(savedProd)
                _pesananList.value = if (!savedPes.isNullOrEmpty()) parsePesananJson(savedPes) else emptyList()
            } catch (e: Exception) {
                Log.e("PasarRepository", "Failed parsing cache, reseeding", e)
                seedDefaultMarketData()
            }
        }
    }

    private fun seedDefaultMarketData() {
        val defaultPedagang = listOf(
            Pedagang(
                id = "ped_1",
                nama = "Ni Ketut Suriati",
                namaKios = "Warung Sayur Bu Ketut",
                nomorKios = "Kios Blok A-01",
                whatsapp = "6281234567890",
                pin = "1234",
                kategoriUtama = "Sayur & Bumbu",
                isActive = true,
                jamBuka = "05:00 - 12:00 WITA",
                catatan = "Menjual aneka sayuran segar panen petani Lumbung Kauh"
            ),
            Pedagang(
                id = "ped_2",
                nama = "I Made Widana",
                namaKios = "Kios Buah Organik Sari Kauh",
                nomorKios = "Kios Blok A-03",
                whatsapp = "6281987654321",
                pin = "2233",
                kategoriUtama = "Buah Segar",
                isActive = true,
                jamBuka = "06:00 - 15:00 WITA",
                catatan = "Buah-buahan segar lokal Tabanan dan Bali manis alami"
            ),
            Pedagang(
                id = "ped_3",
                nama = "Ni Wayan Murni",
                namaKios = "Sembako Beras Sari Pertiwi",
                nomorKios = "Kios Blok B-05",
                whatsapp = "6285712345678",
                pin = "4321",
                kategoriUtama = "Sembako",
                isActive = true,
                jamBuka = "06:00 - 17:00 WITA",
                catatan = "Beras Lumbung Kauh, minyak kelapa, bumbu dapur & gula Bali"
            ),
            Pedagang(
                id = "ped_4",
                nama = "I Nyoman Arta",
                namaKios = "Daging & Ikan Segar Tabanan",
                nomorKios = "Kios Blok C-02",
                whatsapp = "6282134567891",
                pin = "5678",
                kategoriUtama = "Daging & Ikan",
                isActive = true,
                jamBuka = "05:00 - 11:30 WITA",
                catatan = "Daging ayam kampung, babi segar higienis & ikan laut segar"
            ),
            Pedagang(
                id = "ped_5",
                nama = "Men Rai",
                namaKios = "Warung Jaje Bali & Tipat Cantok",
                nomorKios = "Kios Blok D-01",
                whatsapp = "6287812345678",
                pin = "9999",
                kategoriUtama = "Kuliner Bali",
                isActive = true,
                jamBuka = "06:30 - 14:00 WITA",
                catatan = "Tipat cantok bumbu kacang gurih, laklak bali & bubur sumsum"
            )
        )

        val defaultProduk = listOf(
            Produk(
                id = "prod_1",
                pedagangId = "ped_1",
                namaPedagang = "Ni Ketut Suriati",
                nomorKios = "Kios Blok A-01",
                whatsappPedagang = "6281234567890",
                nama = "Kangkung Lombok Lumbung Segar",
                kategori = "Sayur & Bumbu",
                harga = 4000.0,
                satuan = "ikat",
                stokTersedia = true,
                deskripsi = "Sayur kangkung hijau segar langsung petik pagi hari di persawahan Lumbung Kauh.",
                iconEmoji = "🥬"
            ),
            Produk(
                id = "prod_2",
                pedagangId = "ped_1",
                namaPedagang = "Ni Ketut Suriati",
                nomorKios = "Kios Blok A-01",
                whatsappPedagang = "6281234567890",
                nama = "Cabai Rawit Merah Tabanan Super",
                kategori = "Sayur & Bumbu",
                harga = 18000.0,
                satuan = "250 gram",
                stokTersedia = true,
                deskripsi = "Cabai rawit merah pedas mantap asli petani lereng Batukaru Tabanan.",
                iconEmoji = "🌶️"
            ),
            Produk(
                id = "prod_3",
                pedagangId = "ped_1",
                namaPedagang = "Ni Ketut Suriati",
                nomorKios = "Kios Blok A-01",
                whatsappPedagang = "6281234567890",
                nama = "Bawang Merah Bali Kering",
                kategori = "Sayur & Bumbu",
                harga = 32000.0,
                satuan = "kg",
                stokTersedia = true,
                deskripsi = "Bawang merah aromatik kualitas nomor 1, kering sempurna dan tahan lama.",
                iconEmoji = "🧅"
            ),
            Produk(
                id = "prod_4",
                pedagangId = "ped_1",
                namaPedagang = "Ni Ketut Suriati",
                nomorKios = "Kios Blok A-01",
                whatsappPedagang = "6281234567890",
                nama = "Tomat Merah Segar",
                kategori = "Sayur & Bumbu",
                harga = 12000.0,
                satuan = "kg",
                stokTersedia = true,
                deskripsi = "Tomat ranum segar padat berisi untuk sambal dan jus.",
                iconEmoji = "🍅"
            ),
            Produk(
                id = "prod_5",
                pedagangId = "ped_2",
                namaPedagang = "I Made Widana",
                nomorKios = "Kios Blok A-03",
                whatsappPedagang = "6281987654321",
                nama = "Pisang Saba Lumbung Matang Pohon",
                kategori = "Buah Segar",
                harga = 15000.0,
                satuan = "sisir",
                stokTersedia = true,
                deskripsi = "Pisang saba manis legit sangat cocok untuk gorengan, kolak, atau sesaji upacara.",
                iconEmoji = "🍌"
            ),
            Produk(
                id = "prod_6",
                pedagangId = "ped_2",
                namaPedagang = "I Made Widana",
                nomorKios = "Kios Blok A-03",
                whatsappPedagang = "6281987654321",
                nama = "Pepaya California Manis",
                kategori = "Buah Segar",
                harga = 10000.0,
                satuan = "kg",
                stokTersedia = true,
                deskripsi = "Pepaya daging merah jingga tebal, manis segar kaya vitamin.",
                iconEmoji = "🍈"
            ),
            Produk(
                id = "prod_7",
                pedagangId = "ped_2",
                namaPedagang = "I Made Widana",
                nomorKios = "Kios Blok A-03",
                whatsappPedagang = "6281987654321",
                nama = "Jeruk Kintamani Bali",
                kategori = "Buah Segar",
                harga = 22000.0,
                satuan = "kg",
                stokTersedia = true,
                deskripsi = "Jeruk manis banyak air khas dataran tinggi Bali.",
                iconEmoji = "🍊"
            ),
            Produk(
                id = "prod_8",
                pedagangId = "ped_3",
                namaPedagang = "Ni Wayan Murni",
                nomorKios = "Kios Blok B-05",
                whatsappPedagang = "6285712345678",
                nama = "Beras Merah Lumbung Kauh Organik",
                kategori = "Sembako",
                harga = 75000.0,
                satuan = "5 kg",
                stokTersedia = true,
                deskripsi = "Beras merah khas persawahan terasering Lumbung Kauh, pulen bernutrisi tinggi.",
                iconEmoji = "🌾"
            ),
            Produk(
                id = "prod_9",
                pedagangId = "ped_3",
                namaPedagang = "Ni Wayan Murni",
                nomorKios = "Kios Blok B-05",
                whatsappPedagang = "6285712345678",
                nama = "Minyak Kelapa Tandusan Tradisional",
                kategori = "Kebutuhan Dapur",
                harga = 30000.0,
                satuan = "botol 600ml",
                stokTersedia = true,
                deskripsi = "Minyak kelapa murni olahan tradisional wangi khas masakan Bali.",
                iconEmoji = "🥥"
            ),
            Produk(
                id = "prod_10",
                pedagangId = "ped_4",
                namaPedagang = "I Nyoman Arta",
                nomorKios = "Kios Blok C-02",
                whatsappPedagang = "6282134567891",
                nama = "Daging Ayam Broiler Potong Segar",
                kategori = "Daging & Ikan",
                harga = 38000.0,
                satuan = "kg",
                stokTersedia = true,
                deskripsi = "Daging ayam segar dipotong pagi hari, bersih higienis bebas pengawet.",
                iconEmoji = "🍗"
            ),
            Produk(
                id = "prod_11",
                pedagangId = "ped_4",
                namaPedagang = "I Nyoman Arta",
                nomorKios = "Kios Blok C-02",
                whatsappPedagang = "6282134567891",
                nama = "Telur Ayam Kampung Asli",
                kategori = "Daging & Ikan",
                harga = 25000.0,
                satuan = "10 butir",
                stokTersedia = true,
                deskripsi = "Telur ayam kampung segar dari peternakan rumahan warga Lumbung Kauh.",
                iconEmoji = "🥚"
            ),
            Produk(
                id = "prod_12",
                pedagangId = "ped_5",
                namaPedagang = "Men Rai",
                nomorKios = "Kios Blok D-01",
                whatsappPedagang = "6287812345678",
                nama = "Tipat Cantok Bumbu Kacang Khas Lumbung",
                kategori = "Kuliner Bali",
                harga = 12000.0,
                satuan = "porsi",
                stokTersedia = true,
                deskripsi = "Ketupat lembut dengan sayuran segar dilumuri bumbu kacang ulek pedas gurih.",
                iconEmoji = "🍛"
            ),
            Produk(
                id = "prod_13",
                pedagangId = "ped_5",
                namaPedagang = "Men Rai",
                nomorKios = "Kios Blok D-01",
                whatsappPedagang = "6287812345678",
                nama = "Jaje Laklak Bali Gula Juruh",
                kategori = "Kuliner Bali",
                harga = 10000.0,
                satuan = "kotak (5 pcs)",
                stokTersedia = true,
                deskripsi = "Kue laklak beras harum daun suji disajikan parutan kelapa & gula aren cair.",
                iconEmoji = "🥞"
            )
        )

        val defaultPesanan = listOf(
            Pesanan(
                id = "PLK-10821",
                pedagangId = "ped_1",
                namaPedagang = "Ni Ketut Suriati",
                nomorKios = "Kios Blok A-01",
                whatsappPedagang = "6281234567890",
                namaPembeli = "I Wayan Arnawa",
                whatsappPembeli = "081298765432",
                alamatPengantaran = "Banjar Dinas Kauh (Dekat Bale Banjar)",
                tipePengiriman = "Antar ke Rumah",
                catatan = "Tolong kirimkan pagi sebelum jam 9 nggih bu",
                itemsSummary = "Kangkung (2 ikat), Cabai Rawit (1/4 kg), Tomat (1 kg)",
                totalHarga = 38000.0,
                status = Pesanan.STATUS_MENUNGGU,
                waktuPesanan = System.currentTimeMillis() - 15 * 60 * 1000,
                waktuFormat = formatWaktuIndo(System.currentTimeMillis() - 15 * 60 * 1000)
            ),
            Pesanan(
                id = "PLK-10819",
                pedagangId = "ped_5",
                namaPedagang = "Men Rai",
                nomorKios = "Kios Blok D-01",
                whatsappPedagang = "6287812345678",
                namaPembeli = "Ni Luh Made Ari",
                whatsappPembeli = "081987112233",
                alamatPengantaran = "Pasar Lumbung Kauh",
                tipePengiriman = "Ambil di Kios Pasar",
                catatan = "Bumbu cantok cabai 3 sedang",
                itemsSummary = "Tipat Cantok (2 porsi), Jaje Laklak (1 kotak)",
                totalHarga = 34000.0,
                status = Pesanan.STATUS_DIPROSES,
                waktuPesanan = System.currentTimeMillis() - 45 * 60 * 1000,
                waktuFormat = formatWaktuIndo(System.currentTimeMillis() - 45 * 60 * 1000)
            )
        )

        _pedagangList.value = defaultPedagang
        _produkList.value = defaultProduk
        _pesananList.value = defaultPesanan
        saveLocalData()
    }

    private fun saveLocalData() {
        val editor = prefs.edit()
        editor.putString("local_pedagang_json", serializePedagangList(_pedagangList.value))
        editor.putString("local_produk_json", serializeProdukList(_produkList.value))
        editor.putString("local_pesanan_json", serializePesananList(_pesananList.value))
        editor.apply()
    }

    fun setWebAppUrl(url: String) {
        _webAppUrl.value = url.trim()
        prefs.edit().putString("web_app_url", url.trim()).apply()
    }

    fun setAdminPassword(password: String) {
        _adminPassword.value = password
        prefs.edit().putString("admin_password", password).apply()
    }

    fun setCurrentPedagang(pedagang: Pedagang?) {
        _currentPedagang.value = pedagang
        prefs.edit().putString("active_pedagang_id", pedagang?.id ?: "").apply()
    }

    fun syncFromGoogleSheets() {
        val url = _webAppUrl.value
        if (url.isBlank()) {
            _syncStatus.value = "URL Google Apps Script belum diisi di Pengaturan Admin."
            return
        }

        coroutineScope.launch {
            _isSyncing.value = true
            _syncStatus.value = "Menghubungkan ke Google Sheets..."

            val result = api.fetchAllData(url)
            result.onSuccess { (remotePed, remoteProd, remotePes) ->
                if (remotePed.isNotEmpty()) _pedagangList.value = remotePed
                if (remoteProd.isNotEmpty()) _produkList.value = remoteProd
                
                // Check if any new orders arrived for notification
                val currentIds = _pesananList.value.map { it.id }.toSet()
                if (remotePes.isNotEmpty()) {
                    val newIncoming = remotePes.filter { it.id !in currentIds && it.status == Pesanan.STATUS_MENUNGGU }
                    _pesananList.value = remotePes
                    // trigger notification for the most recent order
                    newIncoming.firstOrNull()?.let {
                        _newOrderNotification.tryEmit(it)
                    }
                }
                
                saveLocalData()
                _syncStatus.value = "Sinkronisasi Berhasil! (${remotePed.size} pedagang, ${remoteProd.size} produk, ${remotePes.size} pesanan)"
            }.onFailure { err ->
                _syncStatus.value = "Gagal sinkronisasi: ${err.localizedMessage}"
            }

            _isSyncing.value = false
        }
    }

    fun testConnection(onResult: (Boolean, String) -> Unit) {
        val url = _webAppUrl.value
        coroutineScope.launch {
            val result = api.testConnection(url)
            result.onSuccess { msg ->
                onResult(true, msg)
            }.onFailure { err ->
                onResult(false, err.localizedMessage ?: "Gagal terhubung ke URL Apps Script")
            }
        }
    }

    fun createOrder(
        pedagang: Pedagang,
        pembeliNama: String,
        pembeliWa: String,
        alamat: String,
        tipePengiriman: String,
        catatan: String,
        items: List<CartItem>,
        totalHarga: Double
    ): Pesanan {
        val orderNum = (10000..99999).random()
        val orderId = "PLK-$orderNum"
        val itemsSummary = items.joinToString(", ") { "${it.namaProduk} (${it.jumlah} ${it.satuan})" }
        val now = System.currentTimeMillis()

        val pesanan = Pesanan(
            id = orderId,
            pedagangId = pedagang.id,
            namaPedagang = pedagang.nama,
            nomorKios = pedagang.nomorKios,
            whatsappPedagang = pedagang.whatsapp,
            namaPembeli = pembeliNama,
            whatsappPembeli = pembeliWa,
            alamatPengantaran = alamat,
            tipePengiriman = tipePengiriman,
            catatan = catatan,
            itemsSummary = itemsSummary,
            totalHarga = totalHarga,
            status = Pesanan.STATUS_MENUNGGU,
            waktuPesanan = now,
            waktuFormat = formatWaktuIndo(now)
        )

        // Save locally
        _pesananList.value = listOf(pesanan) + _pesananList.value
        saveLocalData()

        // Notify in-app stream
        _newOrderNotification.tryEmit(pesanan)

        // Sync to Google Sheets in background
        val url = _webAppUrl.value
        if (url.isNotBlank()) {
            coroutineScope.launch {
                api.postOrder(url, pesanan)
            }
        }

        return pesanan
    }

    fun updateOrderStatus(orderId: String, newStatus: String) {
        _pesananList.value = _pesananList.value.map { order ->
            if (order.id == orderId) order.copy(status = newStatus) else order
        }
        saveLocalData()

        val url = _webAppUrl.value
        if (url.isNotBlank()) {
            coroutineScope.launch {
                api.updateOrderStatus(url, orderId, newStatus)
            }
        }
    }

    fun addProduk(produk: Produk) {
        _produkList.value = _produkList.value + produk
        saveLocalData()
        val url = _webAppUrl.value
        if (url.isNotBlank()) {
            coroutineScope.launch {
                api.postProduct(url, produk)
            }
        }
    }

    fun updateProduk(updated: Produk) {
        _produkList.value = _produkList.value.map { if (it.id == updated.id) updated else it }
        saveLocalData()
        val url = _webAppUrl.value
        if (url.isNotBlank()) {
            coroutineScope.launch {
                api.postProduct(url, updated)
            }
        }
    }

    fun deleteProduk(productId: String) {
        _produkList.value = _produkList.value.filter { it.id != productId }
        saveLocalData()
        val url = _webAppUrl.value
        if (url.isNotBlank()) {
            coroutineScope.launch {
                api.deleteProduct(url, productId)
            }
        }
    }

    fun addPedagang(pedagang: Pedagang) {
        _pedagangList.value = _pedagangList.value + pedagang
        saveLocalData()
        val url = _webAppUrl.value
        if (url.isNotBlank()) {
            coroutineScope.launch {
                api.postPedagang(url, pedagang)
            }
        }
    }

    fun updatePedagang(updated: Pedagang) {
        _pedagangList.value = _pedagangList.value.map { if (it.id == updated.id) updated else it }
        if (_currentPedagang.value?.id == updated.id) {
            _currentPedagang.value = updated
        }
        saveLocalData()
        val url = _webAppUrl.value
        if (url.isNotBlank()) {
            coroutineScope.launch {
                api.postPedagang(url, updated)
            }
        }
    }

    fun deletePedagang(pedagangId: String) {
        _pedagangList.value = _pedagangList.value.filter { it.id != pedagangId }
        _produkList.value = _produkList.value.filter { it.pedagangId != pedagangId }
        if (_currentPedagang.value?.id == pedagangId) {
            _currentPedagang.value = null
        }
        saveLocalData()
        val url = _webAppUrl.value
        if (url.isNotBlank()) {
            coroutineScope.launch {
                api.deletePedagang(url, pedagangId)
            }
        }
    }

    // Helper JSON serializations
    private fun serializePedagangList(list: List<Pedagang>): String {
        val arr = JSONArray()
        list.forEach { p ->
            val obj = JSONObject().apply {
                put("id", p.id)
                put("nama", p.nama)
                put("namaKios", p.namaKios)
                put("nomorKios", p.nomorKios)
                put("whatsapp", p.whatsapp)
                put("pin", p.pin)
                put("kategoriUtama", p.kategoriUtama)
                put("isActive", p.isActive)
                put("jamBuka", p.jamBuka)
                put("catatan", p.catatan)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun parsePedagangJson(jsonStr: String): List<Pedagang> {
        val arr = JSONArray(jsonStr)
        val list = mutableListOf<Pedagang>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                Pedagang(
                    id = obj.optString("id"),
                    nama = obj.optString("nama"),
                    namaKios = obj.optString("namaKios"),
                    nomorKios = obj.optString("nomorKios"),
                    whatsapp = obj.optString("whatsapp"),
                    pin = obj.optString("pin", "1234"),
                    kategoriUtama = obj.optString("kategoriUtama", "Sayur & Bumbu"),
                    isActive = obj.optBoolean("isActive", true),
                    jamBuka = obj.optString("jamBuka", "05:00 - 13:00 WITA"),
                    catatan = obj.optString("catatan", "")
                )
            )
        }
        return list
    }

    private fun serializeProdukList(list: List<Produk>): String {
        val arr = JSONArray()
        list.forEach { p ->
            val obj = JSONObject().apply {
                put("id", p.id)
                put("pedagangId", p.pedagangId)
                put("namaPedagang", p.namaPedagang)
                put("namaKios", p.namaKios)
                put("nomorKios", p.nomorKios)
                put("whatsappPedagang", p.whatsappPedagang)
                put("nama", p.nama)
                put("kategori", p.kategori)
                put("harga", p.harga)
                put("satuan", p.satuan)
                put("stokTersedia", p.stokTersedia)
                put("deskripsi", p.deskripsi)
                put("iconEmoji", p.iconEmoji)
                put("fotoUrl", p.fotoUrl)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun parseProdukJson(jsonStr: String): List<Produk> {
        val arr = JSONArray(jsonStr)
        val list = mutableListOf<Produk>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                Produk(
                    id = obj.optString("id"),
                    pedagangId = obj.optString("pedagangId"),
                    namaPedagang = obj.optString("namaPedagang"),
                    namaKios = obj.optString("namaKios"),
                    nomorKios = obj.optString("nomorKios"),
                    whatsappPedagang = obj.optString("whatsappPedagang"),
                    nama = obj.optString("nama"),
                    kategori = obj.optString("kategori", "Sayur & Bumbu"),
                    harga = obj.optDouble("harga", 0.0),
                    satuan = obj.optString("satuan", "kg"),
                    stokTersedia = obj.optBoolean("stokTersedia", true),
                    deskripsi = obj.optString("deskripsi", ""),
                    iconEmoji = obj.optString("iconEmoji", "🥬"),
                    fotoUrl = obj.optString("fotoUrl", "")
                )
            )
        }
        return list
    }

    private fun serializePesananList(list: List<Pesanan>): String {
        val arr = JSONArray()
        list.forEach { p ->
            val obj = JSONObject().apply {
                put("id", p.id)
                put("pedagangId", p.pedagangId)
                put("namaPedagang", p.namaPedagang)
                put("nomorKios", p.nomorKios)
                put("whatsappPedagang", p.whatsappPedagang)
                put("namaPembeli", p.namaPembeli)
                put("whatsappPembeli", p.whatsappPembeli)
                put("alamatPengantaran", p.alamatPengantaran)
                put("tipePengiriman", p.tipePengiriman)
                put("catatan", p.catatan)
                put("itemsSummary", p.itemsSummary)
                put("totalHarga", p.totalHarga)
                put("status", p.status)
                put("waktuPesanan", p.waktuPesanan)
                put("waktuFormat", p.waktuFormat)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun parsePesananJson(jsonStr: String): List<Pesanan> {
        val arr = JSONArray(jsonStr)
        val list = mutableListOf<Pesanan>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                Pesanan(
                    id = obj.optString("id"),
                    pedagangId = obj.optString("pedagangId"),
                    namaPedagang = obj.optString("namaPedagang"),
                    nomorKios = obj.optString("nomorKios"),
                    whatsappPedagang = obj.optString("whatsappPedagang"),
                    namaPembeli = obj.optString("namaPembeli"),
                    whatsappPembeli = obj.optString("whatsappPembeli"),
                    alamatPengantaran = obj.optString("alamatPengantaran"),
                    tipePengiriman = obj.optString("tipePengiriman", "Ambil di Kios Pasar"),
                    catatan = obj.optString("catatan", ""),
                    itemsSummary = obj.optString("itemsSummary"),
                    totalHarga = obj.optDouble("totalHarga", 0.0),
                    status = obj.optString("status", Pesanan.STATUS_MENUNGGU),
                    waktuPesanan = obj.optLong("waktuPesanan", System.currentTimeMillis()),
                    waktuFormat = obj.optString("waktuFormat")
                )
            )
        }
        return list
    }
}
