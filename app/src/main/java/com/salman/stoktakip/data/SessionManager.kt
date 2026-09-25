package com.salman.stoktakip.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Oturum bilgilerini (token, kullanici bilgisi) cihazda AES-256 ile
 * sifrelenmis olarak saklar. Root olmayan bir cihazda bu dosya
 * disaridan okunamaz.
 */
class SessionManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_session_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var kullaniciId: Int
        get() = prefs.getInt(KEY_USER_ID, 0)
        set(value) = prefs.edit().putInt(KEY_USER_ID, value).apply()

    var adSoyad: String?
        get() = prefs.getString(KEY_AD_SOYAD, null)
        set(value) = prefs.edit().putString(KEY_AD_SOYAD, value).apply()

    var kullaniciAdi: String?
        get() = prefs.getString(KEY_KULLANICI_ADI, null)
        set(value) = prefs.edit().putString(KEY_KULLANICI_ADI, value).apply()

    var rol: String?
        get() = prefs.getString(KEY_ROL, null)
        set(value) = prefs.edit().putString(KEY_ROL, value).apply()

    val girisYapilmisMi: Boolean
        get() = !token.isNullOrEmpty()

    val adminMi: Boolean
        get() = rol == "admin"

    fun oturumTemizle() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_AD_SOYAD = "ad_soyad"
        private const val KEY_KULLANICI_ADI = "kullanici_adi"
        private const val KEY_ROL = "rol"
    }
}
