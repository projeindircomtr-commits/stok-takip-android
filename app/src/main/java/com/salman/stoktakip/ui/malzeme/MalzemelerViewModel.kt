package com.salman.stoktakip.ui.malzeme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.salman.stoktakip.data.ServiceLocator
import com.salman.stoktakip.data.remote.dto.MalzemeIstek
import com.salman.stoktakip.data.remote.dto.ResimYuku
import com.salman.stoktakip.util.Resource
import kotlinx.coroutines.launch

class MalzemelerViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ServiceLocator.stokRepository(app)

    val malzemeler = repo.malzemelerGozlemle().asLiveData()
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

    fun ekle(ad: String, miktar: Double, birim: String, kategoriId: Int?, lokasyonId: Int?, resim: ResimYuku?) {
        viewModelScope.launch {
            islemSonucu.value = repo.malzemeEkle(MalzemeIstek(ad, miktar, birim, kategoriId, lokasyonId, resim))
        }
    }

    fun guncelle(id: Int, ad: String, miktar: Double, birim: String, kategoriId: Int?, lokasyonId: Int?, resim: ResimYuku?) {
        viewModelScope.launch {
            islemSonucu.value = repo.malzemeGuncelle(id, MalzemeIstek(ad, miktar, birim, kategoriId, lokasyonId, resim))
        }
    }

    fun sil(id: Int) {
        viewModelScope.launch {
            islemSonucu.value = repo.malzemeSil(id)
        }
    }
}
