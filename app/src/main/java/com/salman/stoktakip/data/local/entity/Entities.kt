package com.salman.stoktakip.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "malzeme_cache")
data class MalzemeCacheEntity(
    @PrimaryKey val id: Int,
    val ad: String,
    val miktarDegeri: Double,
    val miktarBirimi: String,
    val kategoriId: Int?,
    val lokasyonId: Int?,
    val kategoriAdi: String?,
    val lokasyonAdi: String?,
    val resim: String?,
    val syncStatus: String?,
    val olusturmaTarihi: String? = null
)

@Entity(tableName = "arac_cache")
data class AracCacheEntity(
    @PrimaryKey val id: Int,
    val aracIsmi: String,
    val model: String?,
    val plaka: String?,
    val kamera: String,
    val gps: String,
    val sahip: String?,
    val telefon: String?,
    val kategoriId: Int?,
    val lokasyonId: Int?,
    val kategoriAdi: String?,
    val lokasyonAdi: String?,
    val resim: String?,
    val syncStatus: String?,
    val olusturmaTarihi: String? = null
)

@Entity(tableName = "kategori_cache")
data class KategoriCacheEntity(
    @PrimaryKey val id: Int,
    val ad: String
)

@Entity(tableName = "lokasyon_cache")
data class LokasyonCacheEntity(
    @PrimaryKey val id: Int,
    val ad: String
)

/**
 * Baglanti olmadan eklenen/guncellenen kayitlar bu tabloda kuyruga alinir
 * ve baglanti gelince SyncWorker tarafindan sunucuya gonderilir.
 */
@Entity(tableName = "bekleyen_islem")
data class BekleyenIslemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val varlik: String,          // "malzeme" | "arac"
    val islemTuru: String,       // "ekle" | "guncelle" | "sil"
    val hedefId: Int?,           // guncelle/sil icin sunucu id'si
    val payloadJson: String,     // MalzemeIstek/AracIstek JSON'i
    val olusturmaZamani: Long = System.currentTimeMillis(),
    val sonHata: String? = null
)
