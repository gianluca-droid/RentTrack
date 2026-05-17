package com.renttrack.app.data.export

import com.renttrack.app.data.model.SExpense
import com.renttrack.app.data.model.SPayment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Genera XML OOXML per grafici nel file .xlsx Pro */
internal object XlsxChartExporter {

    private val KEY_FMT   = SimpleDateFormat("yyyyMM", Locale.US)
    private val LABEL_FMT = SimpleDateFormat("MMM yy", Locale.ITALIAN)

    // ── Aggregazioni dati ─────────────────────────────────────────────

    data class MonthTotal(val label: String, val entrate: Double, val uscite: Double)
    data class CatTotal(val category: String, val amount: Double)

    fun monthlyTotals(payments: List<SPayment>, expenses: List<SExpense>): List<MonthTotal> {
        val cal = Calendar.getInstance()
        val entrateMap = mutableMapOf<String, Double>()
        val usciteMap  = mutableMapOf<String, Double>()
        val labelMap   = mutableMapOf<String, String>()
        payments.forEach { p ->
            cal.timeInMillis = p.date
            val k = KEY_FMT.format(cal.time)
            entrateMap[k] = (entrateMap[k] ?: 0.0) + p.amount
            labelMap[k]   = LABEL_FMT.format(cal.time)
        }
        expenses.forEach { e ->
            cal.timeInMillis = e.date
            val k = KEY_FMT.format(cal.time)
            usciteMap[k] = (usciteMap[k] ?: 0.0) + e.amount
            labelMap[k]  = LABEL_FMT.format(cal.time)
        }
        val keys = (entrateMap.keys + usciteMap.keys).toSortedSet()
        return keys.map { k ->
            MonthTotal(labelMap[k] ?: k, entrateMap[k] ?: 0.0, usciteMap[k] ?: 0.0)
        }.takeLast(12)
    }

    fun categoryTotals(expenses: List<SExpense>): List<CatTotal> =
        expenses.groupBy { it.category }
            .map { (cat, list) -> CatTotal(cat, list.sumOf { it.amount }) }
            .sortedByDescending { it.amount }
            .take(8)

    // ── Chart 1: Barre mensili ────────────────────────────────────────

    fun barChartXml(monthly: List<MonthTotal>): String {
        val n = monthly.size
        fun pts(vals: List<Double>) = vals.mapIndexed { i, v ->
            "<c:pt idx=\"$i\"><c:v>${num(v)}</c:v></c:pt>"
        }.joinToString("")
        fun cats() = monthly.mapIndexed { i, m ->
            "<c:pt idx=\"$i\"><c:v>${m.label.esc()}</c:v></c:pt>"
        }.joinToString("")

        val months  = cats()
        val inPts   = pts(monthly.map { it.entrate })
        val outPts  = pts(monthly.map { it.uscite })
        val ns = "xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\" " +
                 "xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" " +
                 "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\""
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<c:chartSpace $ns>
  <c:chart>
    <c:title><c:tx><c:rich><a:bodyPr/><a:lstStyle/><a:p><a:r><a:t>Entrate vs Uscite per Mese</a:t></a:r></a:p></c:rich></c:tx><c:overlay val="0"/></c:title>
    <c:plotArea>
      <c:barChart>
        <c:barDir val="col"/><c:grouping val="clustered"/>
        <c:ser>
          <c:idx val="0"/><c:order val="0"/>
          <c:tx><c:strRef><c:f/><c:strCache><c:ptCount val="1"/><c:pt idx="0"><c:v>Entrate</c:v></c:pt></c:strCache></c:strRef></c:tx>
          <c:spPr><a:solidFill><a:srgbClr val="1A9A8A"/></a:solidFill><a:ln><a:noFill/></a:ln></c:spPr>
          <c:cat><c:strLit><c:ptCount val="$n"/>$months</c:strLit></c:cat>
          <c:val><c:numLit><c:ptCount val="$n"/>$inPts</c:numLit></c:val>
        </c:ser>
        <c:ser>
          <c:idx val="1"/><c:order val="1"/>
          <c:tx><c:strRef><c:f/><c:strCache><c:ptCount val="1"/><c:pt idx="0"><c:v>Uscite</c:v></c:pt></c:strCache></c:strRef></c:tx>
          <c:spPr><a:solidFill><a:srgbClr val="FF6B6B"/></a:solidFill><a:ln><a:noFill/></a:ln></c:spPr>
          <c:cat><c:strLit><c:ptCount val="$n"/>$months</c:strLit></c:cat>
          <c:val><c:numLit><c:ptCount val="$n"/>$outPts</c:numLit></c:val>
        </c:ser>
        <c:axId val="1001"/><c:axId val="1002"/>
      </c:barChart>
      <c:catAx><c:axId val="1001"/><c:scaling><c:orientation val="minMax"/></c:scaling><c:delete val="0"/><c:axPos val="b"/><c:crossAx val="1002"/></c:catAx>
      <c:valAx><c:axId val="1002"/><c:scaling><c:orientation val="minMax"/></c:scaling><c:delete val="0"/><c:axPos val="l"/><c:numFmt formatCode="#,##0" sourceLinked="0"/><c:crossAx val="1001"/></c:valAx>
    </c:plotArea>
    <c:legend><c:legendPos val="b"/></c:legend>
    <c:plotVisOnly val="1"/>
  </c:chart>
</c:chartSpace>"""
    }

    // ── Chart 2: Torta categorie spese ───────────────────────────────

    fun pieChartXml(cats: List<CatTotal>): String {
        val n = cats.size
        val catPts = cats.mapIndexed { i, c ->
            "<c:pt idx=\"$i\"><c:v>${c.category.esc()}</c:v></c:pt>"
        }.joinToString("")
        val valPts = cats.mapIndexed { i, c ->
            "<c:pt idx=\"$i\"><c:v>${num(c.amount)}</c:v></c:pt>"
        }.joinToString("")
        val ns = "xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\" " +
                 "xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" " +
                 "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\""
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<c:chartSpace $ns>
  <c:chart>
    <c:title><c:tx><c:rich><a:bodyPr/><a:lstStyle/><a:p><a:r><a:t>Composizione Spese per Categoria</a:t></a:r></a:p></c:rich></c:tx><c:overlay val="0"/></c:title>
    <c:plotArea>
      <c:pieChart>
        <c:ser>
          <c:idx val="0"/><c:order val="0"/>
          <c:dLbls><c:showLegendKey val="0"/><c:showVal val="0"/><c:showCatName val="1"/><c:showSerName val="0"/><c:showPercent val="1"/></c:dLbls>
          <c:cat><c:strLit><c:ptCount val="$n"/>$catPts</c:strLit></c:cat>
          <c:val><c:numLit><c:ptCount val="$n"/>$valPts</c:numLit></c:val>
        </c:ser>
      </c:pieChart>
    </c:plotArea>
    <c:legend><c:legendPos val="r"/></c:legend>
    <c:plotVisOnly val="1"/>
  </c:chart>
</c:chartSpace>"""
    }

