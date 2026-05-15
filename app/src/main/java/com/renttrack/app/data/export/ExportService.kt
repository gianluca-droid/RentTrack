package com.renttrack.app.data.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Gestisce I/O dei file export e lancio del share sheet Android.
 *
 * Flusso standard:
 *   CSV:  CsvExporter.buildCsvContent(...)  → String  → writeToCache() → shareFile()
 *   XLSX: XlsxExporter.buildXlsx(...)       → ByteArray → writeBytesToCache() → shareFile()
 *
 * Layer modulare — pronto per aggiunta futura PdfExporter.
 */
object ExportService {

    // ── Naming ────────────────────────────────────────────────────────────

    /**
     * Nome file CSV professionale.
     * Esempio: "renttrack_via_appia_roma_2026.csv"
     */
    fun buildFileName(
        condoName: String,
        condoAddress: String,
        condoCitta: String,
        filterYear: Int?
    ): String = buildName(condoName, condoAddress, condoCitta, filterYear, "csv")

    /**
     * Nome file XLSX professionale.
     * Esempio: "renttrack_via_appia_roma_2026.xlsx"
     */
    fun buildXlsxFileName(
        condoName: String,
        condoAddress: String,
        condoCitta: String,
        filterYear: Int?
    ): String = buildName(condoName, condoAddress, condoCitta, filterYear, "xlsx")

    private fun buildName(
        condoName: String,
        condoAddress: String,
        condoCitta: String,
        filterYear: Int?,
        ext: String
    ): String {
        val parts = mutableListOf("renttrack")
        if (condoName.isNotBlank())    parts += toSlug(condoName)
        if (condoAddress.isNotBlank()) parts += toSlug(condoAddress)
        if (condoCitta.isNotBlank())   parts += toSlug(condoCitta)
        filterYear?.let { parts += it.toString() }
        return "${parts.joinToString("_")}.$ext"
    }

    // ── I/O ───────────────────────────────────────────────────────────────

    /** Scrive stringa in cache (CSV). Da chiamare su Dispatchers.IO. */
    fun writeToCache(context: Context, content: String, fileName: String): File {
        val file = File(context.cacheDir, fileName)
        file.writeText(content, Charsets.UTF_8)
        return file
    }

    /** Scrive ByteArray in cache (XLSX, PDF). Da chiamare su Dispatchers.IO. */
    fun writeBytesToCache(context: Context, bytes: ByteArray, fileName: String): File {
        val file = File(context.cacheDir, fileName)
        file.writeBytes(bytes)
        return file
    }

    // ── Share ─────────────────────────────────────────────────────────────

    /** Apre il share sheet per un file CSV. */
    fun shareFile(context: Context, file: File): String {
        shareFileInternal(context, file, "text/csv", "Esporta Report CSV")
        return file.name
    }

    /** Apre il share sheet per un file XLSX. */
    fun shareXlsxFile(context: Context, file: File): String {
        shareFileInternal(
            context, file,
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "Esporta Report Excel"
        )
        return file.name
    }

    private fun shareFileInternal(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, chooserTitle)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────

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
