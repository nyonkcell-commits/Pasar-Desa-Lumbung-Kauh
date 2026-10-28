package com.example.data.script

object GoogleAppsScriptCode {
    const val SCRIPT_TUTORIAL = """
PANDUAN PEMASANGAN GOOGLE SHEETS & APPS SCRIPT:

1. Buka Google Sheets (spreadsheet baru) di browser komputer atau HP:
   https://sheets.new
   Beri nama file: "Database Pasar Desa Lumbung Kauh"

2. Di menu atas Google Sheet, klik:
   Extensions (Ekstensi) > Apps Script

3. Hapus semua kode default di editor, lalu TEMPEL (Paste) seluruh kode Apps Script di bawah ini.

4. Klik tombol "Simpan" (ikon disket), lalu klik tombol biru "Deploy" (Terapkan) di kanan atas:
   -> Pilih "New deployment" (Penerapan baru)
   -> Klik ikon Gerigi (Select type) > pilih "Web app"
   -> Description: "API Pasar Lumbung Kauh"
   -> Execute as: "Me" (Email Google Anda)
   -> Who has access: "Anyone" (Siapa saja - Sangat Penting!)
   -> Klik "Deploy"
   -> Jika muncul permintaan izin ("Authorization Required"), klik "Review Permissions", pilih akun Google Anda, klik "Advanced" (Lanjutan), lalu klik "Go to ... (unsafe)" dan "Allow".

5. Salin "Web app URL" (berakhiran /exec) dan tempelkan ke kolom URL di menu Pengaturan Admin aplikasi ini!
"""

