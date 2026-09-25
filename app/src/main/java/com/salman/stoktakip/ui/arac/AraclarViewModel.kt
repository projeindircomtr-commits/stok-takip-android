package com.salman.stoktakip.ui.arac

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.salman.stoktakip.data.ServiceLocator
import com.salman.stoktakip.data.remote.dto.AracIstek
import com.salman.stoktakip.data.remote.dto.ResimYuku
import com.salman.stoktakip.util.Resource
import kotlinx.coroutines.launch

class AraclarViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ServiceLocator.stokRepository(app)

    val araclar = repo.araclarGozlemle().asLiveData()
    val kategoriler = repo.kategorilerGozlemle().asLiveData()
    val lokasyonlar = repo.lokasyonlarGozlemle().asLiveData()
    val bekleyenSayi = repo.bekleyenSayiGozlemle().asLiveData()

    val islemSonucu = MutableLiveData<Resource<Unit>>()
    val yenileniyor = MutableLiveData(false)

    fun yenile() {
        viewModelScope.launch {
            yenileniyor.value = true
            repo.tumVeriyiYenile()
            yenileniyor.value = false
        }
    }

    fun ekle(
        aracIsmi: String, model: String?, plaka: String?, kamera: String, gps: String,
        sahip: String?, telefon: String?, kategoriId: Int?, lokasyonId: Int?, resim: ResimYuku?
    ) {
        viewModelScope.launch {
            islemSonucu.value = repo.aracEkle(
                AracIstek(aracIsmi, model, plaka, kamera, gps, sahip, telefon, kategoriId, lokasyonId, resim)
            )
        }
    }

    fun guncelle(
        id: Int, aracIsmi: String, model: String?, plaka: String?, kamera: String, gps: String,
        sahip: String?, telefon: String?, kategoriId: Int?, lokasyonId: Int?, resim: ResimYuku?
    ) {
        viewModelScope.launch {
            islemSonucu.value = repo.aracGuncelle(
                id, AracIstek(aracIsmi, model, plaka, kamera, gps, sahip, telefon, kategoriId, lokasyonId, resim)
            )
        }
    }

    fun sil(id: Int) {
        viewModelScope.launch {
            islemSonucu.value = repo.aracSil(id)
        }
    }
}
