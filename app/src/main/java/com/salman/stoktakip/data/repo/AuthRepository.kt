package com.salman.stoktakip.data.repo

import com.salman.stoktakip.data.SessionManager
import com.salman.stoktakip.data.remote.ApiService
import com.salman.stoktakip.data.remote.dto.GirisIstek
import com.salman.stoktakip.util.Resource

class AuthRepository(
    private val api: ApiService,
    private val session: SessionManager
) {
    suspend fun girisYap(kullaniciAdi: String, sifre: String): Resource<Unit> {
        return try {
            val cevap = api.girisYap(GirisIstek(kullaniciAdi, sifre))
            val govde = cevap.body()
            if (cevap.isSuccessful && govde?.success == true && govde.data != null) {
                session.token = govde.data.token
                session.kullaniciId = govde.data.kullanici.id
                session.adSoyad = govde.data.kullanici.adSoyad
                session.kullaniciAdi = govde.data.kullanici.kullaniciAdi
                session.rol = govde.data.kullanici.rol
                Resource.Basarili(Unit)
            } else {
                Resource.Hata(govde?.message ?: "Giriş başarısız oldu.", govde?.code)
            }
        } catch (e: Exception) {
            Resource.Hata(e.message ?: "Sunucuya bağlanılamadı.")
        }
    }

    suspend fun cikisYap() {
        try { api.cikisYap() } catch (e: Exception) { /* offline olabilir, sorun degil */ }
        session.oturumTemizle()
    }
}
