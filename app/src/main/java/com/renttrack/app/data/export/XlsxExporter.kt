package com.renttrack.app.data.export

import com.renttrack.app.data.model.SCondominio
import com.renttrack.app.data.model.SExpense
import com.renttrack.app.data.model.SPayment
import java.io.ByteArrayOutputStream
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Genera un vero file .xlsx (Open XML) senza dipendenze esterne.
 *
 * Architettura: ZIP con i seguenti file XML interni:
 *   [Content_Types].xml
 *   _rels/.rels
 *   xl/workbook.xml          — libro con 2 fogli: "Riepilogo" + "Movimenti"
 *   xl/worksheets/sheet1.xml — KPI summary + header premium
 *   xl/worksheets/sheet2.xml — tabella dati completa
 *   xl/styles.xml            — stili brand RentTrack (header teal, entrate verde, uscite rosso)
 *   xl/sharedStrings.xml     — stringhe condivise
 *   xl/_rels/workbook.xml.rels
 *
 * Feature:
 *   - Header premium con logo testo, immobile, periodo, data
 *   - Foglio 1 "Riepilogo": KPI cards (entrate, uscite, saldo, n° mov.)
 *   - Foglio 2 "Movimenti": tabella con freeze header, auto-filter, colori brand
 *   - Righe ENTRATA → sfondo verde chiaro; USCITA → sfondo rosso chiaro
 *   - Riga totali in fondo con bold
 *   - Larghezze colonna ottimizzate
 */
object XlsxExporter {

