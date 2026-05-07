package com.renttrack.app.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renttrack.app.data.dao.MonthTotal
import com.renttrack.app.data.dao.YearTotal
import com.renttrack.app.ui.components.Formatters
import com.renttrack.app.ui.components.SummaryCard
import com.renttrack.app.ui.theme.*
import com.renttrack.app.viewmodel.RentViewModel

private val MONTHS = listOf("Gen","Feb","Mar","Apr","Mag","Giu","Lug","Ago","Set","Ott","Nov","Dic")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: RentViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("📊 Panoramica", "📅 Mensile", "📁 Archivio")

    Column(modifier = Modifier.fillMaxSize().background(DarkBg)) {
        // ── Tab Row ──────────────────────────────────────────────────
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkSurface,
            contentColor = Cyan400,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Cyan400
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selectedTab == index) Cyan400 else TextMuted
                        )
                    }
                )
            }
        }

        // Callback per navigare al tab Mensile con un anno specifico
        val navigateToMensile: (Int) -> Unit = { year ->
            viewModel.setSelectedYear(year)
            selectedTab = 1
        }

        // Crossfade è più stabile di AnimatedContent con LazyColumn dentro i tab:
        // evita il crash quando si cambia tab mentre il LazyColumn sta ancora componendo.
        Crossfade(targetState = selectedTab, label = "report_tabs") { tab ->
            when (tab) {
                0 -> PanoramicaTab(viewModel)
                1 -> MensileTab(viewModel)
                2 -> ArchivioTab(viewModel, onViewMensile = navigateToMensile)
            }
        }
    }
}

// ─── TAB 1: Panoramica ───────────────────────────────────────────────
@Composable
private fun PanoramicaTab(viewModel: RentViewModel) {
    val expenses by viewModel.expenses.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val totalExpenses by viewModel.totalExpenses.collectAsState()
    val totalPayments by viewModel.totalPayments.collectAsState()
    val expensesByCategory by viewModel.expensesByCategory.collectAsState()
    val units by viewModel.units.collectAsState()

    val saldo = totalPayments - totalExpenses
    val saldoPositive = saldo >= 0

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryCard(
                    title = "Spese Totali", value = Formatters.currency(totalExpenses),
                    icon = Icons.Filled.TrendingDown, accentColor = Color(0xFFFF6B6B),
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Incassi Totali", value = Formatters.currency(totalPayments),
                    icon = Icons.Filled.TrendingUp, accentColor = Green400,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            // Saldo
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (saldoPositive) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        null,
                        tint = if (saldoPositive) Green400 else Color(0xFFFF6B6B),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Saldo Complessivo", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                        Text(
                            Formatters.currency(saldo),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (saldoPositive) Green400 else Color(0xFFFF6B6B)
                        )
                    }
                }
            }
        }
        // Spese per categoria
        if (expensesByCategory.isNotEmpty()) {
            item {
                Text("Spese per categoria", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
            }
            val maxCat = expensesByCategory.maxOfOrNull { it.total } ?: 1.0
            items(expensesByCategory) { cat ->
                CategoryBar(cat.category, cat.total, maxCat)
            }
        }
        // Statistiche generali
        item {
            Text("Statistiche", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
        }
        item {
            StatGrid(
                listOf(
                    "N° Spese" to "${expenses.size}",
                    "N° Pagamenti" to "${payments.size}",
                    "N° Inquilini" to "${units.size}",
                    "Media Spesa" to if (expenses.isEmpty()) "—" else Formatters.currency(totalExpenses / expenses.size)
                )
            )
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ─── TAB 2: Mensile ──────────────────────────────────────────────────
@Composable
private fun MensileTab(viewModel: RentViewModel) {
    val selectedYear by viewModel.selectedYear.collectAsState()
    val monthlyExp by viewModel.monthlyExpenses.collectAsState()
    val monthlyPay by viewModel.monthlyPayments.collectAsState()
    val availableYears by viewModel.availableYears.collectAsState()

    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    val yearsToShow = (availableYears + currentYear).distinct().sortedDescending()

    val expMap = monthlyExp.associate { it.month to it.total }
    val payMap = monthlyPay.associate { it.month to it.total }
    val totalExp = monthlyExp.sumOf { it.total }
    val totalPay = monthlyPay.sumOf { it.total }
    val maxVal = (monthlyExp + monthlyPay).maxOfOrNull { it.total } ?: 1.0

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Selettore Anno
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.setSelectedYear(selectedYear - 1) },
                        enabled = selectedYear > (yearsToShow.minOrNull() ?: selectedYear - 5)
                    ) {
                        Icon(Icons.Filled.ChevronLeft, "Anno precedente", tint = Cyan400)
                    }
                    Text(
                        selectedYear.toString(),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    IconButton(
                        onClick = { viewModel.setSelectedYear(selectedYear + 1) },
                        enabled = selectedYear < currentYear
                    ) {
                        Icon(Icons.Filled.ChevronRight, "Anno successivo", tint = Cyan400)
                    }
                }
            }
        }
        // Totali anno
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryCard(
                    title = "Spese $selectedYear", value = Formatters.currency(totalExp),
                    icon = Icons.Filled.TrendingDown, accentColor = Color(0xFFFF6B6B),
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Incassi $selectedYear", value = Formatters.currency(totalPay),
                    icon = Icons.Filled.TrendingUp, accentColor = Green400,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        // Lista mesi
        items((1..12).toList()) { month ->
            val exp = expMap[month] ?: 0.0
            val pay = payMap[month] ?: 0.0
            val saldo = pay - exp
            if (exp == 0.0 && pay == 0.0) return@items
            MonthRow(
                monthName = MONTHS[month - 1],
                expenses = exp,
                payments = pay,
                saldo = saldo,
                maxVal = maxVal
            )
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ─── TAB 3: Archivio ─────────────────────────────────────────────────
@Composable
private fun ArchivioTab(viewModel: RentViewModel, onViewMensile: (Int) -> Unit) {
    val yearlyExp by viewModel.yearlyExpenses.collectAsState()
    val yearlyPay by viewModel.yearlyPayments.collectAsState()

    val allYears = (yearlyExp.map { it.year } + yearlyPay.map { it.year })
        .distinct().sortedDescending()

    if (allYears.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.FolderOpen, null, tint = TextMuted, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(8.dp))
                Text("Nessun dato storico disponibile", color = TextMuted)
            }
        }
        return
    }

    val expByYear = yearlyExp.associate { it.year to it }
    val payByYear = yearlyPay.associate { it.year to it }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Archivio Storico",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Text(
                "${allYears.size} anni con dati",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
        items(allYears) { year ->
            val exp = expByYear[year]?.total ?: 0.0
            val pay = payByYear[year]?.total ?: 0.0
            val expCount = expByYear[year]?.count ?: 0
            val payCount = payByYear[year]?.count ?: 0
            YearCard(
                year = year,
                expenses = exp, expCount = expCount,
                payments = pay, payCount = payCount,
                onSelect = { onViewMensile(year) }
            )
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ─── Componenti locali ───────────────────────────────────────────────

@Composable
private fun CategoryBar(category: String, total: Double, maxVal: Double) {
    val fraction = (total / maxVal).coerceIn(0.0, 1.0).toFloat()
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(category, style = MaterialTheme.typography.bodySmall, color = TextPrimary, modifier = Modifier.weight(1f))
                Text(Formatters.currency(total), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = Cyan400)
            }
            Spacer(Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(DarkSurface)) {
                Box(modifier = Modifier.fillMaxWidth(fraction).height(6.dp).clip(RoundedCornerShape(3.dp)).background(
                    Brush.horizontalGradient(listOf(Cyan500, Cyan400))
                ))
            }
        }
    }
}

