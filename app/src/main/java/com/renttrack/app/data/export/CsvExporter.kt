package com.renttrack.app.data.export

import com.renttrack.app.data.model.SCondominio
import com.renttrack.app.data.model.SExpense
import com.renttrack.app.data.model.SPayment
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Genera un report CSV professionale stile software gestionale immobiliare.
 *
 * Struttura output:
 *   [1] HEADER PREMIUM  — logo RentTrack, immobile, periodo, data generazione
 *   [2] KPI SUMMARY     — Totale entrate, uscite, saldo netto, n° movimenti
 *   [3] SEPARATORE      — Linea visiva
 *   [4] TABELLA DATI    — Colonne allineate, ordinamento cronologico desc
 *   [5] FOOTER          — Riga totali bold
 *
 * Specifiche tecniche:
 *  - BOM UTF-8 (\uFEFF) → apertura corretta in Excel su Windows
 *  - Separatore ";" (standard europeo)
 *  - Importi: "1.200,50 €" (Locale.ITALIAN)
 *  - Date: "14/05/2026"
 *  - Auto-escaping RFC 4180
 */
object CsvExporter {

    private const val BOM = "\uFEFF"
    private const val SEP = ";"

    private val DATE_FMT  = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN)
    private val MONTH_FMT = SimpleDateFormat("MMMM yyyy", Locale.ITALIAN)
    private val NUM_FMT   = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.ITALIAN))

    /** Colonne principali — stile accounting professionale */
    private val HEADERS = listOf(
        "Data", "Tipo", "Categoria / Descrizione", "Riferimento", "Importo (€)", "Immobile", "Note"
    )

    // ─── Entry point principale ──────────────────────────────────────────

    /**
     * Costruisce il contenuto CSV completo con header premium e KPI summary.
     *
     * @param payments   Pagamenti (entrate affitto)
     * @param expenses   Spese (uscite)
     * @param condoMap   id → SCondominio per risolvere etichette
     * @param scopeLabel Etichetta immobile/scope ("Via Appia 333, Roma")
     * @param filterYear Anno filtro (null = tutti gli anni)
     */
    fun buildCsvContent(
        payments: List<SPayment>,
        expenses: List<SExpense>,
        condoMap: Map<String, SCondominio>,
        scopeLabel: String,
        filterYear: Int?
    ): String {
        val sb = StringBuilder()
        sb.append(BOM)

        val totalIn  = payments.sumOf { it.amount }
        val totalOut = expenses.sumOf { it.amount }
        val balance  = totalIn - totalOut
        val rowCount = payments.size + expenses.size

        val periodoLabel = when {
            filterYear != null -> "Anno $filterYear"
            else -> buildDateRangeLabel(payments, expenses)
        }

        // ── [1] HEADER PREMIUM ───────────────────────────────────────
        sb.appendLine("${SEP}${SEP}${SEP}${SEP}${SEP}${SEP}")
        sb.appendLine("${SEP}  ██████╗ ███████╗███╗   ██╗████████╗${SEP}${SEP}${SEP}${SEP}${SEP}")
        sb.appendLine("${SEP}  ██╔══██╗██╔════╝████╗  ██║╚══██╔══╝${SEP}${SEP}${SEP}${SEP}${SEP}")
        sb.appendLine("${SEP}  ██████╔╝█████╗  ██╔██╗ ██║   ██║   ${SEP}${SEP}${SEP}${SEP}${SEP}")
        sb.appendLine("${SEP}  ██╔══██╗██╔══╝  ██║╚██╗██║   ██║   ${SEP}${SEP}${SEP}${SEP}${SEP}")
        sb.appendLine("${SEP}  ██║  ██║███████╗██║ ╚████║   ██║   ${SEP}${SEP}${SEP}${SEP}${SEP}")
        sb.appendLine("${SEP}  ╚═╝  ╚═╝╚══════╝╚═╝  ╚═══╝   ╚═╝   ${SEP}${SEP}${SEP}${SEP}${SEP}")
        sb.appendLine("${SEP}${SEP}${SEP}${SEP}${SEP}${SEP}")
        sb.appendLine("${SEP}  REPORT ECONOMICO IMMOBILIARE${SEP}${SEP}${SEP}${SEP}${SEP}")
        sb.appendLine("${SEP}${SEP}${SEP}${SEP}${SEP}${SEP}")
        sb.appendLine("${SEP}  Immobile${SEP}${SEP}${escape(scopeLabel)}${SEP}${SEP}${SEP}")
        sb.appendLine("${SEP}  Periodo${SEP}${SEP}${escape(periodoLabel)}${SEP}${SEP}${SEP}")
        sb.appendLine("${SEP}  Generato il${SEP}${SEP}${DATE_FMT.format(Date())}${SEP}${SEP}${SEP}")
        sb.appendLine("${SEP}  Software${SEP}${SEP}RentTrack — Gestione Immobiliare${SEP}${SEP}${SEP}")
        sb.appendLine("${SEP}${SEP}${SEP}${SEP}${SEP}${SEP}")
        sb.appendLine(divider())

        // ── [2] KPI SUMMARY ──────────────────────────────────────────
        sb.appendLine("${SEP}  RIEPILOGO FINANZIARIO${SEP}${SEP}${SEP}${SEP}${SEP}")
        sb.appendLine("${SEP}${SEP}${SEP}${SEP}${SEP}${SEP}")
        sb.appendLine("${SEP}  Totale Entrate (Affitti incassati)${SEP}${SEP}${formatAmt(totalIn)}${SEP}${SEP}${SEP}")
        sb.appendLine("${SEP}  Totale Uscite (Spese deducibili)${SEP}${SEP}${formatAmt(totalOut)}${SEP}${SEP}${SEP}")
        sb.appendLine("${SEP}${SEP}${SEP}${SEP}${SEP}${SEP}")
        val balanceLabel = if (balance >= 0) "SALDO NETTO  ▲ POSITIVO" else "SALDO NETTO  ▼ NEGATIVO"
        sb.appendLine("${SEP}  $balanceLabel${SEP}${SEP}${formatAmt(balance)}${SEP}${SEP}${SEP}")
        sb.appendLine("${SEP}  Numero movimenti totali${SEP}${SEP}$rowCount${SEP}${SEP}${SEP}")
        sb.appendLine("${SEP}  di cui Entrate${SEP}${SEP}${payments.size}${SEP}${SEP}${SEP}")
        sb.appendLine("${SEP}  di cui Uscite${SEP}${SEP}${expenses.size}${SEP}${SEP}${SEP}")
        sb.appendLine("${SEP}${SEP}${SEP}${SEP}${SEP}${SEP}")
        sb.appendLine(divider())

        // ── [3] TABELLA DATI ─────────────────────────────────────────
        sb.appendLine("${SEP}  DETTAGLIO MOVIMENTI${SEP}${SEP}${SEP}${SEP}${SEP}")
        sb.appendLine("${SEP}${SEP}${SEP}${SEP}${SEP}${SEP}")
        sb.appendLine(csvRow(HEADERS))

        val rows = mutableListOf<Pair<Long, String>>()

        payments.forEach { p ->
            val condoLabel = condoMap[p.condominioId]?.toLabel() ?: ""
            val desc = buildString {
                append("Affitto")
                if (p.reference.isNotBlank()) append(" — ${p.reference}")
            }
            rows += p.date to csvRow(listOf(
                DATE_FMT.format(Date(p.date)),
                "ENTRATA",
                desc,
                p.method.ifBlank { "—" },
                formatAmt(p.amount),
                condoLabel,
                p.notes.ifBlank { "—" }
            ))
        }

        expenses.forEach { e ->
            val condoLabel = condoMap[e.condominioId]?.toLabel() ?: ""
            val desc = buildString {
                append(e.category)
                if (e.description.isNotBlank()) append(" — ${e.description}")
            }
            rows += e.date to csvRow(listOf(
                DATE_FMT.format(Date(e.date)),
                "USCITA",
                desc,
                "—",
                formatAmt(e.amount),
                condoLabel,
                e.notes.ifBlank { "—" }
            ))
        }

        rows.sortByDescending { it.first }
        rows.forEach { sb.appendLine(it.second) }

        // ── [4] FOOTER TOTALI ────────────────────────────────────────
        sb.appendLine(divider())
        sb.appendLine(csvRow(listOf(
            "TOTALI", "", "", "",
            formatAmt(totalIn + totalOut),
            "",
            ""
        )))
        sb.appendLine("${SEP}  Entrate${SEP}${SEP}${SEP}${formatAmt(totalIn)}${SEP}${SEP}")
        sb.appendLine("${SEP}  Uscite${SEP}${SEP}${SEP}${formatAmt(totalOut)}${SEP}${SEP}")
        sb.appendLine("${SEP}  Saldo Netto${SEP}${SEP}${SEP}${formatAmt(balance)}${SEP}${SEP}")
        sb.appendLine(divider())
        sb.appendLine()
        sb.appendLine("${SEP}  Report generato da RentTrack — Software Gestione Immobiliare${SEP}${SEP}${SEP}${SEP}${SEP}")

        return sb.toString()
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun csvRow(fields: List<String>): String =
        fields.joinToString(SEP) { escape(it) }

    /** RFC 4180: virgolette se il campo contiene SEP, " o newline. */
    private fun escape(field: String): String {
        val needsQuote = field.contains(SEP) || field.contains('"') || field.contains('\n')
        return if (needsQuote) "\"${field.replace("\"", "\"\"")}\"" else field
    }

    /** Importo stile "1.200,50 €" */
    private fun formatAmt(amount: Double): String = "${NUM_FMT.format(amount)} €"

    /** Linea separatrice visiva (adattata al numero di colonne) */
    private fun divider(): String = "${SEP}${SEP}${SEP}${SEP}${SEP}${SEP}"

    /**
     * Determina il range date dai dati (per il label periodo quando filterYear == null).
     * Restituisce "01/2026 – 12/2026" oppure "Tutti i movimenti".
     */
    private fun buildDateRangeLabel(payments: List<SPayment>, expenses: List<SExpense>): String {
        val allDates = payments.map { it.date } + expenses.map { it.date }
        if (allDates.isEmpty()) return "Tutti i movimenti"
        val minTs = allDates.min()
        val maxTs = allDates.max()
        val cal = Calendar.getInstance()
        cal.timeInMillis = minTs
        val fromLabel = "${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.YEAR)}"
        cal.timeInMillis = maxTs
        val toLabel = "${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.YEAR)}"
        return if (fromLabel == toLabel) fromLabel else "$fromLabel – $toLabel"
    }
}

/** Etichetta leggibile di un condominio: "Nome — Via, Città" */
private fun SCondominio.toLabel(): String = buildString {
    append(nome)
    if (indirizzo.isNotBlank()) append(" — $indirizzo")
    if (citta.isNotBlank())     append(", $citta")
}
