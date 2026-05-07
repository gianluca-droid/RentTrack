package com.renttrack.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.renttrack.app.data.database.AppDatabase
import com.renttrack.app.data.repository.RentRepository
import kotlinx.coroutines.flow.first

/**
 * Worker eseguito ogni giorno da WorkManager.
 * Controlla il DB e invia notifiche per:
 *  - Contratti scaduti
 *  - Contratti in scadenza entro 30 giorni
 *  - Cedolini scaduti non pagati
 */
class RentCheckWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(context)
            val repo = RentRepository(
                db.condominioDao(), db.unitDao(), db.expenseDao(),
                db.paymentDao(), db.cedolinoDao(), db.documentoDao(),
                db.tenantHistoryDao()
            )

            val now  = System.currentTimeMillis()
            val day  = 24L * 60 * 60 * 1000
            val in30 = now + 30 * day

            // ── Recupera tutte le unità e cedolini (globale, tutti i condomini) ──
            val allUnits    = repo.getAllUnitsGlobal().first()
            val allCedolini = repo.getAllCedoliniGlobal().first()

            // ── Contratti scaduti ──────────────────────────────────────────────
            val scaduti = allUnits.filter {
                it.leaseEndDate != null && it.leaseEndDate < now
            }
            if (scaduti.isNotEmpty()) {
                NotificationHelper.notifyContrattiScaduti(
                    context, scaduti.size, scaduti.map { it.ownerName }
                )
            }

            // ── Contratti in scadenza entro 30 giorni ─────────────────────────
            val inScadenza = allUnits.filter {
                it.leaseEndDate != null &&
                it.leaseEndDate >= now &&
                it.leaseEndDate <= in30
            }
            if (inScadenza.isNotEmpty()) {
                NotificationHelper.notifyContrattiInScadenza(
                    context, inScadenza.size, 30, inScadenza.map { it.ownerName }
                )
            }

            // ── Cedolini scaduti non pagati ───────────────────────────────────
            val cedoliniScaduti = allCedolini.filter {
                it.status != "Pagato" && it.dueDate < now
            }
            if (cedoliniScaduti.isNotEmpty()) {
                val totale = cedoliniScaduti.sumOf { it.total - it.paidAmount }
                NotificationHelper.notifyCedoliniScaduti(
                    context, cedoliniScaduti.size, totale
                )
            }

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("RentCheckWorker", "Errore controllo notifiche: ${e.message}", e)
            Result.retry()
        }
    }
}
