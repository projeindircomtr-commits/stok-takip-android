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
