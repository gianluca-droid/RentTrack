package com.renttrack.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.renttrack.app.data.repository.SupabaseRentRepository

/**
 * Worker eseguito ogni giorno da WorkManager.
 * Controlla Supabase e invia notifiche per:
 *  - Contratti scaduti / in scadenza
 *  - Cedolini scaduti non pagati
 *  - Reminder pre-scadenza cedolini (configurabile: default 3 giorni prima + giorno stesso)
 *
 * Configurazione reminder leggibile da SharedPreferences "renttrack_prefs":
 *   reminder_days_before (Int, default 3) — giorni prima della scadenza
 *   reminder_same_day    (Boolean, default true) — notifica anche il giorno stesso
 */
class RentCheckWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        // Key SharedPreferences configurazione reminder
        const val PREF_REMINDER_DAYS   = "reminder_days_before"
        const val PREF_REMINDER_SAMEDAY = "reminder_same_day"
        const val DEFAULT_REMINDER_DAYS = 3
    }

    override suspend fun doWork(): Result {
        return try {
            val prefs = com.renttrack.app.SecurePrefs.get(context)
            val token = prefs.getString("auth_token", null)

            // Nessun token → utente non loggato, niente notifiche
            if (token.isNullOrBlank()) return Result.success()

            val repo = SupabaseRentRepository(prefs)

            // Leggi configurazione reminder
            val reminderDaysBefore = prefs.getInt(PREF_REMINDER_DAYS, DEFAULT_REMINDER_DAYS)
            val reminderSameDay    = prefs.getBoolean(PREF_REMINDER_SAMEDAY, true)

            val now    = System.currentTimeMillis()
            val dayMs  = 24L * 60 * 60 * 1000
            val in30   = now + 30 * dayMs

            // ── Recupera tutti i condomini dell'utente ─────────────────────────
            val condomini = repo.getCondomini()
            if (condomini.isEmpty()) return Result.success()

            val unitsScadute    = mutableListOf<String>()
            val unitsInScadenza = mutableListOf<String>()
            var cedoliniScadutiCount  = 0
            var cedoliniScadutiTotale = 0.0

            // Mappa: giorniRimasti → (nomi inquilini, totale) per reminder
            // Gestiamo: 0 (oggi) e reminderDaysBefore (es. 3)
            data class ReminderBucket(val nomi: MutableList<String> = mutableListOf(), var totale: Double = 0.0)
            val reminderBuckets = mutableMapOf<Int, ReminderBucket>()
            if (reminderSameDay)      reminderBuckets[0] = ReminderBucket()
            if (reminderDaysBefore > 0) reminderBuckets[reminderDaysBefore] = ReminderBucket()

            for (condo in condomini) {
                val units    = repo.getUnitsByCondominio(condo.id)
                val cedolini = repo.getCedoliniByCondominio(condo.id)
                val unitMap  = units.associateBy { it.id }

                // ── Contratti scaduti ──────────────────────────────────────────
                units.filter { it.leaseEndDate != null && it.leaseEndDate < now }
                    .forEach { unitsScadute.add(it.ownerName) }

                // ── Contratti in scadenza entro 30 giorni ─────────────────────
                units.filter {
                    it.leaseEndDate != null &&
                    it.leaseEndDate >= now &&
                    it.leaseEndDate <= in30
                }.forEach { unitsInScadenza.add(it.ownerName) }

                // ── Cedolini non pagati ────────────────────────────────────────
                val unpaid = cedolini.filter { it.status != "Pagato" }

                // Cedolini già scaduti
                unpaid.filter { it.dueDate < now }.forEach {
                    cedoliniScadutiCount++
                    cedoliniScadutiTotale += (it.total - it.paidAmount)
                }

                // Reminder: cedolini che scadono tra esattamente N giorni o oggi
                unpaid.filter { it.dueDate >= now }.forEach { ced ->
                    val giorni = ((ced.dueDate - now) / dayMs).toInt()
                    val bucket = reminderBuckets[giorni]
                    if (bucket != null) {
                        val tenantName = unitMap[ced.unitId]?.ownerName ?: "Inquilino"
                        bucket.nomi.add(tenantName)
                        bucket.totale += (ced.total - ced.paidAmount)
                    }
                }
            }

            // ── Invia notifiche scadenze contratti ────────────────────────────
            if (unitsScadute.isNotEmpty()) {
                NotificationHelper.notifyContrattiScaduti(context, unitsScadute.size, unitsScadute)
            }
            if (unitsInScadenza.isNotEmpty()) {
                NotificationHelper.notifyContrattiInScadenza(context, unitsInScadenza.size, 30, unitsInScadenza)
            }

            // ── Invia notifica cedolini scaduti ───────────────────────────────
            if (cedoliniScadutiCount > 0) {
                NotificationHelper.notifyCedoliniScaduti(context, cedoliniScadutiCount, cedoliniScadutiTotale)
            }

            // ── Invia reminder pre-scadenza ───────────────────────────────────
            reminderBuckets.forEach { (giorniRimasti, bucket) ->
                if (bucket.nomi.isNotEmpty()) {
                    NotificationHelper.notifyReminderScadenza(
                        context       = context,
                        count         = bucket.nomi.size,
                        totale        = bucket.totale,
                        nomi          = bucket.nomi,
                        giorniRimasti = giorniRimasti
                    )
                }
            }

            Result.success()
        } catch (e: Exception) {
            // Log dettagliato solo in build di sviluppo (non esposto in release)
            if ((context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                android.util.Log.e("RentCheckWorker", "Errore controllo notifiche: ${e.message}", e)
            }
            Result.retry()
        }
    }
}
