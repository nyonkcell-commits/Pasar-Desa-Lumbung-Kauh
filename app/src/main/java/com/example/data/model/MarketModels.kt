package com.example.data.model

import com.squareup.moshi.JsonClass
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@JsonClass(generateAdapter = true)
data class Pedagang(
    val id: String = "",
    val nama: String = "",
    val namaKios: String = "",
    val nomorKios: String = "",
    val whatsapp: String = "",
    val pin: String = "1234",
    val kategoriUtama: String = "Sayur & Bumbu",
    val isActive: Boolean = true,
    val jamBuka: String = "05:00 - 13:00 WITA",
    val catatan: String = "Kios Segar Pasar Desa Lumbung Kauh"
)

@JsonClass(generateAdapter = true)
data class Produk(
    val id: String = "",
    val pedagangId: String = "",
    val namaPedagang: String = "",
    val namaKios: String = "",
    val nomorKios: String = "",
    val whatsappPedagang: String = "",
    val nama: String = "",
    val kategori: String = "Sayur & Bumbu",
    val harga: Double = 0.0,
    val satuan: String = "kg", // kg, ikat, bungkus, porsi, butir, liter
    val stokTersedia: Boolean = true,
    val deskripsi: String = "",
    val iconEmoji: String = "🥬",
    val fotoUrl: String = ""
)

@JsonClass(generateAdapter = true)
data class CartItem(
    val produkId: String = "",
    val namaProduk: String = "",
    val pedagangId: String = "",
    val namaPedagang: String = "",
    val nomorKios: String = "",
    val whatsappPedagang: String = "",
    val harga: Double = 0.0,
    val satuan: String = "kg",
    val iconEmoji: String = "🥬",
    val jumlah: Int = 1,
    val catatan: String = ""
) {
    val subtotal: Double
        get() = harga * jumlah
}

@JsonClass(generateAdapter = true)
data class Pesanan(
    val id: String = "",
    val pedagangId: String = "",
    val namaPedagang: String = "",
    val nomorKios: String = "",
    val whatsappPedagang: String = "",
    val namaPembeli: String = "",
    val whatsappPembeli: String = "",
    val alamatPengantaran: String = "",
    val tipePengiriman: String = "Ambil di Kios Pasar", // "Ambil di Kios Pasar" atau "Antar ke Rumah (Desa Lumbung Kauh)"
    val catatan: String = "",
    val itemsSummary: String = "",
    val totalHarga: Double = 0.0,
    val status: String = STATUS_MENUNGGU,
    val waktuPesanan: Long = System.currentTimeMillis(),
    val waktuFormat: String = ""
) {
    companion object {
        const val STATUS_MENUNGGU = "Menunggu Konfirmasi"
        const val STATUS_DIPROSES = "Sedang Disiapkan"
        const val STATUS_SIAP = "Siap Diambil/Diantar"
        const val STATUS_SELESAI = "Selesai"
        const val STATUS_BATAL = "Dibatalkan"
    }
}

object MarketCategories {
    val ALL = listOf(
        "Semua",
        "Sayur & Bumbu",
        "Buah Segar",
        "Daging & Ikan",
        "Sembako",
        "Kuliner Bali",
        "Kebutuhan Dapur"
    )

    fun getEmoji(category: String): String = when (category) {
        "Sayur & Bumbu" -> "🥬"
        "Buah Segar" -> "🍌"
        "Daging & Ikan" -> "🥩"
        "Sembako" -> "🍚"
        "Kuliner Bali" -> "🍛"
        "Kebutuhan Dapur" -> "🥥"
        else -> "🧺"
    }
}

fun formatRupiah(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    format.maximumFractionDigits = 0
    return format.format(amount)
}

fun formatWaktuIndo(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm 'WITA'", Locale("id", "ID"))
    return sdf.format(Date(timestamp))
}

fun normalizeWhatsapp(phone: String): String {
    var clean = phone.replace(Regex("[^0-9]"), "")
    if (clean.startsWith("0")) {
        clean = "62" + clean.substring(1)
    } else if (clean.startsWith("+62")) {
        clean = clean.substring(1)
    } else if (!clean.startsWith("62") && clean.isNotEmpty()) {
        clean = "62$clean"
    }
    return clean
}
