package com.renttrack.app.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.renttrack.app.data.model.SCedolino
import com.renttrack.app.data.model.SCedolinoItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Genera un PDF del cedolino usando PdfDocument nativo Android.
 * Nessuna dipendenza esterna richiesta.
 *
 * Layout:
 *  - Header: RentTrack logo + intestazione
 *  - Box dati: inquilino, periodo, scadenza
 *  - Tabella voci con importi
 *  - Totale in evidenza
 *  - Footer: data generazione
 */
object CedolinoPdfGenerator {

    private val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN)

    // Colori
    private val colorDark    = Color.parseColor("#0F1923")
    private val colorCyan    = Color.parseColor("#00BCD4")
    private val colorGreen   = Color.parseColor("#00C896")
    private val colorMuted   = Color.parseColor("#607080")
    private val colorText    = Color.parseColor("#E8EDF2")
    private val colorBorder  = Color.parseColor("#1E2D3D")
    private val colorWhite   = Color.WHITE

    fun generateAndShare(
        context: Context,
        cedolino: SCedolino,
        items: List<SCedolinoItem>,
        tenantName: String,
        propertyName: String
    ): Uri {
        val pageWidth  = 595   // A4 @ 72dpi
        val pageHeight = 842

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page     = document.startPage(pageInfo)
        val canvas   = page.canvas

        var y = 0f

        // ── Sfondo header ────────────────────────────────────────────────
        val headerPaint = Paint().apply { color = colorDark; style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 110f, headerPaint)

        // ── Logo / Nome app ───────────────────────────────────────────────
        val appNamePaint = Paint().apply {
            color     = colorCyan
            textSize  = 26f
            isFakeBoldText = true
            isAntiAlias    = true
        }
        canvas.drawText("RentTrack", 40f, 48f, appNamePaint)

        val subtitlePaint = Paint().apply {
            color    = colorMuted
            textSize = 11f
            isAntiAlias = true
        }
        canvas.drawText("Gestione affitti professionale", 40f, 65f, subtitlePaint)

        // ── Titolo documento ───────────────────────────────────────────────
        val titlePaint = Paint().apply {
            color    = colorText
            textSize = 15f
            isFakeBoldText = true
            isAntiAlias    = true
        }
        canvas.drawText("AVVISO DI PAGAMENTO", 40f, 92f, titlePaint)

        y = 130f

        // ── Card dati principali ───────────────────────────────────────────
        drawCard(canvas, 30f, y, pageWidth - 60f, 120f, colorBorder)

        val labelPaint = Paint().apply { color = colorMuted; textSize = 9f; isAntiAlias = true }
        val valuePaint = Paint().apply { color = colorDark;  textSize = 13f; isFakeBoldText = true; isAntiAlias = true }

        var cx = 50f; var cy = y + 22f
        canvas.drawText("INQUILINO", cx, cy, labelPaint)
        canvas.drawText(tenantName, cx, cy + 16f, valuePaint)

        canvas.drawText("PROPRIETA'", cx, cy + 44f, labelPaint)
        canvas.drawText(propertyName, cx, cy + 60f, valuePaint)

        cx = 300f
        canvas.drawText("PERIODO", cx, cy, labelPaint)
        canvas.drawText(cedolino.period, cx, cy + 16f, valuePaint)

        canvas.drawText("SCADENZA", cx, cy + 44f, labelPaint)
        val dueDateStr = dateFmt.format(Date(cedolino.dueDate))
        canvas.drawText(dueDateStr, cx, cy + 60f, valuePaint.apply {
            color = if (cedolino.dueDate < System.currentTimeMillis()) Color.RED else colorGreen
        })
        valuePaint.color = colorDark  // reset

        canvas.drawText("DATA EMISSIONE", cx + 130f, cy, labelPaint)
        canvas.drawText(dateFmt.format(Date(cedolino.issueDate)), cx + 130f, cy + 16f, valuePaint)

        y += 140f

        // ── Intestazione tabella ──────────────────────────────────────────
        val tableHeaderBg = Paint().apply { color = colorCyan; style = Paint.Style.FILL }
        canvas.drawRect(30f, y, pageWidth - 30f, y + 28f, tableHeaderBg)

        val thPaint = Paint().apply { color = colorWhite; textSize = 10f; isFakeBoldText = true; isAntiAlias = true }
        canvas.drawText("DESCRIZIONE", 44f, y + 18f, thPaint)
        canvas.drawText("IMPORTO", pageWidth - 120f, y + 18f, thPaint)

        y += 28f

        // ── Righe tabella ─────────────────────────────────────────────────
        val rowBg1 = Paint().apply { color = Color.parseColor("#F8FAFB"); style = Paint.Style.FILL }
        val rowBg2 = Paint().apply { color = Color.parseColor("#FFFFFF"); style = Paint.Style.FILL }
        val rowText = Paint().apply { color = colorDark; textSize = 11f; isAntiAlias = true }
        val rowAmount = Paint().apply { color = colorDark; textSize = 11f; isFakeBoldText = true; isAntiAlias = true }

        items.forEachIndexed { idx, item ->
            val rowY = y + idx * 32f
            canvas.drawRect(30f, rowY, pageWidth - 30f, rowY + 32f, if (idx % 2 == 0) rowBg1 else rowBg2)
            canvas.drawText(item.description, 44f, rowY + 20f, rowText)
            val amtStr = "€ ${String.format("%.2f", item.amount)}"
            canvas.drawText(amtStr, pageWidth - 120f, rowY + 20f, rowAmount)
        }

        y += items.size * 32f

        // Linea separatore
        val linePaint = Paint().apply { color = colorCyan; strokeWidth = 2f }
        canvas.drawLine(30f, y + 4f, pageWidth - 30f, y + 4f, linePaint)
        y += 16f

        // ── Totale ────────────────────────────────────────────────────────
        val totalBg = Paint().apply { color = Color.parseColor("#E8F9F5"); style = Paint.Style.FILL }
        canvas.drawRect(30f, y, pageWidth - 30f, y + 44f, totalBg)

        val totalLabel = Paint().apply { color = colorMuted; textSize = 11f; isFakeBoldText = true; isAntiAlias = true }
        val totalValue = Paint().apply { color = colorGreen; textSize = 20f; isFakeBoldText = true; isAntiAlias = true }

        canvas.drawText("TOTALE DA VERSARE", 44f, y + 27f, totalLabel)
        canvas.drawText("€ ${String.format("%.2f", cedolino.total)}", pageWidth - 160f, y + 30f, totalValue)

        y += 60f

        // Saldo parziale (se ha già pagato qualcosa)
        if (cedolino.paidAmount > 0) {
            val paidPaint = Paint().apply { color = colorMuted; textSize = 10f; isAntiAlias = true }
            canvas.drawText("Già versato: € ${String.format("%.2f", cedolino.paidAmount)}", 44f, y, paidPaint)
            val remaining = cedolino.total - cedolino.paidAmount
            val remPaint  = Paint().apply { color = Color.RED; textSize = 11f; isFakeBoldText = true; isAntiAlias = true }
            canvas.drawText("Residuo: € ${String.format("%.2f", remaining)}", 44f, y + 16f, remPaint)
            y += 36f
        }

        y += 20f

        // ── Note informative ──────────────────────────────────────────────
        val notePaint = Paint().apply { color = colorMuted; textSize = 9f; isAntiAlias = true }
        canvas.drawText("Documento generato automaticamente da RentTrack.", 44f, y, notePaint)
        canvas.drawText("Per chiarimenti contatta il proprietario dell'immobile.", 44f, y + 14f, notePaint)

        // ── Footer ────────────────────────────────────────────────────────
        val footerPaint = Paint().apply { color = colorDark; style = Paint.Style.FILL }
        canvas.drawRect(0f, (pageHeight - 36).toFloat(), pageWidth.toFloat(), pageHeight.toFloat(), footerPaint)
        val footerText = Paint().apply { color = colorMuted; textSize = 9f; isAntiAlias = true }
        val genDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALIAN).format(Date())
        canvas.drawText("RentTrack  •  Generato il $genDate", 40f, (pageHeight - 14).toFloat(), footerText)

        document.finishPage(page)

        // ── Salva il file ─────────────────────────────────────────────────
        val fileName = "cedolino_${cedolino.period.replace(" ", "_")}_${tenantName.replace(" ", "_")}.pdf"
        val file = File(context.cacheDir, fileName)
        document.writeTo(file.outputStream())
        document.close()

        return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }

    /**
     * Genera una RICEVUTA DI PAGAMENTO (PDF) da condividere dopo aver registrato un pagamento.
     * Layout identico al cedolino ma con header verde e titolo "RICEVUTA DI PAGAMENTO".
     */
    fun generateReceiptAndShare(
        context: Context,
        cedolino: SCedolino,
        items: List<SCedolinoItem>,
        tenantName: String,
        propertyName: String,
        paymentMethod: String,
        reference: String,
        paidAmount: Double
    ): Uri {
        val pageWidth  = 595
        val pageHeight = 842
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page     = document.startPage(pageInfo)
        val canvas   = page.canvas
        var y = 0f

        // ── Header verde (pagato) ────────────────────────────────────────
        val headerPaint = Paint().apply { color = Color.parseColor("#0A2A1A"); style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 110f, headerPaint)

        val appNamePaint = Paint().apply { color = colorGreen; textSize = 26f; isFakeBoldText = true; isAntiAlias = true }
        canvas.drawText("RentTrack", 40f, 48f, appNamePaint)
        val subtitlePaint = Paint().apply { color = colorMuted; textSize = 11f; isAntiAlias = true }
        canvas.drawText("Gestione affitti professionale", 40f, 65f, subtitlePaint)
        val titlePaint = Paint().apply { color = colorText; textSize = 15f; isFakeBoldText = true; isAntiAlias = true }
        canvas.drawText("RICEVUTA DI PAGAMENTO", 40f, 92f, titlePaint)
        y = 130f

        // ── Card dati principali ─────────────────────────────────────────
        drawCard(canvas, 30f, y, pageWidth - 60f, 140f, colorGreen)
        val labelPaint = Paint().apply { color = colorMuted; textSize = 9f; isAntiAlias = true }
        val valuePaint = Paint().apply { color = colorDark; textSize = 13f; isFakeBoldText = true; isAntiAlias = true }

        var cx = 50f; var cy = y + 22f
        canvas.drawText("INQUILINO", cx, cy, labelPaint)
        canvas.drawText(tenantName, cx, cy + 16f, valuePaint)
        canvas.drawText("PROPRIETÀ", cx, cy + 44f, labelPaint)
        canvas.drawText(propertyName, cx, cy + 60f, valuePaint)

        cx = 300f
        canvas.drawText("PERIODO", cx, cy, labelPaint)
        canvas.drawText(cedolino.period, cx, cy + 16f, valuePaint)
        canvas.drawText("DATA PAGAMENTO", cx, cy + 44f, labelPaint)
        canvas.drawText(dateFmt.format(Date()), cx, cy + 60f, valuePaint.apply { color = colorGreen })
        valuePaint.color = colorDark
        canvas.drawText("METODO", cx, cy + 84f, labelPaint)
        canvas.drawText(paymentMethod, cx, cy + 100f, valuePaint)
        y += 160f

        // ── Tabella voci ─────────────────────────────────────────────────
        val tableHeaderBg = Paint().apply { color = colorGreen; style = Paint.Style.FILL }
        canvas.drawRect(30f, y, pageWidth - 30f, y + 28f, tableHeaderBg)
        val thPaint = Paint().apply { color = colorWhite; textSize = 10f; isFakeBoldText = true; isAntiAlias = true }
        canvas.drawText("DESCRIZIONE", 44f, y + 18f, thPaint)
        canvas.drawText("IMPORTO", pageWidth - 120f, y + 18f, thPaint)
        y += 28f

        val rowBg1 = Paint().apply { color = Color.parseColor("#F0FFF4"); style = Paint.Style.FILL }
        val rowBg2 = Paint().apply { color = Color.parseColor("#FFFFFF"); style = Paint.Style.FILL }
        val rowText = Paint().apply { color = colorDark; textSize = 11f; isAntiAlias = true }
        val rowAmount = Paint().apply { color = colorDark; textSize = 11f; isFakeBoldText = true; isAntiAlias = true }
        items.forEachIndexed { idx, item ->
            val rowY = y + idx * 32f
            canvas.drawRect(30f, rowY, pageWidth - 30f, rowY + 32f, if (idx % 2 == 0) rowBg1 else rowBg2)
            canvas.drawText(item.description, 44f, rowY + 20f, rowText)
            canvas.drawText("€ ${String.format("%.2f", item.amount)}", pageWidth - 120f, rowY + 20f, rowAmount)
        }
        y += items.size * 32f

        // ── Totale pagato ────────────────────────────────────────────────
        val linePaint = Paint().apply { color = colorGreen; strokeWidth = 2f }
        canvas.drawLine(30f, y + 4f, pageWidth - 30f, y + 4f, linePaint)
        y += 16f
        val totalBg = Paint().apply { color = Color.parseColor("#E0FFF0"); style = Paint.Style.FILL }
        canvas.drawRect(30f, y, pageWidth - 30f, y + 44f, totalBg)
        val totalLabel = Paint().apply { color = colorMuted; textSize = 11f; isFakeBoldText = true; isAntiAlias = true }
        val totalValue = Paint().apply { color = colorGreen; textSize = 20f; isFakeBoldText = true; isAntiAlias = true }
        canvas.drawText("IMPORTO RICEVUTO", 44f, y + 27f, totalLabel)
        canvas.drawText("€ ${String.format("%.2f", paidAmount)}", pageWidth - 160f, y + 30f, totalValue)
        y += 60f

        if (reference.isNotBlank()) {
            val refPaint = Paint().apply { color = colorMuted; textSize = 10f; isAntiAlias = true }
            canvas.drawText("Riferimento: $reference", 44f, y, refPaint)
            y += 20f
        }
        y += 10f

        val notePaint = Paint().apply { color = colorMuted; textSize = 9f; isAntiAlias = true }
        canvas.drawText("Ricevuta generata automaticamente da RentTrack.", 44f, y, notePaint)
        canvas.drawText("Conservare come prova di pagamento.", 44f, y + 14f, notePaint)

        val footerPaint = Paint().apply { color = Color.parseColor("#0A2A1A"); style = Paint.Style.FILL }
        canvas.drawRect(0f, (pageHeight - 36).toFloat(), pageWidth.toFloat(), pageHeight.toFloat(), footerPaint)
        val footerText = Paint().apply { color = colorMuted; textSize = 9f; isAntiAlias = true }
        val genDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALIAN).format(Date())
        canvas.drawText("RentTrack  •  Generato il $genDate", 40f, (pageHeight - 14).toFloat(), footerText)

        document.finishPage(page)
        val fileName = "ricevuta_${cedolino.period.replace(" ", "_")}_${tenantName.replace(" ", "_")}.pdf"
        val file = File(context.cacheDir, fileName)
        document.writeTo(file.outputStream())
        document.close()
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }

    private fun drawCard(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, borderColor: Int) {
        val bgPaint = Paint().apply { color = Color.parseColor("#F8FAFB"); style = Paint.Style.FILL }
        canvas.drawRoundRect(x, y, x + w, y + h, 8f, 8f, bgPaint)
        val borderPaint = Paint().apply {
            color = borderColor; style = Paint.Style.STROKE; strokeWidth = 1f
        }
        canvas.drawRoundRect(x, y, x + w, y + h, 8f, 8f, borderPaint)
    }
}
