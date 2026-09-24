package com.salman.stoktakip

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.salman.stoktakip.data.SessionManager
import com.salman.stoktakip.sync.SyncWorker
import java.util.concurrent.TimeUnit

class StokApp : Application() {

    lateinit var session: SessionManager
        private set

    override fun onCreate() {
        super.onCreate()
        session = SessionManager(this)
        planlaPeriyodikSenkron()
    }

    /** Baglanti geri geldiginde bekleyen kayitlari otomatik gondermeye calisir. */
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
}
