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

    /* ------------------------------ YAKIT TAKIP ------------------------------ */
    suspend fun yakitKayitlariGetir(): Resource<List<YakitKaydi>> {
        if (!AgDurumu.bagliMi(context)) return Resource.Hata("Çevrimdışısınız.", "offline")
        return try {
            val cevap = api.yakitKayitlari()
            val govde = cevap.body()
            if (cevap.isSuccessful && govde?.success == true) Resource.Basarili(govde.data.orEmpty())
            else Resource.Hata(govde?.message ?: "Veriler alınamadı.")
        } catch (e: Exception) {
            Resource.Hata(e.message ?: "Sunucuya bağlanılamadı.")
        }
    }

    suspend fun yakitEkle(plaka: String, yakitTipi: String, litre: Double, birimFiyat: Double?, ad: String?, soyad: String?, telefon: String?): Resource<Unit> = try {
        val cevap = api.yakitEkle(YakitIstek(plaka, yakitTipi, litre, birimFiyat, ad, soyad, telefon))
        if (cevap.isSuccessful && cevap.body()?.success == true) Resource.Basarili(Unit)
        else Resource.Hata(cevap.body()?.message ?: "Yakıt kaydı eklenemedi.")
    } catch (e: Exception) {
        Resource.Hata(e.message ?: "Sunucuya bağlanılamadı.")
    }

    suspend fun yakitSil(id: Int): Resource<Unit> = try {
        val cevap = api.yakitSil(id)
        if (cevap.isSuccessful && cevap.body()?.success == true) Resource.Basarili(Unit)
        else Resource.Hata(cevap.body()?.message ?: "Silinemedi.")
    } catch (e: Exception) {
        Resource.Hata(e.message ?: "Sunucuya bağlanılamadı.")
    }

    /* ------------------------------ YEMEK TAKIP ------------------------------ */
    suspend fun yemekKayitlariGetir(): Resource<List<YemekKaydi>> {
        if (!AgDurumu.bagliMi(context)) return Resource.Hata("Çevrimdışısınız.", "offline")
        return try {
            val cevap = api.yemekKayitlari()
            val govde = cevap.body()
            if (cevap.isSuccessful && govde?.success == true) Resource.Basarili(govde.data.orEmpty())
            else Resource.Hata(govde?.message ?: "Veriler alınamadı.")
        } catch (e: Exception) {
            Resource.Hata(e.message ?: "Sunucuya bağlanılamadı.")
        }
    }

    suspend fun yemekEkle(kisiSayisi: Int, yemekAdedi: Int, ogun: String, notMetni: String?): Resource<Unit> = try {
        val cevap = api.yemekEkle(YemekIstek(kisiSayisi, yemekAdedi, ogun, notMetni))
        if (cevap.isSuccessful && cevap.body()?.success == true) Resource.Basarili(Unit)
        else Resource.Hata(cevap.body()?.message ?: "Yemek kaydı eklenemedi.")
    } catch (e: Exception) {
        Resource.Hata(e.message ?: "Sunucuya bağlanılamadı.")
    }

    suspend fun yemekSil(id: Int): Resource<Unit> = try {
        val cevap = api.yemekSil(id)
        if (cevap.isSuccessful && cevap.body()?.success == true) Resource.Basarili(Unit)
        else Resource.Hata(cevap.body()?.message ?: "Silinemedi.")
    } catch (e: Exception) {
        Resource.Hata(e.message ?: "Sunucuya bağlanılamadı.")
    }

    /* ------------------------------ EVRAK SURE TAKIP ------------------------------ */
    suspend fun evraklarGetir(): Resource<List<Evrak>> {
        if (!AgDurumu.bagliMi(context)) return Resource.Hata("Çevrimdışısınız.", "offline")
        return try {
            val cevap = api.evraklar()
            val govde = cevap.body()
            if (cevap.isSuccessful && govde?.success == true) Resource.Basarili(govde.data.orEmpty())
            else Resource.Hata(govde?.message ?: "Veriler alınamadı.")
        } catch (e: Exception) {
            Resource.Hata(e.message ?: "Sunucuya bağlanılamadı.")
        }
    }

    suspend fun evrakEkle(
        aracId: Int?, evrakTipi: String, ilgiliAd: String, tarih: String, resim: ResimYuku?, notMetni: String?
    ): Resource<Unit> = try {
        val cevap = api.evrakEkle(EvrakIstek(aracId, evrakTipi, ilgiliAd, tarih, resim, notMetni))
        if (cevap.isSuccessful && cevap.body()?.success == true) Resource.Basarili(Unit)
        else Resource.Hata(cevap.body()?.message ?: "Evrak kaydedilemedi.")
    } catch (e: Exception) {
        Resource.Hata(e.message ?: "Sunucuya bağlanılamadı.")
    }

    suspend fun evrakSil(id: Int): Resource<Unit> = try {
        val cevap = api.evrakSil(id)
        if (cevap.isSuccessful && cevap.body()?.success == true) Resource.Basarili(Unit)
        else Resource.Hata(cevap.body()?.message ?: "Silinemedi.")
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

        return try {
            val bekleyenler = db.bekleyenIslemDao().hepsi()
            if (bekleyenler.isEmpty()) return Resource.Basarili(Unit)

            var hataOldu = false
            var sonHataMesaji = ""
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
                        sonHataMesaji = "sunucu kaydı reddetti"
                    }
                } catch (e: Exception) {
                    val hataMetni = "${e.javaClass.simpleName}: ${e.message}"
                    db.bekleyenIslemDao().hataYaz(kayit.id, hataMetni)
                    hataOldu = true
                    sonHataMesaji = hataMetni
                }
            }

            tumVeriyiYenile()
            if (hataOldu) Resource.Hata("Senkron hatası ($sonHataMesaji)")
            else Resource.Basarili(Unit)
        } catch (e: Exception) {
            Resource.Hata("Senkron hatası (${e.javaClass.simpleName}: ${e.message})")
        }
    }

    private suspend fun gonderMalzemeIslemi(kayit: BekleyenIslemEntity): Boolean {
        return when (kayit.islemTuru) {
            "ekle" -> {
                val hamIstek = gson.fromJson(kayit.payloadJson, MalzemeIstek::class.java)
                val dosyaYolu = hamIstek.resim?.data?.removePrefix("dosya://")
                val istek = fotografiDosyadanGeriYukle(hamIstek)
                val basarili = api.malzemeEkle(istek).let { it.isSuccessful && it.body()?.success == true }
                if (basarili && dosyaYolu != null) java.io.File(dosyaYolu).delete()
                basarili
            }
            "guncelle" -> {
                val hamIstek = gson.fromJson(kayit.payloadJson, MalzemeIstek::class.java)
                val dosyaYolu = hamIstek.resim?.data?.removePrefix("dosya://")
                val istek = fotografiDosyadanGeriYukle(hamIstek)
                val id = kayit.hedefId ?: return true
                val basarili = api.malzemeGuncelle(id, istek).let { it.isSuccessful && it.body()?.success == true }
                if (basarili && dosyaYolu != null) java.io.File(dosyaYolu).delete()
                basarili
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
                val hamIstek = gson.fromJson(kayit.payloadJson, AracIstek::class.java)
                val dosyaYolu = hamIstek.resim?.data?.removePrefix("dosya://")
                val istek = fotografiDosyadanGeriYukle(hamIstek)
                val basarili = api.aracEkle(istek).let { it.isSuccessful && it.body()?.success == true }
                if (basarili && dosyaYolu != null) java.io.File(dosyaYolu).delete()
                basarili
            }
            "guncelle" -> {
                val hamIstek = gson.fromJson(kayit.payloadJson, AracIstek::class.java)
                val dosyaYolu = hamIstek.resim?.data?.removePrefix("dosya://")
                val istek = fotografiDosyadanGeriYukle(hamIstek)
                val id = kayit.hedefId ?: return true
                val basarili = api.aracGuncelle(id, istek).let { it.isSuccessful && it.body()?.success == true }
                if (basarili && dosyaYolu != null) java.io.File(dosyaYolu).delete()
                basarili
            }
            "sil" -> {
                val id = kayit.hedefId ?: return true
                api.aracSil(id).let { it.isSuccessful && it.body()?.success == true }
            }
            else -> true
        }
    }

    /* ------------------------------ YARDIMCI: OFFLINE KUYRUK ------------------------------ */
    /**
     * KRITIK: Fotografin buyuk base64 verisini asla SQLite satirina
     * (payloadJson) gommeyiz - Android'in CursorWindow'u ~2MB ile sinirli
     * olup, fotografli bir kayit bu siniri kolayca asip
     * "SQLiteBlobTooBigException" ile uygulamayi cokertir. Bunun yerine
     * fotografi ayri bir dosyaya yazar, veritabanina sadece dosya yolunu
     * yaziriz. Sunucuya gonderilirken dosyadan geri okunur.
     */
    private fun fotografiDosyayaTasi(istek: Any?): Any? {
        val resimAlaniVarMi = istek is MalzemeIstek || istek is AracIstek
        if (!resimAlaniVarMi) return istek

        return when (istek) {
            is MalzemeIstek -> {
                val yeniResim = istek.resim?.data?.let { veri -> base64iDosyayaYaz(veri) }
                    ?.let { yol -> istek.resim?.copy(data = "dosya://$yol") }
                if (istek.resim != null && yeniResim == null) istek else istek.copy(resim = yeniResim ?: istek.resim)
            }
            is AracIstek -> {
                val yeniResim = istek.resim?.data?.let { veri -> base64iDosyayaYaz(veri) }
                    ?.let { yol -> istek.resim?.copy(data = "dosya://$yol") }
                if (istek.resim != null && yeniResim == null) istek else istek.copy(resim = yeniResim ?: istek.resim)
            }
            else -> istek
        }
    }

    private fun base64iDosyayaYaz(veri: String): String? {
        return try {
            val klasor = java.io.File(context.cacheDir, "bekleyen_fotolar").apply { if (!exists()) mkdirs() }
            val dosya = java.io.File(klasor, "${java.util.UUID.randomUUID()}.txt")
            dosya.writeText(veri)
            dosya.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun fotografiDosyadanGeriYukle(istek: MalzemeIstek): MalzemeIstek {
        val veri = istek.resim?.data ?: return istek
        if (!veri.startsWith("dosya://")) return istek
        val yol = veri.removePrefix("dosya://")
        val icerik = try { java.io.File(yol).readText() } catch (e: Exception) { null } ?: return istek.copy(resim = null)
        return istek.copy(resim = istek.resim?.copy(data = icerik))
    }

    private fun fotografiDosyadanGeriYukle(istek: AracIstek): AracIstek {
        val veri = istek.resim?.data ?: return istek
        if (!veri.startsWith("dosya://")) return istek
        val yol = veri.removePrefix("dosya://")
        val icerik = try { java.io.File(yol).readText() } catch (e: Exception) { null } ?: return istek.copy(resim = null)
        return istek.copy(resim = istek.resim?.copy(data = icerik))
    }

    private suspend fun kuyrugaEkle(varlik: String, islem: String, hedefId: Int?, payload: Any?) {
        val guvenliPayload = fotografiDosyayaTasi(payload)
        db.bekleyenIslemDao().ekle(
            BekleyenIslemEntity(
                varlik = varlik,
                islemTuru = islem,
                hedefId = hedefId,
                payloadJson = if (guvenliPayload != null) gson.toJson(guvenliPayload) else ""
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
