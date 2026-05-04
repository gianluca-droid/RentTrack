package com.renttrack.app

import android.app.Application
import android.content.Context

class RentTrackApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // â”€â”€ Crash handler: salva l'errore in SharedPreferences â”€â”€â”€â”€â”€â”€
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


