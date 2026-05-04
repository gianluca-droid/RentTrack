package com.condogest.app

import android.app.Application
import android.content.Context

class CondoGestApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // ── Crash handler: salva l'errore in SharedPreferences ──────
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
    }
}
