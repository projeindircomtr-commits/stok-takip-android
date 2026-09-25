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
import com.salman.stoktakip.ui.main.MainActivity
import com.salman.stoktakip.util.DisHizmetler

/**
 * Telefon/uygulama kapali olsa bile WorkManager tarafindan periyodik
 * calistirilir (bkz. StokApp). Istanbul'un 39 ilcesini kontrol eder,
 * don/kar riski olan ilce varsa yerel bir bildirim gosterir.
 * Firebase/sunucu GEREKMEZ - tamamen telefonun kendi sisteminde calisir.
 */
class HavaUyariWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val liste = DisHizmetler.tumIlcelerHavaDurumuGetir() ?: return Result.retry()
            val riskliler = liste.filter { it.riskli }
            if (riskliler.isNotEmpty()) {
                bildirimGoster(riskliler.size, riskliler.take(3).joinToString(", ") { it.ad })
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun bildirimGoster(sayi: Int, ornekIlceler: String) {
        val context = applicationContext
        val kanalId = "hava_uyari_kanali"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val kanal = NotificationChannel(
                kanalId, "Hava Durumu Uyarıları", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Don/kar riski olan ilçeler için uyarılar" }
            val nm = context.getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(kanal)
        }

        val acmaIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("acilacak_ekran", "hava_durumu")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, acmaIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bildirim = NotificationCompat.Builder(context, kanalId)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("⚠️ Don/Kar Riski — $sayi ilçe")
            .setContentText("$ornekIlceler ve diğerleri. Detaylar için dokunun.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$ornekIlceler ve diğer ilçelerde donma/kar riski var. Araç ve ekip hazırlığını kontrol edin."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(2001, bildirim)
        } catch (e: SecurityException) {
            // Bildirim izni verilmemis - sessizce gec, uygulama icindeki
            // uyari banner'i (HavaDurumuFragment) zaten calisiyor olacak.
        }
    }
}