    private val DATE_FMT = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN)
    private val NUM_FMT  = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.ITALIAN))
    /** OOXML richiede numeri con '.' come decimale — lo stile cella gestisce la visualizzazione */
    private fun rawNum(d: Double): String = String.format(Locale.US, "%.2f", d)

    fun buildXlsx(
        payments: List<SPayment>,
        expenses: List<SExpense>,
        condoMap: Map<String, SCondominio>,
        scopeLabel: String,
        filterYear: Int?
    ): ByteArray {
        val periodoLabel = filterYear?.let { "Anno $it" } ?: buildRangeLabel(payments, expenses)
        val totalIn  = payments.sumOf { it.amount }
        val totalOut = expenses.sumOf { it.amount }
        val balance  = totalIn - totalOut

        // Raccoglie tutte le righe dati ordinate per data desc
        val rows = buildRows(payments, expenses, condoMap)

        // SharedStrings: raccoglie tutte le stringhe per SST
        val sst = SharedStringTable()

        val sheet1Xml = buildSummarySheet(sst, scopeLabel, periodoLabel, totalIn, totalOut, balance, rows.size)
        val sheet2Xml = buildDataSheet(sst, rows, totalIn, totalOut, balance)
        val sstXml    = sst.buildXml()

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            zip.entry("[Content_Types].xml", contentTypesXml())
            zip.entry("_rels/.rels", rootRelsXml())
            zip.entry("xl/workbook.xml", workbookXml())
            zip.entry("xl/_rels/workbook.xml.rels", workbookRelsXml())
            zip.entry("xl/styles.xml", stylesXml())
            zip.entry("xl/sharedStrings.xml", sstXml)
            zip.entry("xl/worksheets/sheet1.xml", sheet1Xml)
            zip.entry("xl/worksheets/sheet2.xml", sheet2Xml)
        }
        return baos.toByteArray()
    }

    // ── Sheet 1: Riepilogo KPI ───────────────────────────────────────────

    private fun buildSummarySheet(
        sst: SharedStringTable,
        scopeLabel: String,
        periodoLabel: String,
        totalIn: Double,
        totalOut: Double,
        balance: Double,
        movCount: Int
    ): String {
        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.appendLine("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        // <cols> DEVE essere prima di <sheetData> — se no il file risulta corrotto
        sb.appendLine("""<cols><col min="1" max="1" width="32" customWidth="1"/><col min="2" max="2" width="22" customWidth="1"/></cols>""")
        sb.appendLine("""<sheetData>""")

        fun strRow(r: Int, label: String, value: String, styleLabel: Int = 1, styleVal: Int = 1) {
            sb.appendLine("""<row r="$r"><c r="A$r" t="s" s="$styleLabel"><v>${sst.id(label)}</v></c><c r="B$r" t="s" s="$styleVal"><v>${sst.id(value)}</v></c></row>""")
        }
        // numRow usa rawNum(): <v> deve contenere un float US, lo stile si occupa del formato visivo
        fun numRow(r: Int, label: String, value: Double, styleLabel: Int = 1, styleVal: Int = 5) {
            sb.appendLine("""<row r="$r"><c r="A$r" t="s" s="$styleLabel"><v>${sst.id(label)}</v></c><c r="B$r" s="$styleVal"><v>${rawNum(value)}</v></c></row>""")
        }
        fun emptyRow(r: Int) { sb.appendLine("""<row r="$r"/>""") }

        strRow(1,  "RENTTRACK — REPORT ECONOMICO IMMOBILIARE", "", 3, 1)
        emptyRow(2)
        strRow(3,  "Immobile",    scopeLabel,           2, 1)
        strRow(4,  "Periodo",     periodoLabel,          2, 1)
        strRow(5,  "Generato il", DATE_FMT.format(Date()), 2, 1)
        strRow(6,  "Software",    "RentTrack — Gestione Immobiliare", 2, 1)
        emptyRow(7)
        strRow(8,  "RIEPILOGO FINANZIARIO", "", 3, 1)
        emptyRow(9)
        numRow(10, "Totale Entrate (Affitti incassati)", totalIn,  2, 6)
        numRow(11, "Totale Uscite (Spese)",              totalOut, 2, 7)
        emptyRow(12)
        numRow(13, if (balance >= 0) "SALDO NETTO  ▲ POSITIVO" else "SALDO NETTO  ▼ NEGATIVO",
                   balance, 3, if (balance >= 0) 6 else 7)
        emptyRow(14)
        strRow(15, "N° movimenti totali", "$movCount",  2, 1)
        strRow(16, "di cui Entrate",      "${movCount}", 2, 1)

        sb.appendLine("""</sheetData></worksheet>""")
        return sb.toString()
    }

    // ── Sheet 2: Movimenti ───────────────────────────────────────────────

    private fun buildDataSheet(
        sst: SharedStringTable,
        rows: List<DataRow>,
        totalIn: Double,
        totalOut: Double,
        balance: Double
    ): String {
        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.appendLine("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        // Freeze prima riga
        sb.appendLine("""<sheetViews><sheetView workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>""")
        // Larghezze
        sb.appendLine("""<cols>
            <col min="1" max="1" width="13" customWidth="1"/>
            <col min="2" max="2" width="11" customWidth="1"/>
            <col min="3" max="3" width="38" customWidth="1"/>
            <col min="4" max="4" width="18" customWidth="1"/>
            <col min="5" max="5" width="16" customWidth="1"/>
            <col min="6" max="6" width="30" customWidth="1"/>
            <col min="7" max="7" width="24" customWidth="1"/>
        </cols>""")
        sb.appendLine("""<sheetData>""")

        val headers = listOf("Data", "Tipo", "Categoria / Descrizione", "Metodo / Rif.", "Importo (€)", "Immobile", "Note")

        // Riga 1 — header bold teal
        sb.append("""<row r="1">""")
        headers.forEachIndexed { i, h ->
            val col = ('A' + i)
            sb.append("""<c r="$col${1}" t="s" s="4"><v>${sst.id(h)}</v></c>""")
        }
        sb.appendLine("""</row>""")

        // Righe dati
        rows.forEachIndexed { idx, row ->
            val r = idx + 2
            val isEntry = row.tipo == "ENTRATA"
            val rowStyle = if (isEntry) 6 else 7   // verde / rosso
            val numStyle = if (isEntry) 8 else 9   // numero verde / rosso

            sb.append("""<row r="$r">""")
            sb.append("""<c r="A$r" t="s" s="$rowStyle"><v>${sst.id(row.data)}</v></c>""")
            sb.append("""<c r="B$r" t="s" s="$rowStyle"><v>${sst.id(row.tipo)}</v></c>""")
            sb.append("""<c r="C$r" t="s" s="$rowStyle"><v>${sst.id(row.descrizione)}</v></c>""")
            sb.append("""<c r="D$r" t="s" s="$rowStyle"><v>${sst.id(row.riferimento)}</v></c>""")
            sb.append("""<c r="E$r" s="$numStyle"><v>${rawNum(row.importo)}</v></c>""")
            sb.append("""<c r="F$r" t="s" s="$rowStyle"><v>${sst.id(row.immobile)}</v></c>""")
            sb.append("""<c r="G$r" t="s" s="$rowStyle"><v>${sst.id(row.note)}</v></c>""")
            sb.appendLine("""</row>""")
        }

        // Riga totali bold
        val totalR = rows.size + 2
        sb.append("""<row r="$totalR">""")
        sb.append("""<c r="A$totalR" t="s" s="3"><v>${sst.id("TOTALI")}</v></c>""")
        sb.append("""<c r="B$totalR" s="3"/>""")
        sb.append("""<c r="C$totalR" t="s" s="3"><v>${sst.id("Entrate: ${NUM_FMT.format(totalIn)} €  |  Uscite: ${NUM_FMT.format(totalOut)} €")}</v></c>""")
        sb.append("""<c r="D$totalR" s="3"/>""")
        sb.append("""<c r="E$totalR" s="5"><v>${rawNum(balance)}</v></c>""")
        sb.append("""<c r="F$totalR" s="3"/>""")
        sb.append("""<c r="G$totalR" t="s" s="3"><v>${sst.id("Saldo Netto")}</v></c>""")
        sb.appendLine("""</row>""")

        sb.appendLine("""</sheetData>""")
        // Auto-filter sull'header
        sb.appendLine("""<autoFilter ref="A1:G1"/>""")
        sb.appendLine("""</worksheet>""")
        return sb.toString()
    }

    // ── Dati ─────────────────────────────────────────────────────────────

    private data class DataRow(
        val ts: Long,
        val data: String,
        val tipo: String,
        val descrizione: String,
        val riferimento: String,
        val importo: Double,
        val immobile: String,
        val note: String
    )

    private fun buildRows(
        payments: List<SPayment>,
        expenses: List<SExpense>,
        condoMap: Map<String, SCondominio>
    ): List<DataRow> {
        val rows = mutableListOf<DataRow>()
        payments.forEach { p ->
            rows += DataRow(
                ts          = p.date,
                data        = DATE_FMT.format(Date(p.date)),
                tipo        = "ENTRATA",
                descrizione = "Affitto${if (p.reference.isNotBlank()) " — ${p.reference}" else ""}",
                riferimento = p.method.ifBlank { "—" },
                importo     = p.amount,
                immobile    = condoMap[p.condominioId]?.toLabel() ?: "",
                note        = p.notes.ifBlank { "—" }
            )
        }
        expenses.forEach { e ->
            rows += DataRow(
                ts          = e.date,
                data        = DATE_FMT.format(Date(e.date)),
                tipo        = "USCITA",
                descrizione = "${e.category}${if (e.description.isNotBlank()) " — ${e.description}" else ""}",
                riferimento = "—",
                importo     = e.amount,
                immobile    = condoMap[e.condominioId]?.toLabel() ?: "",
                note        = e.notes.ifBlank { "—" }
            )
        }
        rows.sortByDescending { it.ts }
        return rows
    }

    private fun buildRangeLabel(payments: List<SPayment>, expenses: List<SExpense>): String {
        val all = payments.map { it.date } + expenses.map { it.date }
        if (all.isEmpty()) return "Tutti i movimenti"
        val cal = Calendar.getInstance()
        cal.timeInMillis = all.min()
        val from = "${cal.get(Calendar.MONTH)+1}/${cal.get(Calendar.YEAR)}"
        cal.timeInMillis = all.max()
        val to   = "${cal.get(Calendar.MONTH)+1}/${cal.get(Calendar.YEAR)}"
        return if (from == to) from else "$from – $to"
    }

    // ── Open XML files ────────────────────────────────────────────────────

    private fun contentTypesXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml"  ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml"           ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml"  ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml"  ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml"             ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
  <Override PartName="/xl/sharedStrings.xml"      ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
</Types>"""

    private fun rootRelsXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun workbookXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Riepilogo" sheetId="1" r:id="rId1"/>
    <sheet name="Movimenti" sheetId="2" r:id="rId2"/>
  </sheets>
</workbook>"""

    private fun workbookRelsXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
  <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles"        Target="styles.xml"/>
</Relationships>"""

    /**
     * Stili brand RentTrack:
     *  0 = default
     *  1 = normale
     *  2 = label grigio muted
     *  3 = bold (titoli/totali)
     *  4 = header teal bold (riga intestazione tabella)
     *  5 = numero bold
     *  6 = verde (entrate / saldo positivo)
     *  7 = rosso (uscite / saldo negativo)
     *  8 = numero verde
     *  9 = numero rosso
     */
    private fun stylesXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="5">
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><sz val="11"/><b/><name val="Calibri"/></font>
    <font><sz val="12"/><b/><color rgb="FF1A9A8A"/><name val="Calibri"/></font>
    <font><sz val="11"/><b/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
  </fonts>
  <fills count="6">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF0D2137"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF1A9A8A"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFE8F8F5"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFFFF0F0"/></patternFill></fill>
  </fills>
  <borders count="2">
    <border><left/><right/><top/><bottom/></border>
    <border><left style="thin"><color rgb="FFD1D5DB"/></left><right style="thin"><color rgb="FFD1D5DB"/></right><top style="thin"><color rgb="FFD1D5DB"/></top><bottom style="thin"><color rgb="FFD1D5DB"/></bottom></border>
  </borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs count="10">
    <xf numFmtId="0"  fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0"  fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"/>
    <xf numFmtId="0"  fontId="1" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0"  fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1"/>
    <xf numFmtId="0"  fontId="4" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/>
    <xf numFmtId="4"  fontId="2" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1" applyNumberFormat="1"/>
    <xf numFmtId="0"  fontId="0" fillId="4" borderId="1" xfId="0" applyFill="1" applyBorder="1"/>
    <xf numFmtId="0"  fontId="0" fillId="5" borderId="1" xfId="0" applyFill="1" applyBorder="1"/>
    <xf numFmtId="4"  fontId="0" fillId="4" borderId="1" xfId="0" applyFill="1" applyBorder="1" applyNumberFormat="1"/>
    <xf numFmtId="4"  fontId="0" fillId="5" borderId="1" xfId="0" applyFill="1" applyBorder="1" applyNumberFormat="1"/>
  </cellXfs>
</styleSheet>"""

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun ZipOutputStream.entry(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun SCondominio.toLabel(): String = buildString {
        append(nome)
        if (indirizzo.isNotBlank()) append(" — $indirizzo")
        if (citta.isNotBlank())     append(", $citta")
    }
}

/** Tabella stringhe condivise (SST) per Open XML */
private class SharedStringTable {
    private val map = mutableMapOf<String, Int>()
    private val list = mutableListOf<String>()

    fun id(s: String): Int = map.getOrPut(s) {
        list += s
        list.size - 1
    }

    fun buildXml(): String {
        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.appendLine("""<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${list.size}" uniqueCount="${list.size}">""")
        list.forEach { s ->
            sb.appendLine("""<si><t xml:space="preserve">${s.xmlEscape()}</t></si>""")
        }
        sb.appendLine("</sst>")
        return sb.toString()
    }

    private fun String.xmlEscape() = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}
