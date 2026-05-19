package com.renttrack.app

import android.app.Application
import android.content.Context
import androidx.work.*
import com.renttrack.app.notifications.NotificationHelper
import com.renttrack.app.notifications.RentCheckWorker
import java.util.concurrent.TimeUnit

class RentTrackApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // ── Crash handler ────────────────────────────────────────────
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val msg = buildString {
                    append("CRASH: ${throwable::class.simpleName}\n")
                    append("${throwable.message}\n\n")
                    throwable.stackTrace.take(8).forEach { append("  at $it\n") }
                }
                getSharedPreferences("crash_prefs", Context.MODE_PRIVATE)
                    .edit().putString("last_crash", msg).commit()
            } catch (_: Exception) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // ── Canali notifiche (richiesti da Android 8+) ───────────────
        NotificationHelper.createChannels(this)

        // ── WorkManager: check giornaliero scadenze e morosità ───────
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val dailyWork = PeriodicWorkRequestBuilder<RentCheckWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            // Nessun delay: prima esecuzione entro 15 min dall'avvio
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "rent_daily_check",
            ExistingPeriodicWorkPolicy.UPDATE,  // sostituisce se versione precedente era KEEP
            dailyWork
        )
    }
}
