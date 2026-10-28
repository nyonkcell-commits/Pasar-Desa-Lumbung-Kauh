package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CartItem
import com.example.data.model.formatRupiah
import com.example.data.model.normalizeWhatsapp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Pasar Desa Lumbung Kauh", appName)
    }

    @Test
    fun `whatsapp phone normalization works`() {
        assertEquals("6281234567890", normalizeWhatsapp("081234567890"))
        assertEquals("6281234567890", normalizeWhatsapp("+6281234567890"))
        assertEquals("6281234567890", normalizeWhatsapp("6281234567890"))
        assertEquals("6281234567890", normalizeWhatsapp("0812-3456-7890"))
    }

    @Test
    fun `cart subtotal calculation is accurate`() {
        val item = CartItem(
            produkId = "p1",
            namaProduk = "Kangkung Segar",
            harga = 4000.0,
            jumlah = 3
        )
        assertEquals(12000.0, item.subtotal, 0.01)
    }

    @Test
    fun `format rupiah outputs Indonesian currency`() {
        val formatted = formatRupiah(15000.0)
        assertTrue(formatted.contains("15") && (formatted.contains("Rp") || formatted.contains("IDR")))
    }
}