@Composable
private fun StatGrid(items: List<Pair<String, String>>) {
    // Usiamo Column + Row espliciti invece di chunked+weight per evitare
    // crash da layout instabile quando il numero di item cambia
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (label, value) ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                        }
                    }
                }
                // Se la riga ha un solo elemento, riempi il secondo slot con uno Spacer
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MonthRow(monthName: String, expenses: Double, payments: Double, saldo: Double, maxVal: Double) {
    val expFrac = (expenses / maxVal).coerceIn(0.0, 1.0).toFloat()
    val payFrac = (payments / maxVal).coerceIn(0.0, 1.0).toFloat()
    val saldoPositive = saldo >= 0

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    monthName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    modifier = Modifier.width(40.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Barra spese
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(DarkSurface)) {
                            Box(modifier = Modifier.fillMaxWidth(expFrac).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFFF6B6B)))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(Formatters.currency(expenses), style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF6B6B), modifier = Modifier.width(72.dp), textAlign = TextAlign.End)
                    }
                    // Barra incassi
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(DarkSurface)) {
                            Box(modifier = Modifier.fillMaxWidth(payFrac).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Green400))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(Formatters.currency(payments), style = MaterialTheme.typography.labelSmall, color = Green400, modifier = Modifier.width(72.dp), textAlign = TextAlign.End)
                    }
                }
                Spacer(Modifier.width(8.dp))
                // Saldo
                Column(horizontalAlignment = Alignment.End) {
                    Text("Saldo", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(
                        Formatters.currency(saldo),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (saldoPositive) Green400 else Color(0xFFFF6B6B)
                    )
                }
            }
        }
    }
}

@Composable
private fun YearCard(year: Int, expenses: Double, expCount: Int, payments: Double, payCount: Int, onSelect: () -> Unit) {
    val saldo = payments - expenses
    val saldoPositive = saldo >= 0
    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            year.toString(),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Cyan400
                        )
                        if (year == currentYear) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Cyan400.copy(alpha = 0.15f)
                            ) {
                                Text("In corso", style = MaterialTheme.typography.labelSmall, color = Cyan400, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Text(
                        "$expCount spese · $payCount pagamenti",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Saldo", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(
                        Formatters.currency(saldo),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (saldoPositive) Green400 else Color(0xFFFF6B6B)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = TextMuted.copy(alpha = 0.15f))
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LabeledAmount("Spese", expenses, Color(0xFFFF6B6B))
                LabeledAmount("Incassi", payments, Green400)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onSelect,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan400),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Filled.CalendarMonth, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Vedi ripartizione mensile", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun LabeledAmount(label: String, amount: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(Formatters.currency(amount), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = color)
    }
}
