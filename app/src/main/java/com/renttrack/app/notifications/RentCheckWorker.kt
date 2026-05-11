package com.renttrack.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.renttrack.app.data.repository.SupabaseRentRepository

/**
 * Worker eseguito ogni giorno da WorkManager.
 * Controlla Supabase e invia notifiche per:
 *  - Contratti scaduti
 *  - Contratti in scadenza entro 30 giorni
 *  - Cedolini scaduti non pagati
 *
 * Migrato da Room → SupabaseRentRepository (cloud-first).
 */
class RentCheckWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            val token = prefs.getString("auth_token", null)

            // Nessun token → utente non loggato, niente notifiche
            if (token.isNullOrBlank()) return Result.success()

            val repo = SupabaseRentRepository(prefs)

            val now  = System.currentTimeMillis()
            val day  = 24L * 60 * 60 * 1000
            val in30 = now + 30 * day

            // ── Recupera tutti i condomini dell'utente ─────────────────────────
            val condomini = repo.getCondomini()
            if (condomini.isEmpty()) return Result.success()

            var unitsScadute    = mutableListOf<String>()   // nomi inquilini contratto scaduto
            var unitsInScadenza = mutableListOf<String>()   // nomi inquilini contratto in scadenza
            var cedoliniScadutiCount = 0
            var cedoliniScadutiTotale = 0.0

            for (condo in condomini) {
                val units    = repo.getUnitsByCondominio(condo.id)
                val cedolini = repo.getCedoliniByCondominio(condo.id)

                // ── Contratti scaduti ──────────────────────────────────────────
                units.filter { it.leaseEndDate != null && it.leaseEndDate < now }
                    .forEach { unitsScadute.add(it.ownerName) }

                // ── Contratti in scadenza entro 30 giorni ─────────────────────
                units.filter {
                    it.leaseEndDate != null &&
                    it.leaseEndDate >= now &&
                    it.leaseEndDate <= in30
                }.forEach { unitsInScadenza.add(it.ownerName) }

                // ── Cedolini scaduti non pagati ───────────────────────────────
                cedolini.filter { it.status != "Pagato" && it.dueDate < now }
                    .forEach {
                        cedoliniScadutiCount++
                        cedoliniScadutiTotale += (it.total - it.paidAmount)
                    }
            }

            // ── Invia notifiche ────────────────────────────────────────────────
            if (unitsScadute.isNotEmpty()) {
                NotificationHelper.notifyContrattiScaduti(
                    context, unitsScadute.size, unitsScadute
                )
            }
            if (unitsInScadenza.isNotEmpty()) {
                NotificationHelper.notifyContrattiInScadenza(
                    context, unitsInScadenza.size, 30, unitsInScadenza
                )
            }
            if (cedoliniScadutiCount > 0) {
                NotificationHelper.notifyCedoliniScaduti(
                    context, cedoliniScadutiCount, cedoliniScadutiTotale
                )
            }

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("RentCheckWorker", "Errore controllo notifiche: ${e.message}", e)
            Result.retry()
        }
    }
}