    const val APP_SCRIPT_SOURCE = """/**
 * GOOGLE APPS SCRIPT - DATABASE PASAR DESA LUMBUNG KAUH
 * Backend REST API untuk Aplikasi Android Pasar Desa Lumbung Kauh
 */

const SHEET_PEDAGANG = "Pedagang";
const SHEET_PRODUK = "Produk";
const SHEET_PESANAN = "Pesanan";

function doGet(e) {
  try {
    const sheetApp = SpreadsheetApp.getActiveSpreadsheet();
    setupSheetsIfMissing(sheetApp);
    
    const action = e.parameter.action || "getAll";
    
    if (action === "ping") {
      return responseJson({ status: "success", message: "API Pasar Desa Lumbung Kauh Berjalan Normal!", timestamp: new Date().toISOString() });
    }
    
    if (action === "getAll") {
      const pedagang = getSheetData(sheetApp.getSheetByName(SHEET_PEDAGANG));
      const produk = getSheetData(sheetApp.getSheetByName(SHEET_PRODUK));
      const pesanan = getSheetData(sheetApp.getSheetByName(SHEET_PESANAN));
      return responseJson({ status: "success", pedagang: pedagang, produk: produk, pesanan: pesanan });
    }
    
    if (action === "getPesanan") {
      const pesanan = getSheetData(sheetApp.getSheetByName(SHEET_PESANAN));
      return responseJson({ status: "success", pesanan: pesanan });
    }
    
    return responseJson({ status: "error", message: "Action tidak dikenali" });
  } catch (err) {
    return responseJson({ status: "error", message: err.toString() });
  }
}

function doPost(e) {
  try {
    const sheetApp = SpreadsheetApp.getActiveSpreadsheet();
    setupSheetsIfMissing(sheetApp);
    
    const body = JSON.parse(e.postData.contents);
    const action = body.action;
    
    if (action === "tambahPesanan") {
      const order = body.data;
      const sheet = sheetApp.getSheetByName(SHEET_PESANAN);
      sheet.appendRow([
        order.id || ("PLK-" + Utilities.getUuid().substring(0, 6).toUpperCase()),
        order.pedagangId || "",
        order.namaPedagang || "",
        order.nomorKios || "",
        order.whatsappPedagang || "",
        order.namaPembeli || "",
        order.whatsappPembeli || "",
        order.alamatPengantaran || "",
        order.tipePengiriman || "Ambil di Kios Pasar",
        order.catatan || "",
        order.itemsSummary || "",
        Number(order.totalHarga) || 0,
        order.status || "Menunggu Konfirmasi",
        order.waktuPesanan || new Date().getTime(),
        order.waktuFormat || new Date().toLocaleString("id-ID")
      ]);
      return responseJson({ status: "success", message: "Pesanan berhasil dicatat di Google Sheets" });
    }
    
    if (action === "updatePesananStatus") {
      const sheet = sheetApp.getSheetByName(SHEET_PESANAN);
      const data = sheet.getDataRange().getValues();
      const orderId = body.orderId;
      const newStatus = body.status;
      
      for (let i = 1; i < data.length; i++) {
        if (data[i][0] == orderId) {
          sheet.getRange(i + 1, 13).setValue(newStatus);
          return responseJson({ status: "success", message: "Status pesanan diperbarui" });
        }
      }
      return responseJson({ status: "error", message: "Pesanan tidak ditemukan" });
    }
    
    if (action === "tambahProduk") {
      const prod = body.data;
      const sheet = sheetApp.getSheetByName(SHEET_PRODUK);
      sheet.appendRow([
        prod.id || ("prod_" + Utilities.getUuid().substring(0, 6)),
        prod.pedagangId || "",
        prod.namaPedagang || "",
        prod.nomorKios || "",
        prod.whatsappPedagang || "",
        prod.nama || "",
        prod.kategori || "Sayur & Bumbu",
        Number(prod.harga) || 0,
        prod.satuan || "kg",
        prod.stokTersedia !== false,
        prod.deskripsi || "",
        prod.iconEmoji || "🥬",
        prod.fotoUrl || ""
      ]);
      return responseJson({ status: "success", message: "Produk berhasil ditambahkan" });
    }
    
    if (action === "hapusProduk") {
      const sheet = sheetApp.getSheetByName(SHEET_PRODUK);
      const data = sheet.getDataRange().getValues();
      const id = body.produkId;
      for (let i = 1; i < data.length; i++) {
        if (data[i][0] == id) {
          sheet.deleteRow(i + 1);
          return responseJson({ status: "success", message: "Produk dihapus" });
        }
      }
      return responseJson({ status: "error", message: "Produk tidak ditemukan" });
    }
    
    if (action === "tambahPedagang") {
      const ped = body.data;
      const sheet = sheetApp.getSheetByName(SHEET_PEDAGANG);
      sheet.appendRow([
        ped.id || ("ped_" + Utilities.getUuid().substring(0, 6)),
        ped.nama || "",
        ped.namaKios || "",
        ped.nomorKios || "",
        ped.whatsapp || "",
        ped.pin || "1234",
        ped.kategoriUtama || "Sayur & Bumbu",
        ped.isActive !== false,
        ped.jamBuka || "05:00 - 13:00 WITA",
        ped.catatan || ""
      ]);
      return responseJson({ status: "success", message: "Pedagang berhasil ditambahkan" });
    }
    
    if (action === "hapusPedagang") {
      const sheet = sheetApp.getSheetByName(SHEET_PEDAGANG);
      const data = sheet.getDataRange().getValues();
      const id = body.pedagangId;
      for (let i = 1; i < data.length; i++) {
        if (data[i][0] == id) {
          sheet.deleteRow(i + 1);
          return responseJson({ status: "success", message: "Pedagang dihapus" });
        }
      }
      return responseJson({ status: "error", message: "Pedagang tidak ditemukan" });
    }
    
    return responseJson({ status: "error", message: "Action POST tidak dikenal" });
  } catch (err) {
    return responseJson({ status: "error", message: err.toString() });
  }
}

function getSheetData(sheet) {
  if (!sheet) return [];
  const rows = sheet.getDataRange().getValues();
  if (rows.length <= 1) return [];
  const headers = rows[0];
  const results = [];
  for (let i = 1; i < rows.length; i++) {
    const obj = {};
    for (let j = 0; j < headers.length; j++) {
      obj[headers[j]] = rows[i][j];
    }
    results.push(obj);
  }
  return results;
}

function setupSheetsIfMissing(ss) {
  let sPed = ss.getSheetByName(SHEET_PEDAGANG);
  if (!sPed) {
    sPed = ss.insertSheet(SHEET_PEDAGANG);
    sPed.appendRow(["id", "nama", "namaKios", "nomorKios", "whatsapp", "pin", "kategoriUtama", "isActive", "jamBuka", "catatan"]);
    sPed.getRange(1, 1, 1, 10).setBackground("#1B5E20").setFontColor("#FFFFFF").setFontWeight("bold");
  }
  
  let sProd = ss.getSheetByName(SHEET_PRODUK);
  if (!sProd) {
    sProd = ss.insertSheet(SHEET_PRODUK);
    sProd.appendRow(["id", "pedagangId", "namaPedagang", "nomorKios", "whatsappPedagang", "nama", "kategori", "harga", "satuan", "stokTersedia", "deskripsi", "iconEmoji", "fotoUrl"]);
    sProd.getRange(1, 1, 1, 13).setBackground("#2E7D32").setFontColor("#FFFFFF").setFontWeight("bold");
  }
  
  let sPes = ss.getSheetByName(SHEET_PESANAN);
  if (!sPes) {
    sPes = ss.insertSheet(SHEET_PESANAN);
    sPes.appendRow(["id", "pedagangId", "namaPedagang", "nomorKios", "whatsappPedagang", "namaPembeli", "whatsappPembeli", "alamatPengantaran", "tipePengiriman", "catatan", "itemsSummary", "totalHarga", "status", "waktuPesanan", "waktuFormat"]);
    sPes.getRange(1, 1, 1, 15).setBackground("#BF360C").setFontColor("#FFFFFF").setFontWeight("bold");
  }
}

function responseJson(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
"""
}
