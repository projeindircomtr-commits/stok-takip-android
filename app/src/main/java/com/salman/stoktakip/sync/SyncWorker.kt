package com.salman.stoktakip.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.salman.stoktakip.data.ServiceLocator
import com.salman.stoktakip.util.Resource

class SyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repo = ServiceLocator.stokRepository(applicationContext)
        return when (repo.bekleyenleriSenkronize()) {
            is Resource.Basarili -> Result.success()
            is Resource.Hata -> Result.retry()
            else -> Result.retry()
        }
    }
}
