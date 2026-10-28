package com.example.data.network

import android.util.Log
import com.example.data.model.Pedagang
import com.example.data.model.Pesanan
import com.example.data.model.Produk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GoogleSheetsApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun testConnection(webAppUrl: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (webAppUrl.isBlank()) {
                return@withContext Result.failure(Exception("URL Web App masih kosong"))
            }
            val url = if (webAppUrl.contains("?")) "$webAppUrl&action=ping" else "$webAppUrl?action=ping"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                Result.success("Terhubung! Respon: $body")
            } else {
                Result.failure(Exception("HTTP ${response.code}: $body"))
            }
        } catch (e: Exception) {
            Log.e("GoogleSheetsApi", "Error test connection", e)
            Result.failure(e)
        }
    }

    suspend fun fetchAllData(webAppUrl: String): Result<Triple<List<Pedagang>, List<Produk>, List<Pesanan>>> =
        withContext(Dispatchers.IO) {
            try {
                if (webAppUrl.isBlank()) {
                    return@withContext Result.failure(Exception("URL Google Apps Script belum diisi"))
                }
                val url = if (webAppUrl.contains("?")) "$webAppUrl&action=getAll" else "$webAppUrl?action=getAll"
                val request = Request.Builder().url(url).get().build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""
                
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: $body"))
                }

                val json = JSONObject(body)
                val status = json.optString("status", "")
                if (status != "success" && !json.has("pedagang")) {
                    return@withContext Result.failure(Exception(json.optString("message", "Format respon tidak sesuai")))
                }

                val pedagangList = mutableListOf<Pedagang>()
                val produkList = mutableListOf<Produk>()
                val pesananList = mutableListOf<Pesanan>()

                val arrPedagang = json.optJSONArray("pedagang") ?: JSONArray()
                for (i in 0 until arrPedagang.length()) {
                    val item = arrPedagang.getJSONObject(i)
                    pedagangList.add(
                        Pedagang(
                            id = item.optString("id"),
                            nama = item.optString("nama"),
                            namaKios = item.optString("namaKios"),
                            nomorKios = item.optString("nomorKios"),
                            whatsapp = item.optString("whatsapp"),
                            pin = item.optString("pin", "1234"),
                            kategoriUtama = item.optString("kategoriUtama", "Sayur & Bumbu"),
                            isActive = item.optBoolean("isActive", true),
                            jamBuka = item.optString("jamBuka", "05:00 - 13:00 WITA"),
                            catatan = item.optString("catatan", "")
                        )
                    )
                }

                val arrProduk = json.optJSONArray("produk") ?: JSONArray()
                for (i in 0 until arrProduk.length()) {
                    val item = arrProduk.getJSONObject(i)
                    produkList.add(
                        Produk(
                            id = item.optString("id"),
                            pedagangId = item.optString("pedagangId"),
                            namaPedagang = item.optString("namaPedagang"),
                            namaKios = item.optString("namaKios"),
                            nomorKios = item.optString("nomorKios"),
                            whatsappPedagang = item.optString("whatsappPedagang"),
                            nama = item.optString("nama"),
                            kategori = item.optString("kategori", "Sayur & Bumbu"),
                            harga = item.optDouble("harga", 0.0),
                            satuan = item.optString("satuan", "kg"),
                            stokTersedia = item.optBoolean("stokTersedia", true),
                            deskripsi = item.optString("deskripsi", ""),
                            iconEmoji = item.optString("iconEmoji", "🥬"),
                            fotoUrl = item.optString("fotoUrl", "")
                        )
                    )
                }

                val arrPesanan = json.optJSONArray("pesanan") ?: JSONArray()
                for (i in 0 until arrPesanan.length()) {
                    val item = arrPesanan.getJSONObject(i)
                    pesananList.add(
                        Pesanan(
                            id = item.optString("id"),
                            pedagangId = item.optString("pedagangId"),
                            namaPedagang = item.optString("namaPedagang"),
                            nomorKios = item.optString("nomorKios"),
                            whatsappPedagang = item.optString("whatsappPedagang"),
                            namaPembeli = item.optString("namaPembeli"),
                            whatsappPembeli = item.optString("whatsappPembeli"),
                            alamatPengantaran = item.optString("alamatPengantaran"),
                            tipePengiriman = item.optString("tipePengiriman", "Ambil di Kios Pasar"),
                            catatan = item.optString("catatan", ""),
                            itemsSummary = item.optString("itemsSummary"),
                            totalHarga = item.optDouble("totalHarga", 0.0),
                            status = item.optString("status", Pesanan.STATUS_MENUNGGU),
                            waktuPesanan = item.optLong("waktuPesanan", System.currentTimeMillis()),
                            waktuFormat = item.optString("waktuFormat")
                        )
                    )
                }

                Result.success(Triple(pedagangList, produkList, pesananList))
            } catch (e: Exception) {
                Log.e("GoogleSheetsApi", "Error fetchAllData", e)
                Result.failure(e)
            }
        }

    suspend fun postOrder(webAppUrl: String, pesanan: Pesanan): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                if (webAppUrl.isBlank()) return@withContext Result.success(true)
                val payload = JSONObject().apply {
                    put("action", "tambahPesanan")
                    put("data", JSONObject().apply {
                        put("id", pesanan.id)
                        put("pedagangId", pesanan.pedagangId)
                        put("namaPedagang", pesanan.namaPedagang)
                        put("nomorKios", pesanan.nomorKios)
                        put("whatsappPedagang", pesanan.whatsappPedagang)
                        put("namaPembeli", pesanan.namaPembeli)
                        put("whatsappPembeli", pesanan.whatsappPembeli)
                        put("alamatPengantaran", pesanan.alamatPengantaran)
                        put("tipePengiriman", pesanan.tipePengiriman)
                        put("catatan", pesanan.catatan)
                        put("itemsSummary", pesanan.itemsSummary)
                        put("totalHarga", pesanan.totalHarga)
                        put("status", pesanan.status)
                        put("waktuPesanan", pesanan.waktuPesanan)
                        put("waktuFormat", pesanan.waktuFormat)
                    })
                }

                val req = Request.Builder()
                    .url(webAppUrl)
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()
                val resp = client.newCall(req).execute()
                Result.success(resp.isSuccessful)
            } catch (e: Exception) {
                Log.e("GoogleSheetsApi", "Error postOrder", e)
                Result.failure(e)
            }
        }

    suspend fun updateOrderStatus(webAppUrl: String, orderId: String, newStatus: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                if (webAppUrl.isBlank()) return@withContext Result.success(true)
                val payload = JSONObject().apply {
                    put("action", "updatePesananStatus")
                    put("orderId", orderId)
                    put("status", newStatus)
                }

                val req = Request.Builder()
                    .url(webAppUrl)
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()
                val resp = client.newCall(req).execute()
                Result.success(resp.isSuccessful)
            } catch (e: Exception) {
                Log.e("GoogleSheetsApi", "Error updateOrderStatus", e)
                Result.failure(e)
            }
        }

    suspend fun postProduct(webAppUrl: String, produk: Produk): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                if (webAppUrl.isBlank()) return@withContext Result.success(true)
                val payload = JSONObject().apply {
                    put("action", "tambahProduk")
                    put("data", JSONObject().apply {
                        put("id", produk.id)
                        put("pedagangId", produk.pedagangId)
                        put("namaPedagang", produk.namaPedagang)
                        put("namaKios", produk.namaKios)
                        put("nomorKios", produk.nomorKios)
                        put("whatsappPedagang", produk.whatsappPedagang)
                        put("nama", produk.nama)
                        put("kategori", produk.kategori)
                        put("harga", produk.harga)
                        put("satuan", produk.satuan)
                        put("stokTersedia", produk.stokTersedia)
                        put("deskripsi", produk.deskripsi)
                        put("iconEmoji", produk.iconEmoji)
                        put("fotoUrl", produk.fotoUrl)
                    })
                }

                val req = Request.Builder()
                    .url(webAppUrl)
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()
                val resp = client.newCall(req).execute()
                Result.success(resp.isSuccessful)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun deleteProduct(webAppUrl: String, productId: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                if (webAppUrl.isBlank()) return@withContext Result.success(true)
                val payload = JSONObject().apply {
                    put("action", "hapusProduk")
                    put("produkId", productId)
                }

                val req = Request.Builder()
                    .url(webAppUrl)
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()
                val resp = client.newCall(req).execute()
                Result.success(resp.isSuccessful)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun postPedagang(webAppUrl: String, pedagang: Pedagang): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                if (webAppUrl.isBlank()) return@withContext Result.success(true)
                val payload = JSONObject().apply {
                    put("action", "tambahPedagang")
                    put("data", JSONObject().apply {
                        put("id", pedagang.id)
                        put("nama", pedagang.nama)
                        put("namaKios", pedagang.namaKios)
                        put("nomorKios", pedagang.nomorKios)
                        put("whatsapp", pedagang.whatsapp)
                        put("pin", pedagang.pin)
                        put("kategoriUtama", pedagang.kategoriUtama)
                        put("isActive", pedagang.isActive)
                        put("jamBuka", pedagang.jamBuka)
                        put("catatan", pedagang.catatan)
                    })
                }

                val req = Request.Builder()
                    .url(webAppUrl)
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()
                val resp = client.newCall(req).execute()
                Result.success(resp.isSuccessful)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun deletePedagang(webAppUrl: String, pedagangId: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                if (webAppUrl.isBlank()) return@withContext Result.success(true)
                val payload = JSONObject().apply {
                    put("action", "hapusPedagang")
                    put("pedagangId", pedagangId)
                }

                val req = Request.Builder()
                    .url(webAppUrl)
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()
                val resp = client.newCall(req).execute()
                Result.success(resp.isSuccessful)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
