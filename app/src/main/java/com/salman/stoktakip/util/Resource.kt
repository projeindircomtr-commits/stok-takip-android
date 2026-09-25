package com.salman.stoktakip.util

sealed class Resource<out T> {
    data class Basarili<T>(val data: T) : Resource<T>()
    data class Hata(val mesaj: String, val kod: String? = null) : Resource<Nothing>()
    object Yukleniyor : Resource<Nothing>()
}

/** Basit ag baglanti kontrolu. */
object AgDurumu {
    fun bagliMi(context: android.content.Context): Boolean {
        val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE)
                as android.net.ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

/**
 * Ham Java/Kotlin exception mesajlarini ("Unable to resolve host...",
 * "timeout" vb.) kullaniciya gosterilecek anlasilir Turkce mesaja cevirir.
 * Tum repository fonksiyonlarinin catch bloklarinda kullanilir.
 */
fun agHatasiniAcikla(e: Exception): String {
    return when (e) {
        is java.net.UnknownHostException -> "İnternet bağlantınız yok, lütfen kontrol edin."
        is java.net.SocketTimeoutException -> "Sunucu yanıt vermedi, internet bağlantınızı kontrol edip tekrar deneyin."
        is java.net.ConnectException -> "Sunucuya ulaşılamadı, lütfen daha sonra tekrar deneyin."
        is javax.net.ssl.SSLException -> "Güvenli bağlantı kurulamadı, lütfen tekrar deneyin."
        else -> "Beklenmeyen bir hata oluştu, lütfen tekrar deneyin."
    }
}

/** Resmi Base64 data-url formatina cevirir (mobile-api/bootstrap.php ile uyumlu). */
fun bitmapDosyasiniBase64eCevir(context: android.content.Context, uri: android.net.Uri): com.salman.stoktakip.data.remote.dto.ResimYuku? {
    return try {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        val bytes = input.use { it.readBytes() }
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        com.salman.stoktakip.data.remote.dto.ResimYuku(
            data = "data:$mime;base64,$base64",
            type = mime
        )
    } catch (e: Exception) {
        null
    }
}

/** Kamera ile cekilecek fotograf icin FileProvider uzerinden gecici bir Uri uretir. */
fun kameraIcinGeciciUriOlustur(context: android.content.Context): android.net.Uri {
    val klasor = java.io.File(context.cacheDir, "images").apply { if (!exists()) mkdirs() }
    val dosya = java.io.File(klasor, "kamera_${System.currentTimeMillis()}.jpg")
    return androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", dosya)
}

/** Sunucudan gelen "YYYY-MM-DD HH:MM:SS" formatini "DD.MM.YYYY HH:MM" olarak gosterir. */
fun tarihiBicimlendir(sunucuTarihi: String): String {
    return try {
        val kaynak = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
        val hedef = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.US)
        val tarih = kaynak.parse(sunucuTarihi) ?: return sunucuTarihi
        hedef.format(tarih)
    } catch (e: Exception) {
        sunucuTarihi
    }
}
