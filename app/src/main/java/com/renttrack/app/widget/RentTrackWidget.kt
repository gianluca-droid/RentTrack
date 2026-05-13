package com.renttrack.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.renttrack.app.MainActivity
import com.renttrack.app.R
import com.renttrack.app.data.repository.SupabaseRentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Widget Android per la home screen.
 * Mostra: Saldo (incassi - spese), cedolini aperti, prossima scadenza.
 * Tap → apre MainActivity (Dashboard).
 *
 * Aggiornato ogni ora tramite updatePeriodMillis nel widget_rent_track_info.xml.
 */
class RentTrackWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    companion object {
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_rent_track)

            // ── Intent: tap sul widget → apre l'app ───────────────────────
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(android.R.id.content, pendingIntent)

            // ── Ora aggiornamento ──────────────────────────────────────────
            val timeFmt = SimpleDateFormat("HH:mm", Locale.ITALIAN)
            views.setTextViewText(R.id.widget_update_time, timeFmt.format(Date()))

            // ── Dati da Supabase (caricati in background) ──────────────────
            val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            val token = prefs.getString("auth_token", null)

            if (token.isNullOrBlank()) {
                // Non loggato: mostra stato vuoto
                views.setTextViewText(R.id.widget_saldo, "Accedi")
                views.setTextViewText(R.id.widget_open_count, "--")
                views.setTextViewText(R.id.widget_next_due, "Apri RentTrack")
                appWidgetManager.updateAppWidget(widgetId, views)
                return
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repo      = SupabaseRentRepository(prefs)
                    val condomini = repo.getCondomini()

                    var totalPay      = 0.0
                    var totalExp      = 0.0
                    var openCedolini  = 0
                    var nextDueDate   = Long.MAX_VALUE
                    var nextDueName   = ""
                    val now           = System.currentTimeMillis()

                    for (condo in condomini) {
                        val units    = repo.getUnitsByCondominio(condo.id)
                        val expenses = repo.getExpensesByCondominio(condo.id)
                        val payments = repo.getPaymentsByCondominio(condo.id)
                        val cedolini = repo.getCedoliniByCondominio(condo.id)
                        val unitMap  = units.associateBy { it.id }

                        totalPay += payments.sumOf { it.amount }
                        totalExp += expenses.sumOf { it.amount }

                        cedolini.filter { it.status != "Pagato" }.forEach { ced ->
                            openCedolini++
                            if (ced.dueDate in (now + 1)..nextDueDate) {
                                nextDueDate = ced.dueDate
                                nextDueName = unitMap[ced.unitId]?.ownerName ?: ""
                            }
                        }
                    }

                    val saldo   = totalPay - totalExp
                    val dateFmt = SimpleDateFormat("dd/MM/yy", Locale.ITALIAN)

                    val saldoStr = if (saldo >= 0) "+${String.format("%.0f", saldo)} €"
                                  else "${String.format("%.0f", saldo)} €"
                    val saldoColor = if (saldo >= 0) 0xFF00C896.toInt() else 0xFFFF6B6B.toInt()

                    val nextDueStr = if (nextDueDate == Long.MAX_VALUE) "Nessuna"
                                    else "${dateFmt.format(Date(nextDueDate))} — $nextDueName"

                    // Aggiorna widget sul main thread (RemoteViews è thread-safe)
                    views.setTextViewText(R.id.widget_saldo, saldoStr)
                    views.setTextColor(R.id.widget_saldo, saldoColor)
                    views.setTextViewText(R.id.widget_open_count, "$openCedolini")
                    views.setTextViewText(R.id.widget_next_due, nextDueStr)

                } catch (e: Exception) {
                    views.setTextViewText(R.id.widget_saldo, "Errore")
                    views.setTextViewText(R.id.widget_open_count, "--")
                    views.setTextViewText(R.id.widget_next_due, "Riprova")
                } finally {
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            }
        }
    }
}
