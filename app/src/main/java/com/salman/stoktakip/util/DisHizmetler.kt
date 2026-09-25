package com.salman.stoktakip.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Sitenin footer.php'sindeki ile ayni ucretsiz, anahtar gerektirmeyen
 * API'ler: hava durumu icin open-meteo.com, doviz kuru icin frankfurter.dev.
 */
object DisHizmetler {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val havaKodlari = mapOf(
        0 to "Açık", 1 to "Az Bulutlu", 2 to "Parçalı Bulutlu", 3 to "Kapalı",
        45 to "Sisli", 48 to "Sisli", 51 to "Hafif Çise", 53 to "Çise", 55 to "Yoğun Çise",
        61 to "Hafif Yağmur", 63 to "Yağmur", 65 to "Kuvvetli Yağmur",
        71 to "Hafif Kar", 73 to "Kar", 75 to "Yoğun Kar",
        80 to "Sağanak", 81 to "Sağanak", 82 to "Kuvvetli Sağanak",
        95 to "Fırtına", 96 to "Fırtına", 99 to "Şiddetli Fırtına"
    )

    /** Istanbul icin anlik sicaklik + hava durumu metni. Basarisiz olursa null doner. */
    suspend fun havaDurumuGetir(): String? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.open-meteo.com/v1/forecast?latitude=41.0082&longitude=28.9784&current=temperature_2m,weather_code&timezone=auto"
            val body = calistir(url) ?: return@withContext null
            val current = JSONObject(body).optJSONObject("current") ?: return@withContext null
            val sicaklik = Math.round(current.optDouble("temperature_2m"))
            val kod = current.optInt("weather_code")
            "$sicaklik°C • ${havaKodlari[kod] ?: "Bilinmiyor"}"
        } catch (e: Exception) {
            null
        }
    }

    /** USD/TRY veya EUR/TRY kurunu getirir. Basarisiz olursa null doner. */
    suspend fun kurGetir(kaynakParaBirimi: String): Double? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.frankfurter.dev/v2/rate/$kaynakParaBirimi/TRY"
            val body = calistir(url) ?: return@withContext null
            val deger = JSONObject(body).optDouble("rate", Double.NaN)
            if (deger.isNaN()) null else deger
        } catch (e: Exception) {
            null
        }
    }

    private fun calistir(url: String): String? {
        val istek = Request.Builder().url(url).get().build()
        client.newCall(istek).execute().use { cevap ->
            if (!cevap.isSuccessful) return null
            return cevap.body?.string()
        }
    }
}
