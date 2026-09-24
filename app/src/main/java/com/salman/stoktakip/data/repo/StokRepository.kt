package com.salman.stoktakip.data.repo

import android.content.Context
import com.google.gson.Gson
import com.salman.stoktakip.data.local.AppDatabase
import com.salman.stoktakip.data.local.entity.*
import com.salman.stoktakip.data.remote.ApiService
import com.salman.stoktakip.data.remote.dto.*
import com.salman.stoktakip.util.AgDurumu
import com.salman.stoktakip.util.Resource
import kotlinx.coroutines.flow.Flow

class StokRepository(
    private val context: Context,
    private val api: ApiService
) {
    private val db = AppDatabase.getInstance(context)
    private val gson = Gson()

    /* ------------------------------ GOZLEM (Flow) ------------------------------ */
    fun malzemelerGozlemle(): Flow<List<MalzemeCacheEntity>> = db.malzemeDao().tumunuGozlemle()
    fun araclarGozlemle(): Flow<List<AracCacheEntity>> = db.aracDao().tumunuGozlemle()
    fun kategorilerGozlemle(): Flow<List<KategoriCacheEntity>> = db.kategoriDao().tumunuGozlemle()
    fun lokasyonlarGozlemle(): Flow<List<LokasyonCacheEntity>> = db.lokasyonDao().tumunuGozlemle()
    fun bekleyenSayiGozlemle(): Flow<Int> = db.bekleyenIslemDao().sayiGozlemle()

    /* ------------------------------ SENKRON (sunucu -> yerel) ------------------------------ */
    suspend fun tumVeriyiYenile(): Resource<Unit> {
        if (!AgDurumu.bagliMi(context)) return Resource.Hata("Çevrimdışısınız. Önbellekteki veriler gösteriliyor.", "offline")
        return try {
            val kategoriler = api.kategoriler().body()?.data.orEmpty()
            val lokasyonlar = api.lokasyonlar().body()?.data.orEmpty()
            val malzemeler = api.malzemeler().body()?.data.orEmpty()
            val araclar = api.araclar().body()?.data.orEmpty()

            db.kategoriDao().onbellekYenile(kategoriler.map { KategoriCacheEntity(it.id, it.ad) })
            db.lokasyonDao().onbellekYenile(lokasyonlar.map { LokasyonCacheEntity(it.id, it.ad) })
            db.malzemeDao().onbellekYenile(malzemeler.map {
                MalzemeCacheEntity(
                    it.id, it.ad, it.miktarDegeri, it.miktarBirimi, it.kategoriId, it.lokasyonId,
                    it.kategori, it.lokasyon, it.resim, it.syncStatus, it.createdAt
                )
            })
            db.aracDao().onbellekYenile(araclar.map {
                AracCacheEntity(
                    it.id, it.aracIsmi, it.model, it.plaka, it.kamera, it.gps, it.sahip, it.telefon,
                    it.kategoriId, it.lokasyonId, it.kategori, it.lokasyon, it.resim, it.syncStatus, it.createdAt
                )
            })
            Resource.Basarili(Unit)
        } catch (e: Exception) {
            Resource.Hata(e.message ?: "Veriler alınamadı.")
        }
    }

    /** Cevrimicidir sunucuda arama yapar; cevrimdisiysa cagiran taraf onbellek Flow'unu filtreler. */
    suspend fun malzemeAra(sorgu: String, kategoriId: Int?, lokasyonId: Int?): Resource<List<Malzeme>> {
        if (!AgDurumu.bagliMi(context)) return Resource.Hata("Çevrimdışı arama yapılamıyor.", "offline")
        return try {
            val cevap = api.malzemeler(sorgu.ifBlank { null }, kategoriId, lokasyonId)
            val govde = cevap.body()
            if (cevap.isSuccessful && govde?.success == true) Resource.Basarili(govde.data.orEmpty())
            else Resource.Hata(govde?.message ?: "Arama başarısız oldu.")
        } catch (e: Exception) {
            Resource.Hata(e.message ?: "Sunucuya bağlanılamadı.")
        }
    }

    suspend fun aracAra(sorgu: String, kategoriId: Int?, lokasyonId: Int?): Resource<List<Arac>> {
        if (!AgDurumu.bagliMi(context)) return Resource.Hata("Çevrimdışı arama yapılamıyor.", "offline")
        return try {
            val cevap = api.araclar(sorgu.ifBlank { null }, kategoriId, lokasyonId)
            val govde = cevap.body()
            if (cevap.isSuccessful && govde?.success == true) Resource.Basarili(govde.data.orEmpty())
            else Resource.Hata(govde?.message ?: "Arama başarısız oldu.")
        } catch (e: Exception) {
            Resource.Hata(e.message ?: "Sunucuya bağlanılamadı.")
        }
    }

    /* ------------------------------ MALZEME EKLE / GUNCELLE / SIL ------------------------------ */
    suspend fun malzemeEkle(istek: MalzemeIstek): Resource<Unit> {
        if (!AgDurumu.bagliMi(context)) {
            kuyrugaEkle("malzeme", "ekle", null, istek)
            geciciMalzemeOnbellegeEkle(istek)
            return Resource.Basarili(Unit)
        }
        return try {
            val cevap = api.malzemeEkle(istek)
            if (cevap.isSuccessful && cevap.body()?.success == true) {
                tumVeriyiYenile()
                Resource.Basarili(Unit)
            } else {
                Resource.Hata(cevap.body()?.message ?: "Kayıt eklenemedi.")
            }
        } catch (e: Exception) {
            kuyrugaEkle("malzeme", "ekle", null, istek)
            geciciMalzemeOnbellegeEkle(istek)
            Resource.Basarili(Unit)
        }
    }

    suspend fun malzemeGuncelle(id: Int, istek: MalzemeIstek): Resource<Unit> {
        if (!AgDurumu.bagliMi(context)) {
            kuyrugaEkle("malzeme", "guncelle", id, istek)
            return Resource.Basarili(Unit)
        }
        return try {
            val cevap = api.malzemeGuncelle(id, istek)
            if (cevap.isSuccessful && cevap.body()?.success == true) {
                tumVeriyiYenile()
                Resource.Basarili(Unit)
            } else Resource.Hata(cevap.body()?.message ?: "Güncellenemedi.")
        } catch (e: Exception) {
            kuyrugaEkle("malzeme", "guncelle", id, istek)
            Resource.Basarili(Unit)
        }
    }

    suspend fun malzemeSil(id: Int): Resource<Unit> {
        if (!AgDurumu.bagliMi(context)) {
            kuyrugaEkle("malzeme", "sil", id, null)
            db.malzemeDao().idIleSil(id)
            return Resource.Basarili(Unit)
        }
        return try {
            val cevap = api.malzemeSil(id)
            if (cevap.isSuccessful && cevap.body()?.success == true) {
                db.malzemeDao().idIleSil(id)
                Resource.Basarili(Unit)
            } else Resource.Hata(cevap.body()?.message ?: "Silinemedi.")
        } catch (e: Exception) {
            Resource.Hata(e.message ?: "Sunucuya bağlanılamadı.")
        }
    }

    /* ------------------------------ ARAC EKLE / GUNCELLE / SIL ------------------------------ */
    suspend fun aracEkle(istek: AracIstek): Resource<Unit> {
        if (!AgDurumu.bagliMi(context)) {
            kuyrugaEkle("arac", "ekle", null, istek)
            geciciAracOnbellegeEkle(istek)
            return Resource.Basarili(Unit)
        }
        return try {
            val cevap = api.aracEkle(istek)
            if (cevap.isSuccessful && cevap.body()?.success == true) {
                tumVeriyiYenile()
                Resource.Basarili(Unit)
            } else Resource.Hata(cevap.body()?.message ?: "Kayıt eklenemedi.")
        } catch (e: Exception) {
            kuyrugaEkle("arac", "ekle", null, istek)
            geciciAracOnbellegeEkle(istek)
            Resource.Basarili(Unit)
        }
    }

    suspend fun aracGuncelle(id: Int, istek: AracIstek): Resource<Unit> {
        if (!AgDurumu.bagliMi(context)) {
            kuyrugaEkle("arac", "guncelle", id, istek)
            return Resource.Basarili(Unit)
        }
        return try {
            val cevap = api.aracGuncelle(id, istek)
            if (cevap.isSuccessful && cevap.body()?.success == true) {
                tumVeriyiYenile()
                Resource.Basarili(Unit)
            } else Resource.Hata(cevap.body()?.message ?: "Güncellenemedi.")
        } catch (e: Exception) {
            kuyrugaEkle("arac", "guncelle", id, istek)
            Resource.Basarili(Unit)
        }
    }

    suspend fun aracSil(id: Int): Resource<Unit> {
        if (!AgDurumu.bagliMi(context)) {
            kuyrugaEkle("arac", "sil", id, null)
            db.aracDao().idIleSil(id)
            return Resource.Basarili(Unit)
        }
        return try {
            val cevap = api.aracSil(id)
            if (cevap.isSuccessful && cevap.body()?.success == true) {
                db.aracDao().idIleSil(id)
                Resource.Basarili(Unit)
            } else Resource.Hata(cevap.body()?.message ?: "Silinemedi.")
        } catch (e: Exception) {
            Resource.Hata(e.message ?: "Sunucuya bağlanılamadı.")
        }
    }

    /* ------------------------------ DASHBOARD / RAPORLAR / KULLANICILAR ------------------------------ */
    suspend fun dashboardGetir(): Resource<Dashboard> {
        if (!AgDurumu.bagliMi(context)) return Resource.Hata("Çevrimdışısınız.", "offline")
        return try {
            val cevap = api.dashboard()
            val govde = cevap.body()
            if (cevap.isSuccessful && govde?.success == true && govde.data != null) Resource.Basarili(govde.data)
            else Resource.Hata(govde?.message ?: "Veriler alınamadı.")
        } catch (e: Exception) {
            Resource.Hata(e.message ?: "Sunucuya bağlanılamadı.")
        }
    }

    suspend fun kritikStokGetir(): Resource<List<KritikMalzeme>> {
        if (!AgDurumu.bagliMi(context)) return Resource.Hata("Çevrimdışısınız.", "offline")
        return try {
            val cevap = api.kritikStok()
            val govde = cevap.body()
            if (cevap.isSuccessful && govde?.success == true) Resource.Basarili(govde.data.orEmpty())
            else Resource.Hata(govde?.message ?: "Veriler alınamadı.")
        } catch (e: Exception) {
            Resource.Hata(e.message ?: "Sunucuya bağlanılamadı.")
        }
    }

    suspend fun kullanicilarGetir(): Resource<List<KullaniciListItem>> {
        if (!AgDurumu.bagliMi(context)) return Resource.Hata("Çevrimdışısınız.", "offline")
        return try {
            val cevap = api.kullanicilar()
            val govde = cevap.body()
            if (cevap.isSuccessful && govde?.success == true) Resource.Basarili(govde.data.orEmpty())
            else Resource.Hata(govde?.message ?: "Veriler alınamadı.")
        } catch (e: Exception) {
            Resource.Hata(e.message ?: "Sunucuya bağlanılamadı.")
        }
    }

    suspend fun kullaniciEkle(adSoyad: String, kullaniciAdi: String, sifre: String, rol: String): Resource<Unit> = try {
        val cevap = api.kullaniciEkle(KullaniciEkleIstek(adSoyad, kullaniciAdi, sifre, rol))
        if (cevap.isSuccessful && cevap.body()?.success == true) Resource.Basarili(Unit)
        else Resource.Hata(cevap.body()?.message ?: "Kullanıcı eklenemedi.")
    } catch (e: Exception) {
        Resource.Hata(e.message ?: "Sunucuya bağlanılamadı.")
    }

    /* ------------------------------ KATEGORI / LOKASYON EKLE ------------------------------ */
    suspend fun kategoriEkle(ad: String): Resource<Unit> = try {
        val cevap = api.kategoriEkle(AdIstek(ad))
        if (cevap.isSuccessful && cevap.body()?.success == true) { tumVeriyiYenile(); Resource.Basarili(Unit) }
        else Resource.Hata(cevap.body()?.message ?: "Eklenemedi.")
    } catch (e: Exception) {
        Resource.Hata(e.message ?: "Sunucuya bağlanılamadı.")
    }

    suspend fun lokasyonEkle(ad: String): Resource<Unit> = try {
        val cevap = api.lokasyonEkle(AdIstek(ad))
        if (cevap.isSuccessful && cevap.body()?.success == true) { tumVeriyiYenile(); Resource.Basarili(Unit) }
        else Resource.Hata(cevap.body()?.message ?: "Eklenemedi.")
    } catch (e: Exception) {
        Resource.Hata(e.message ?: "Sunucuya bağlanılamadı.")
    }

    /* ------------------------------ BEKLEYEN KUYRUGU SUNUCUYA GONDER ------------------------------ */
    suspend fun bekleyenleriSenkronize(): Resource<Unit> {
        if (!AgDurumu.bagliMi(context)) return Resource.Hata("Çevrimdışı.", "offline")

        val bekleyenler = db.bekleyenIslemDao().hepsi()
        if (bekleyenler.isEmpty()) return Resource.Basarili(Unit)

        var hataOldu = false
        for (kayit in bekleyenler) {
            try {
                val basarili = when (kayit.varlik) {
                    "malzeme" -> gonderMalzemeIslemi(kayit)
                    "arac" -> gonderAracIslemi(kayit)
                    else -> true
                }
                if (basarili) {
                    db.bekleyenIslemDao().sil(kayit)
                } else {
                    hataOldu = true
                }
            } catch (e: Exception) {
                db.bekleyenIslemDao().hataYaz(kayit.id, e.message ?: "Bilinmeyen hata")
                hataOldu = true
            }
        }

        tumVeriyiYenile()
        return if (hataOldu) Resource.Hata("Bazı kayıtlar senkronize edilemedi, tekrar denenecek.")
        else Resource.Basarili(Unit)
    }

    private suspend fun gonderMalzemeIslemi(kayit: BekleyenIslemEntity): Boolean {
        return when (kayit.islemTuru) {
            "ekle" -> {
                val istek = gson.fromJson(kayit.payloadJson, MalzemeIstek::class.java)
                api.malzemeEkle(istek).let { it.isSuccessful && it.body()?.success == true }
            }
            "guncelle" -> {
                val istek = gson.fromJson(kayit.payloadJson, MalzemeIstek::class.java)
                val id = kayit.hedefId ?: return true
                api.malzemeGuncelle(id, istek).let { it.isSuccessful && it.body()?.success == true }
            }
            "sil" -> {
                val id = kayit.hedefId ?: return true
                api.malzemeSil(id).let { it.isSuccessful && it.body()?.success == true }
            }
            else -> true
        }
    }

    private suspend fun gonderAracIslemi(kayit: BekleyenIslemEntity): Boolean {
        return when (kayit.islemTuru) {
            "ekle" -> {
                val istek = gson.fromJson(kayit.payloadJson, AracIstek::class.java)
                api.aracEkle(istek).let { it.isSuccessful && it.body()?.success == true }
            }
            "guncelle" -> {
                val istek = gson.fromJson(kayit.payloadJson, AracIstek::class.java)
                val id = kayit.hedefId ?: return true
                api.aracGuncelle(id, istek).let { it.isSuccessful && it.body()?.success == true }
            }
            "sil" -> {
                val id = kayit.hedefId ?: return true
                api.aracSil(id).let { it.isSuccessful && it.body()?.success == true }
            }
            else -> true
        }
    }

    /* ------------------------------ YARDIMCI: OFFLINE KUYRUK ------------------------------ */
    private suspend fun kuyrugaEkle(varlik: String, islem: String, hedefId: Int?, payload: Any?) {
        db.bekleyenIslemDao().ekle(
            BekleyenIslemEntity(
                varlik = varlik,
                islemTuru = islem,
                hedefId = hedefId,
                payloadJson = if (payload != null) gson.toJson(payload) else ""
            )
        )
    }

    private suspend fun geciciMalzemeOnbellegeEkle(istek: MalzemeIstek) {
        val geciciId = -((System.currentTimeMillis() % Int.MAX_VALUE).toInt().coerceAtLeast(1))
        db.malzemeDao().ekleVeyaGuncelle(
            listOf(
                MalzemeCacheEntity(
                    geciciId, istek.ad, istek.miktarDegeri, istek.miktarBirimi,
                    istek.kategoriId, istek.lokasyonId, null, null, null, "offline_bekliyor"
                )
            )
        )
    }

    private suspend fun geciciAracOnbellegeEkle(istek: AracIstek) {
        val geciciId = -((System.currentTimeMillis() % Int.MAX_VALUE).toInt().coerceAtLeast(1))
        db.aracDao().ekleVeyaGuncelle(
            listOf(
                AracCacheEntity(
                    geciciId, istek.aracIsmi, istek.model, istek.plaka, istek.kamera, istek.gps,
                    istek.sahip, istek.telefon, istek.kategoriId, istek.lokasyonId, null, null, null, "offline_bekliyor"
                )
            )
        )
    }
}
