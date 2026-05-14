package com.renttrack.app.data.export

import com.renttrack.app.data.model.SCondominio
import com.renttrack.app.data.model.SExpense
import com.renttrack.app.data.model.SPayment
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Genera contenuto CSV professionale compatibile con Excel italiano.
 *
 * Specifiche tecniche:
 *  - BOM UTF-8 (\uFEFF) → apertura corretta in Excel su Windows
 *  - Separatore ";" (standard europeo)
 *  - Importi italiani: "1.200,50 €"
 *  - Date italiane: "14/05/2026"
 *  - Auto-escaping RFC 4180 (virgolette se il campo contiene ";" o a capo)
 *  - Righe ordinate per data decrescente
 *
 * Predisposto per futura estensione: PdfExporter, ExcelExporter.
 */
object CsvExporter {

    private const val BOM = "\uFEFF"
    private const val SEP = ";"

    private val DATE_FMT = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN)
    private val NUM_FMT  = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.ITALIAN))

    /** Header colonne — leggibili da commercialisti e landlord. */
    private val HEADERS = listOf(
        "Tipo", "Categoria", "Data", "Importo (€)", "Descrizione", "Stato", "Immobile", "Note"
    )

    /**
     * Costruisce il contenuto CSV completo.
     *
     * @param payments   Lista pagamenti (incassi affitto)
     * @param expenses   Lista spese (uscite)
     * @param condoMap   Mappa id → SCondominio per risolvere etichette immobile
     * @param scopeLabel Etichetta dello scope (es. "Appartamento Roma — Via Latina 12")
     * @param filterYear Anno di filtro (null = tutti gli anni)
     * @return String pronta per writeText() — il BOM è già incluso
     */
    fun buildCsvContent(
        payments: List<SPayment>,
        expenses: List<SExpense>,
        condoMap: Map<String, SCondominio>,
        scopeLabel: String,
        filterYear: Int?
    ): String {
        val sb = StringBuilder()

        // BOM per compatibilità Excel Windows
        sb.append(BOM)

        // Intestazione report
        val yearLabel = filterYear?.toString() ?: "tutti gli anni"
        sb.appendLine("# RentTrack Export — $scopeLabel — $yearLabel")
        sb.appendLine("# Generato il ${DATE_FMT.format(Date())}")
        sb.appendLine()

        // Header colonne
        sb.appendLine(csvRow(HEADERS))

        // Raccoglie tutte le righe con timestamp per l'ordinamento
        val rows = mutableListOf<Pair<Long, String>>()

        payments.forEach { p ->
            val condoLabel = condoMap[p.condominioId]?.toLabel() ?: ""
            rows += p.date to csvRow(
                listOf(
                    "Incasso",
                    "Affitto",
                    DATE_FMT.format(Date(p.date)),
                    formatAmount(p.amount),
                    p.reference.ifBlank { "Pagamento affitto" },
                    "Pagato",
                    condoLabel,
                    p.notes
                )
            )
        }

        expenses.forEach { e ->
            val condoLabel = condoMap[e.condominioId]?.toLabel() ?: ""
            rows += e.date to csvRow(
                listOf(
                    "Uscita",
                    e.category,
                    DATE_FMT.format(Date(e.date)),
                    formatAmount(e.amount),
                    e.description,
                    "-",
                    condoLabel,
                    e.notes
                )
            )
        }

        // Ordine cronologico decrescente (più recente in cima)
        rows.sortByDescending { it.first }
        rows.forEach { sb.appendLine(it.second) }

        return sb.toString()
    }

    // ── Helpers ──────────────────────────────────────────────────

    private fun csvRow(fields: List<String>): String =
        fields.joinToString(SEP) { escape(it) }

    /** RFC 4180: racchiude in virgolette se il campo contiene SEP, " o newline. */
    private fun escape(field: String): String {
        val needsQuote = field.contains(SEP) || field.contains('"') || field.contains('\n')
        return if (needsQuote) "\"${field.replace("\"", "\"\"")}\"" else field
    }

    /** Formatta come "1.200,50 €" (stile italiano). */
    private fun formatAmount(amount: Double): String = "${NUM_FMT.format(amount)} €"
}

/** Etichetta leggibile di un condominio: "Nome — Via, Città". */
private fun SCondominio.toLabel(): String = buildString {
    append(nome)
    if (indirizzo.isNotBlank()) append(" — $indirizzo")
    if (citta.isNotBlank()) append(", $citta")
}
