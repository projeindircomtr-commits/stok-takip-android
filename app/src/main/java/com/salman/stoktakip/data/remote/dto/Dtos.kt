package com.salman.stoktakip.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T?,
    val code: String? = null
)

data class ResimYuku(
    val data: String,   // "data:image/jpeg;base64,...."
    val type: String     // "image/jpeg"
)

data class GirisIstek(
    @SerializedName("kullanici_adi") val kullaniciAdi: String,
    val sifre: String,
    val cihaz: String = "Android"
)

data class Kullanici(
    val id: Int,
    @SerializedName("ad_soyad") val adSoyad: String,
    @SerializedName("kullanici_adi") val kullaniciAdi: String,
    val rol: String
)

data class GirisCevap(
    val token: String,
    @SerializedName("expires_at") val expiresAt: String,
    val kullanici: Kullanici
)

data class Kategori(
    val id: Int,
    val ad: String
)

data class Lokasyon(
    val id: Int,
    val ad: String
)

data class Malzeme(
    val id: Int,
    val ad: String,
    @SerializedName("miktar_degeri") val miktarDegeri: Double,
    @SerializedName("miktar_birimi") val miktarBirimi: String,
    @SerializedName("kategori_id") val kategoriId: Int?,
    @SerializedName("lokasyon_id") val lokasyonId: Int?,
    val resim: String?,
    @SerializedName("sync_status") val syncStatus: String?,
    val kategori: String?,
    val lokasyon: String?,
    @SerializedName("created_at") val createdAt: String?
)

data class MalzemeIstek(
    val ad: String,
    @SerializedName("miktar_degeri") val miktarDegeri: Double,
    @SerializedName("miktar_birimi") val miktarBirimi: String,
    @SerializedName("kategori_id") val kategoriId: Int?,
    @SerializedName("lokasyon_id") val lokasyonId: Int?,
    val resim: ResimYuku? = null
)

data class Arac(
    val id: Int,
    @SerializedName("arac_ismi") val aracIsmi: String,
    val model: String?,
    val plaka: String?,
    val kamera: String,
    val gps: String,
    val sahip: String?,
    val telefon: String?,
    @SerializedName("kategori_id") val kategoriId: Int?,
    @SerializedName("lokasyon_id") val lokasyonId: Int?,
    val resim: String?,
    @SerializedName("sync_status") val syncStatus: String?,
    val kategori: String?,
    val lokasyon: String?,
    @SerializedName("created_at") val createdAt: String?
)

data class AracIstek(
    @SerializedName("arac_ismi") val aracIsmi: String,
    val model: String?,
    val plaka: String?,
    val kamera: String,
    val gps: String,
    val sahip: String?,
    val telefon: String?,
    @SerializedName("kategori_id") val kategoriId: Int?,
    @SerializedName("lokasyon_id") val lokasyonId: Int?,
    val resim: ResimYuku? = null
)

data class AdIstek(val ad: String)

data class KategoriDagilim(
    val ad: String,
    val adet: Int
)

data class LokasyonDagilim(
    val ad: String,
    val adet: Int
)

data class Dashboard(
    @SerializedName("malzeme_sayisi") val malzemeSayisi: Int,
    @SerializedName("arac_sayisi") val aracSayisi: Int,
    @SerializedName("kategori_sayisi") val kategoriSayisi: Int,
    @SerializedName("lokasyon_sayisi") val lokasyonSayisi: Int,
    @SerializedName("bekleyen_senkron") val bekleyenSenkron: Int,
    @SerializedName("kritik_stok") val kritikStok: Int,
    @SerializedName("kategori_dagilimi") val kategoriDagilimi: List<KategoriDagilim>?,
    @SerializedName("lokasyon_dagilimi") val lokasyonDagilimi: List<LokasyonDagilim>?,
    val kullanici: Kullanici
)

data class KritikMalzeme(
    val id: Int,
    val ad: String,
    @SerializedName("miktar_degeri") val miktarDegeri: Double,
    @SerializedName("miktar_birimi") val miktarBirimi: String,
    val kategori: String?,
    val lokasyon: String?
)

data class KullaniciListItem(
    val id: Int,
    @SerializedName("ad_soyad") val adSoyad: String,
    @SerializedName("kullanici_adi") val kullaniciAdi: String,
    val rol: String
)

data class KullaniciEkleIstek(
    @SerializedName("ad_soyad") val adSoyad: String,
    @SerializedName("kullanici_adi") val kullaniciAdi: String,
    val sifre: String,
    val rol: String
)

data class YakitKaydi(
    val id: Int,
    @SerializedName("arac_id") val aracId: Int?,
    val plaka: String,
    @SerializedName("yakit_tipi") val yakitTipi: String,
    val litre: Double,
    @SerializedName("birim_fiyat") val birimFiyat: Double?,
    @SerializedName("verilen_ad") val verilenAd: String?,
    @SerializedName("verilen_soyad") val verilenSoyad: String?,
    @SerializedName("verilen_telefon") val verilenTelefon: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("arac_ismi") val aracIsmi: String?
)

data class YakitIstek(
    val plaka: String,
    @SerializedName("yakit_tipi") val yakitTipi: String,
    val litre: Double,
    @SerializedName("birim_fiyat") val birimFiyat: Double?,
    @SerializedName("verilen_ad") val verilenAd: String?,
    @SerializedName("verilen_soyad") val verilenSoyad: String?,
    @SerializedName("verilen_telefon") val verilenTelefon: String?
)
