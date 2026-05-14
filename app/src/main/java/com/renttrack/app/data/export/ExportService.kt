package com.renttrack.app.data.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Gestisce I/O dei file export e lancio del share sheet Android.
 *
 * Flusso standard:
 *   1. CsvExporter.buildCsvContent(...)   → String
 *   2. ExportService.buildFileName(...)   → String
 *   3. ExportService.writeToCache(...)    → File  (Dispatchers.IO)
 *   4. ExportService.shareFile(...)       → Unit  (main thread)
 *
 * Predisposto per futura aggiunta di esporter alternativi:
 *   // object PdfExporter   { fun buildPdf(...):  ByteArray = TODO() }
 *   // object ExcelExporter { fun buildXlsx(...): ByteArray = TODO() }
 */
object ExportService {

    /**
     * Costruisce un nome file leggibile e sicuro per il filesystem.
     * Esempio: "renttrack_bilocale_via_latina_12_roma_2026.csv"
     *
     * @param condoName    Nome del condominio/appartamento
     * @param condoAddress Indirizzo
     * @param condoCitta   Città
     * @param filterYear   Anno di filtro (null = omesso dal nome)
     */
    fun buildFileName(
        condoName: String,
        condoAddress: String,
        condoCitta: String,
        filterYear: Int?
    ): String {
        val parts = mutableListOf("renttrack")
        if (condoName.isNotBlank())    parts += toSlug(condoName)
        if (condoAddress.isNotBlank()) parts += toSlug(condoAddress)
        if (condoCitta.isNotBlank())   parts += toSlug(condoCitta)
        filterYear?.let { parts += it.toString() }
        return "${parts.joinToString("_")}.csv"
    }

    /**
     * Scrive il contenuto in cache.
     * Da chiamare su Dispatchers.IO.
     *
     * @param context  Contesto Android per cacheDir
     * @param content  Stringa CSV (già con BOM)
     * @param fileName Nome file (da buildFileName)
     * @return Il File scritto, pronto per shareFile()
     */
    fun writeToCache(context: Context, content: String, fileName: String): File {
        val file = File(context.cacheDir, fileName)
        file.writeText(content, Charsets.UTF_8)  // BOM già incluso nel content
        return file
    }

    /**
     * Apre il share sheet Android per il file CSV.
     * Da chiamare sul main thread (viewModelScope usa Dispatchers.Main).
     *
     * @return Il nome del file esportato (per il messaggio di conferma)
     */
    fun shareFile(context: Context, file: File): String {
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.provider", file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "Esporta CSV")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return file.name
    }

    // ── Helpers ──────────────────────────────────────────────────

    /**
     * Converte testo in slug sicuro per nomi file.
     * "Via Latina, 12" → "via_latina_12"
     * Max 30 caratteri per evitare nomi eccessivamente lunghi.
     */
    private fun toSlug(text: String): String = text
        .lowercase()
        .replace(Regex("[àáâã]"), "a")
        .replace(Regex("[èéêë]"), "e")
        .replace(Regex("[ìíîï]"), "i")
        .replace(Regex("[òóôõ]"), "o")
        .replace(Regex("[ùúûü]"), "u")
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
        .take(30)
}