    // ── Drawing & Relationships ───────────────────────────────────────

    fun drawingXml(): String {
        val ns = "xmlns:xdr=\"http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing\" " +
                 "xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" " +
                 "xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\" " +
                 "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\""
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<xdr:wsDr $ns>
  <xdr:twoCellAnchor>
    <xdr:from><xdr:col>0</xdr:col><xdr:colOff>0</xdr:colOff><xdr:row>19</xdr:row><xdr:rowOff>0</xdr:rowOff></xdr:from>
    <xdr:to><xdr:col>10</xdr:col><xdr:colOff>0</xdr:colOff><xdr:row>35</xdr:row><xdr:rowOff>0</xdr:rowOff></xdr:to>
    <xdr:graphicFrame macro=""><xdr:nvGraphicFramePr><xdr:cNvPr id="2" name="Chart1"/><xdr:cNvGraphicFramePr><a:graphicFrameLocks noGrp="1"/></xdr:cNvGraphicFramePr></xdr:nvGraphicFramePr><xdr:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/></xdr:xfrm><a:graphic><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/chart"><c:chart r:id="rId1"/></a:graphicData></a:graphic></xdr:graphicFrame>
    <xdr:clientData/>
  </xdr:twoCellAnchor>
  <xdr:twoCellAnchor>
    <xdr:from><xdr:col>0</xdr:col><xdr:colOff>0</xdr:colOff><xdr:row>36</xdr:row><xdr:rowOff>0</xdr:rowOff></xdr:from>
    <xdr:to><xdr:col>10</xdr:col><xdr:colOff>0</xdr:colOff><xdr:row>52</xdr:row><xdr:rowOff>0</xdr:rowOff></xdr:to>
    <xdr:graphicFrame macro=""><xdr:nvGraphicFramePr><xdr:cNvPr id="3" name="Chart2"/><xdr:cNvGraphicFramePr><a:graphicFrameLocks noGrp="1"/></xdr:cNvGraphicFramePr></xdr:nvGraphicFramePr><xdr:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/></xdr:xfrm><a:graphic><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/chart"><c:chart r:id="rId2"/></a:graphicData></a:graphic></xdr:graphicFrame>
    <xdr:clientData/>
  </xdr:twoCellAnchor>
</xdr:wsDr>"""
    }

    fun drawingRelsXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/chart" Target="../charts/chart1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/chart" Target="../charts/chart2.xml"/>
</Relationships>"""

    fun sheet1RelsXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/drawing" Target="../drawings/drawing1.xml"/>
</Relationships>"""

    // ── Helpers ───────────────────────────────────────────────────────

    private fun num(d: Double) = String.format(Locale.US, "%.2f", d)
    private fun String.esc() = replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;")
}
