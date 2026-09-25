package com.salman.stoktakip

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.salman.stoktakip.data.SessionManager
import com.salman.stoktakip.sync.EvrakUyariWorker
import com.salman.stoktakip.sync.HavaUyariWorker
import com.salman.stoktakip.sync.SyncWorker
import java.util.concurrent.TimeUnit

class StokApp : Application() {

    lateinit var session: SessionManager
        private set

    override fun onCreate() {
        super.onCreate()
        session = SessionManager(this)
        planlaPeriyodikSenkron()
        baglantiDegisimindeSenkronizeEt()
        planlaHavaUyarisi()
        planlaEvrakUyarisi()
    }

    /** Yedek plan: WorkManager 30 dakikada bir bekleyen kayitlari kontrol eder. */
    private fun planlaPeriyodikSenkron() {
        val kisitlamalar = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val istek = PeriodicWorkRequestBuilder<SyncWorker>(30, TimeUnit.MINUTES)
            .setConstraints(kisitlamalar)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "otomatik_senkron",
            ExistingPeriodicWorkPolicy.KEEP,
            istek
        )
    }

    /**
     * Kis mucadelesi icin: Istanbul'un 39 ilcesini 3 saatte bir arka planda
     * kontrol eder, don/kar riski varsa bildirim gonderir. Uygulama kapali
     * olsa bile calisir (WorkManager sistem tarafindan tetiklenir).
     */
    private fun planlaHavaUyarisi() {
        val kisitlamalar = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val istek = PeriodicWorkRequestBuilder<HavaUyariWorker>(3, TimeUnit.HOURS)
            .setConstraints(kisitlamalar)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "hava_uyari_kontrolu",
            ExistingPeriodicWorkPolicy.KEEP,
            istek
        )
    }

    /** Gunde bir kez, suresi dolmak uzere olan evraklar icin kontrol eder. */
    private fun planlaEvrakUyarisi() {
        val kisitlamalar = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val istek = PeriodicWorkRequestBuilder<EvrakUyariWorker>(24, TimeUnit.HOURS)
            .setConstraints(kisitlamalar)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "evrak_uyari_kontrolu",
            ExistingPeriodicWorkPolicy.KEEP,
            istek
        )
    }

    /**
     * Asil mekanizma: telefon internete her baglandiginda (wifi acilinca,
     * ucak modu kapaninca, mobil veri gelince vb.) ANINDA senkronizasyonu
     * tetikler - 30 dakika beklemez.
     */
    private fun baglantiDegisimindeSenkronizeEt() {
        val cm = getSystemService(ConnectivityManager::class.java) ?: return
        val istek = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        cm.registerNetworkCallback(istek, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (!session.girisYapilmisMi) return
                val anlikIstek = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build()
                WorkManager.getInstance(this@StokApp).enqueueUniqueWork(
                    "anlik_senkron",
                    ExistingWorkPolicy.REPLACE,
                    anlikIstek
                )
            }
        })
    }
}
