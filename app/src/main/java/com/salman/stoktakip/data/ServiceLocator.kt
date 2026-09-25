package com.salman.stoktakip.data

import android.content.Context
import com.salman.stoktakip.StokApp
import com.salman.stoktakip.data.remote.ApiService
import com.salman.stoktakip.data.remote.RetrofitClient
import com.salman.stoktakip.data.repo.AuthRepository
import com.salman.stoktakip.data.repo.StokRepository

/**
 * Basit servis konumlandirici. Hilt/Dagger gibi ek build karmasikligi
 * getirmeden tum baglanti/oturum nesnelerini tek noktadan saglar.
 */
object ServiceLocator {

    @Volatile private var api: ApiService? = null
    @Volatile private var stokRepo: StokRepository? = null
    @Volatile private var authRepo: AuthRepository? = null

    fun session(context: Context): SessionManager =
        (context.applicationContext as StokApp).session

    private fun api(context: Context): ApiService = api ?: synchronized(this) {
        api ?: RetrofitClient.create(session(context)).also { api = it }
    }

    fun stokRepository(context: Context): StokRepository = stokRepo ?: synchronized(this) {
        stokRepo ?: StokRepository(context.applicationContext, api(context)).also { stokRepo = it }
    }

    fun authRepository(context: Context): AuthRepository = authRepo ?: synchronized(this) {
        authRepo ?: AuthRepository(api(context), session(context)).also { authRepo = it }
    }
}
