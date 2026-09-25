package com.salman.stoktakip.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.salman.stoktakip.data.ServiceLocator
import com.salman.stoktakip.ui.main.MainActivity
import com.salman.stoktakip.util.Resource

/**
 * Gunde bir kez calisir (bkz. StokApp), suresi 15 gun icinde dolacak ya
 * da dolmus evraklar icin yerel bildirim gosterir. Firebase/sunucu
 * GEREKMEZ - tamamen telefonun kendi sisteminde calisir.
 */
class EvrakUyariWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val session = ServiceLocator.session(applicationContext)
            if (!session.girisYapilmisMi) return Result.success()

            val repo = ServiceLocator.stokRepository(applicationContext)
            val sonuc = repo.evraklarGetir()
            if (sonuc !is Resource.Basarili) return Result.retry()

            val riskliler = sonuc.data.filter { it.kalanGun <= 15 }
            if (riskliler.isNotEmpty()) {
                val ornekler = riskliler.take(3).joinToString(", ") { it.ilgiliAd }
                bildirimGoster(riskliler.size, ornekler)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun bildirimGoster(sayi: Int, ornekler: String) {
        val context = applicationContext
        val kanalId = "evrak_uyari_kanali"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val kanal = NotificationChannel(
                kanalId, "Evrak Süre Uyarıları", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Süresi dolmak üzere olan evraklar için uyarılar" }
            val nm = context.getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(kanal)
        }

        val acmaIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 1, acmaIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bildirim = NotificationCompat.Builder(context, kanalId)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("📄 Süresi Dolan Evrak — $sayi kayıt")
            .setContentText("$ornekler ve diğerleri. Detaylar için dokunun.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$ornekler ve diğer evrakların süresi 15 gün içinde doluyor veya doldu."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(2002, bildirim)
        } catch (e: SecurityException) {
            // Bildirim izni yok - sessizce gec.
        }
    }
}
