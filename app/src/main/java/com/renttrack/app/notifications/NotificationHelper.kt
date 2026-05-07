package com.renttrack.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.renttrack.app.MainActivity
import com.renttrack.app.R

object NotificationHelper {

    const val CHANNEL_SCADENZE   = "renttrack_scadenze"
    const val CHANNEL_MOROSITA   = "renttrack_morosita"

    /** Crea i canali notifiche (chiamare all'avvio dell'app) */
    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SCADENZE,
                "Scadenze contratti",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Avvisi per contratti in scadenza" }
        )

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MOROSITA,
                "Cedolini e morosità",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Avvisi per cedolini scaduti non pagati" }
        )
    }

    private fun launchIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Notifica: contratto/i scaduto/i */
    fun notifyContrattiScaduti(context: Context, count: Int, nomi: List<String>) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val body = if (nomi.size <= 3) nomi.joinToString(", ")
                   else "${nomi.take(3).joinToString(", ")} e altri ${nomi.size - 3}"
        val notif = NotificationCompat.Builder(context, CHANNEL_SCADENZE)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ $count contratt${if (count == 1) "o scaduto" else "i scaduti"}")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Inquilini: $body"))
            .setContentIntent(launchIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        nm.notify(1001, notif)
    }

    /** Notifica: contratto/i in scadenza entro N giorni */
    fun notifyContrattiInScadenza(context: Context, count: Int, giorni: Int, nomi: List<String>) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val body = if (nomi.size <= 3) nomi.joinToString(", ")
                   else "${nomi.take(3).joinToString(", ")} e altri ${nomi.size - 3}"
        val notif = NotificationCompat.Builder(context, CHANNEL_SCADENZE)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📅 $count contratt${if (count == 1) "o scade" else "i scadono"} entro $giorni giorni")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Inquilini: $body"))
            .setContentIntent(launchIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        nm.notify(1002, notif)
    }

    /** Notifica: cedolini scaduti non pagati */
    fun notifyCedoliniScaduti(context: Context, count: Int, totale: Double) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val totaleStr = String.format("%.2f €", totale)
        val notif = NotificationCompat.Builder(context, CHANNEL_MOROSITA)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🔴 $count cedolin${if (count == 1) "o scaduto" else "i scaduti"} non pagati")
            .setContentText("Totale da incassare: $totaleStr")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Hai $count cedolin${if (count == 1) "o" else "i"} non pagati per un totale di $totaleStr. Apri l'app per i dettagli."))
            .setContentIntent(launchIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        nm.notify(1003, notif)
    }
}
