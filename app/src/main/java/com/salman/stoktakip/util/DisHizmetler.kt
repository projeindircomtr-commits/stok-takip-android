package com.salman.stoktakip.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Bir ilcenin anlik hava durumu. */
data class IlceHava(
    val ad: String,
    val sicaklik: Int,
    val minSicaklik: Int,
    val havaKodu: Int,
    val havaMetni: String,
    val ruzgarKmh: Int,
    val riskli: Boolean
)

/**
 * Sitenin footer.php'sindeki ile ayni ucretsiz, anahtar gerektirmeyen
 * API'ler: hava durumu icin open-meteo.com, doviz kuru icin frankfurter.dev.
 */
object DisHizmetler {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val havaKodlari = mapOf(
        0 to "Açık", 1 to "Az Bulutlu", 2 to "Parçalı Bulutlu", 3 to "Kapalı",
        45 to "Sisli", 48 to "Sisli", 51 to "Hafif Çise", 53 to "Çise", 55 to "Yoğun Çise",
        61 to "Hafif Yağmur", 63 to "Yağmur", 65 to "Kuvvetli Yağmur",
        71 to "Hafif Kar", 73 to "Kar", 75 to "Yoğun Kar", 77 to "Kar Taneleri",
        80 to "Sağanak", 81 to "Sağanak", 82 to "Kuvvetli Sağanak",
        85 to "Kar Sağanağı", 86 to "Kuvvetli Kar Sağanağı",
        95 to "Fırtına", 96 to "Fırtına", 99 to "Şiddetli Fırtına"
    )

    /** Kar/don kodlari - kis mucadelesi acisindan "riskli" sayilir. */
    private val karKodlari = setOf(71, 73, 75, 77, 85, 86)

    /** Istanbul'un 39 ilcesi - yaklasik merkez koordinatlari. */
    val istanbulIlceleri: List<Pair<String, Pair<Double, Double>>> = listOf(
        "Adalar" to (40.8767 to 29.1244),
        "Arnavutköy" to (41.1859 to 28.7401),
        "Ataşehir" to (40.9923 to 29.1244),
        "Avcılar" to (40.9793 to 28.7215),
        "Bağcılar" to (41.0389 to 28.8567),
        "Bahçelievler" to (41.0022 to 28.8586),
        "Bakırköy" to (40.9819 to 28.8772),
        "Başakşehir" to (41.0930 to 28.8010),
        "Bayrampaşa" to (41.0453 to 28.9134),
        "Beşiktaş" to (41.0422 to 29.0060),
        "Beykoz" to (41.1350 to 29.0947),
        "Beylikdüzü" to (41.0016 to 28.6403),
        "Beyoğlu" to (41.0370 to 28.9770),
        "Büyükçekmece" to (41.0200 to 28.5850),
        "Çatalca" to (41.1430 to 28.4610),
        "Çekmeköy" to (41.0350 to 29.2100),
        "Esenler" to (41.0450 to 28.8790),
        "Esenyurt" to (41.0350 to 28.6720),
        "Eyüpsultan" to (41.0480 to 28.9340),
        "Fatih" to (41.0186 to 28.9490),
        "Gaziosmanpaşa" to (41.0650 to 28.9150),
        "Güngören" to (41.0170 to 28.8740),
        "Kadıköy" to (40.9833 to 29.0333),
        "Kağıthane" to (41.0800 to 28.9700),
        "Kartal" to (40.9050 to 29.1900),
        "Küçükçekmece" to (41.0000 to 28.7750),
        "Maltepe" to (40.9350 to 29.1550),
        "Pendik" to (40.8770 to 29.2340),
        "Sancaktepe" to (41.0000 to 29.2300),
        "Sarıyer" to (41.1670 to 29.0500),
        "Silivri" to (41.0730 to 28.2470),
        "Sultanbeyli" to (40.9600 to 29.2700),
        "Sultangazi" to (41.1050 to 28.8700),
        "Şile" to (41.1750 to 29.6100),
        "Şişli" to (41.0600 to 28.9870),
        "Tuzla" to (40.8150 to 29.3000),
        "Ümraniye" to (41.0160 to 29.1250),
        "Üsküdar" to (41.0230 to 29.0150),
        "Zeytinburnu" to (40.9950 to 28.9050)
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

    /**
     * Istanbul'un 39 ilcesinin hepsini TEK bir API cagrisiyla getirir
     * (open-meteo virgulle ayrilmis coklu koordinat destekliyor).
     * Don/kar riski olanlar "riskli=true" ile isaretlenir, liste risklileri
     * en basa alacak sekilde siralanir.
     */
    suspend fun tumIlcelerHavaDurumuGetir(): List<IlceHava>? = withContext(Dispatchers.IO) {
        try {
            val enlemler = istanbulIlceleri.joinToString(",") { it.second.first.toString() }
            val boylamlar = istanbulIlceleri.joinToString(",") { it.second.second.toString() }
            val url = "https://api.open-meteo.com/v1/forecast?latitude=$enlemler&longitude=$boylamlar" +
                "&current=temperature_2m,weather_code,wind_speed_10m" +
                "&daily=temperature_2m_min&timezone=auto&forecast_days=1"
            val body = calistir(url) ?: return@withContext null

            // Coklu konum istendiginde open-meteo bir JSON DIZISI doner (her ilce icin bir eleman).
            val dizi = JSONArray(body)
            val sonuc = mutableListOf<IlceHava>()
            for (i in 0 until dizi.length()) {
                val obj = dizi.getJSONObject(i)
                val current = obj.optJSONObject("current") ?: continue
                val daily = obj.optJSONObject("daily")
                val minDizi = daily?.optJSONArray("temperature_2m_min")

                val sicaklik = Math.round(current.optDouble("temperature_2m")).toInt()
                val minSicaklik = if (minDizi != null && minDizi.length() > 0)
                    Math.round(minDizi.optDouble(0)).toInt() else sicaklik
                val kod = current.optInt("weather_code")
                val ruzgar = Math.round(current.optDouble("wind_speed_10m")).toInt()
                val riskli = kod in karKodlari || minSicaklik <= 0 || sicaklik <= 0

                sonuc.add(
                    IlceHava(
                        ad = istanbulIlceleri[i].first,
                        sicaklik = sicaklik,
                        minSicaklik = minSicaklik,
                        havaKodu = kod,
                        havaMetni = havaKodlari[kod] ?: "Bilinmiyor",
                        ruzgarKmh = ruzgar,
                        riskli = riskli
                    )
                )
            }
            sonuc.sortedWith(compareByDescending<IlceHava> { it.riskli }.thenBy { it.minSicaklik })
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
