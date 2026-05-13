package com.renttrack.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renttrack.app.viewmodel.MonthlyTotal
import com.renttrack.app.viewmodel.YearlyTotal
import com.renttrack.app.ui.components.Formatters
import com.renttrack.app.ui.components.SummaryCard
import com.renttrack.app.ui.theme.*
import com.renttrack.app.viewmodel.SupabaseRentViewModel

private val MONTHS = listOf("Gen","Feb","Mar","Apr","Mag","Giu","Lug","Ago","Set","Ott","Nov","Dic")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: SupabaseRentViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Panoramica", "Mensile", "Archivio")

    // Inizializza report dati al primo lancio
    LaunchedEffect(Unit) { viewModel.setReportScope(SupabaseRentViewModel.ReportScope.ACTIVE) }

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

        val navigateToMensile: (Int) -> Unit = { year ->
            viewModel.setSelectedYear(year)
            selectedTab = 1
        }
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PanoramicaTab(viewModel: SupabaseRentViewModel) {
    val totalExpenses   by viewModel.reportTotalExpenses.collectAsState()
    val totalPayments   by viewModel.reportTotalPayments.collectAsState()
    val expByCategory   by viewModel.reportExpensesByCategory.collectAsState()
    val units           by viewModel.units.collectAsState()
    val expenses        by viewModel.expenses.collectAsState()
    val payments        by viewModel.payments.collectAsState()
    val monthlyExp      by viewModel.reportMonthlyExpenses.collectAsState()
    val monthlyPay      by viewModel.reportMonthlyPayments.collectAsState()
    val availableYears  by viewModel.reportAvailableYears.collectAsState()
    val selectedYear    by viewModel.selectedYear.collectAsState()
    val allCondomini    by viewModel.allCondomini.collectAsState()
    val reportScope     by viewModel.reportScope.collectAsState()
    val reportIsLoading by viewModel.reportIsLoading.collectAsState()
    var showCustomPicker by remember { mutableStateOf(false) }

    val saldo = totalPayments - totalExpenses
    val saldoPositive = saldo >= 0

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Toggle scope (visibile solo se >1 proprietà) ─────────────
        if (allCondomini.size > 1) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Dati:",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        FilterChip(
                            selected = reportScope == SupabaseRentViewModel.ReportScope.ACTIVE,
                            onClick = { viewModel.setReportScope(SupabaseRentViewModel.ReportScope.ACTIVE) },
                            label = { Text("Questa proprietà", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Cyan400.copy(alpha = 0.2f),
                                selectedLabelColor = Cyan400
                            )
                        )
                        FilterChip(
                            selected = reportScope == SupabaseRentViewModel.ReportScope.ALL,
                            onClick = { viewModel.setReportScope(SupabaseRentViewModel.ReportScope.ALL) },
                            label = { Text("Tutte (${allCondomini.size})", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Cyan400.copy(alpha = 0.2f),
                                selectedLabelColor = Cyan400
                            )
                        )
                        if (reportIsLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = Cyan400,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
        }
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

        // ── Grafico Incassi vs Spese mensile ─────────────────────────
        if (monthlyExp.isNotEmpty() || monthlyPay.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Header con selezione anno
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Incassi vs Spese",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary, modifier = Modifier.weight(1f)
                            )
                            // Chip anni
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                availableYears.forEach { year ->
                                    FilterChip(
                                        selected = selectedYear == year,
                                        onClick  = { viewModel.setSelectedYear(year) },
                                        label    = { Text("$year", style = MaterialTheme.typography.labelSmall) },
                                        colors   = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Cyan400.copy(alpha = 0.2f),
                                            selectedLabelColor = Cyan400
                                        )
                                    )
                                }
                            }
                        }
                        // Legenda
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            LegendDot(Green400, "Incassi")
                            LegendDot(Color(0xFFFF6B6B), "Spese")
                        }
                        // Grafico
                        MonthlyBarChart(
                            monthlyPayments = monthlyPay,
                            monthlyExpenses = monthlyExp,
                            modifier = Modifier.fillMaxWidth().height(180.dp)
                        )
                    }
                }
            }
        }

        // Spese per categoria
        if (expByCategory.isNotEmpty()) {
            item { Text("Spese per categoria", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary) }
            val maxCat = expByCategory.maxOfOrNull { it.total } ?: 1.0
            items(expByCategory) { cat -> CategoryBar(cat.category, cat.total, maxCat) }
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
        // ── Bottone Export CSV con dialog selezione immobile ────
        item {
            val ctx = LocalContext.current
            val allCondomini    by viewModel.allCondomini.collectAsState()
            val activeCondoId   by viewModel.activeCondominioId.collectAsState()
            var showExportDialog by remember { mutableStateOf(false) }

            OutlinedButton(
                onClick = { showExportDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan400),
                border = androidx.compose.foundation.BorderStroke(1.dp, Cyan400.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Download, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "Esporta CSV",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        "Scegli quale immobile esportare",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            // ── Dialog selezione immobile per export ───────────────
            if (showExportDialog) {
                // 0 = solo attiva, 1 = tutte, 2 = custom
                var mode by remember { mutableIntStateOf(0) }
                var customPicked by remember {
                    mutableStateOf(setOf(activeCondoId).filter { it.isNotBlank() }.toSet())
                }
                val availableYears by viewModel.reportAvailableYears.collectAsState()
                var selectedExportYear by remember { mutableStateOf<Int?>(null) } // null = tutti gli anni

                AlertDialog(
                    onDismissRequest = { showExportDialog = false },
                    containerColor = DarkSurface,
                    shape = RoundedCornerShape(20.dp),
                    icon = {
                        Icon(Icons.Filled.Download, null, tint = Cyan400, modifier = Modifier.size(28.dp))
                    },
                    title = {
                        Text(
                            "Esporta CSV",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Seleziona quale immobile includere nell'export:",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                            Spacer(Modifier.height(4.dp))

                            // ── Opzione: Solo attiva
                            val activeCondo = allCondomini.find { it.id == activeCondoId }
                            ExportOptionRow(
                                selected = mode == 0,
                                onClick = { mode = 0 },
                                label = activeCondo?.let {
                                    if (it.indirizzo.isNotBlank()) "${it.nome} — ${it.indirizzo}"
                                    else it.nome
                                } ?: "Proprietà attiva",
                                sublabel = "Solo questa proprietà"
                            )

                            // ── Opzione: Tutte
                            ExportOptionRow(
                                selected = mode == 1,
                                onClick = { mode = 1 },
                                label = "Tutte le proprietà",
                                sublabel = "${allCondomini.size} immobili"
                            )

                            // ── Opzione: Selezione custom (visibile solo se >1 immobile)
                            if (allCondomini.size > 1) {
                                ExportOptionRow(
                                    selected = mode == 2,
                                    onClick = { mode = 2 },
                                    label = "Selezione personalizzata",
                                    sublabel = if (mode == 2) "${customPicked.size} selezionati" else "Scegli quali includere"
                                )

                                // Checkbox lista immobili (visibile solo in modalità custom)
                                androidx.compose.animation.AnimatedVisibility(visible = mode == 2) {
                                    Column(
                                        modifier = androidx.compose.ui.Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp, top = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        allCondomini.forEach { condo ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Checkbox(
                                                    checked = condo.id in customPicked,
                                                    onCheckedChange = { checked ->
                                                        customPicked = customPicked.toMutableSet().also {
                                                            if (checked) it.add(condo.id)
                                                            else it.remove(condo.id)
                                                        }
                                                    },
                                                    colors = CheckboxDefaults.colors(checkedColor = Cyan400)
                                                )
                                                Column {
                                                    Text(
                                                        condo.nome,
                                                        color = TextPrimary,
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                    if (condo.indirizzo.isNotBlank()) {
                                                        Text(
                                                            condo.indirizzo,
                                                            color = TextMuted,
                                                            style = MaterialTheme.typography.labelSmall
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // ── Sezione filtro anno ────────────────────────────────
                            if (availableYears.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                HorizontalDivider(color = androidx.compose.ui.graphics.Color(0xFF2D3748))
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Periodo:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                                androidx.compose.foundation.lazy.LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Chip "Tutti"
                                    item {
                                        FilterChip(
                                            selected = selectedExportYear == null,
                                            onClick  = { selectedExportYear = null },
                                            label    = { Text("Tutti", style = MaterialTheme.typography.labelSmall) },
                                            colors   = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Cyan400.copy(alpha = 0.2f),
                                                selectedLabelColor     = Cyan400
                                            )
                                        )
                                    }
                                    items(availableYears.size) { i ->
                                        val y = availableYears[i]
                                        FilterChip(
                                            selected = selectedExportYear == y,
                                            onClick  = { selectedExportYear = y },
                                            label    = { Text("$y", style = MaterialTheme.typography.labelSmall) },
                                            colors   = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Cyan400.copy(alpha = 0.2f),
                                                selectedLabelColor     = Cyan400
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        val canConfirm = mode != 2 || customPicked.isNotEmpty()
                        Button(
                            onClick = {
                                showExportDialog = false
                                when (mode) {
                                    0 -> viewModel.setReportScope(SupabaseRentViewModel.ReportScope.ACTIVE)
                                    1 -> viewModel.setReportScope(SupabaseRentViewModel.ReportScope.ALL)
                                    2 -> viewModel.setReportScope(SupabaseRentViewModel.ReportScope.CUSTOM, customPicked)
                                }
                                viewModel.exportCSVAfterScope(ctx, mode, customPicked, activeCondoId, selectedExportYear)
                            },
                            enabled = canConfirm,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Cyan400,
                                contentColor = androidx.compose.ui.graphics.Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.Download, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Esporta", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showExportDialog = false }) {
                            Text("Annulla", color = TextMuted)
                        }
                    }
                )
            }
        }
        // ── Sezione Backup & Ripristino ─────────────────────────
        item {
            val ctx = LocalContext.current
            val backupStatus by viewModel.backupStatus.collectAsState()

            // File picker per ripristino ZIP
            val restoreLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.GetContent()
            ) { uri ->
                uri?.let { viewModel.restoreDatabase(ctx, it) }
            }

            // Snackbar di stato
            backupStatus?.let { msg ->
                LaunchedEffect(msg) {
                    kotlinx.coroutines.delay(4000)
                    viewModel.clearBackupStatus()
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (msg.startsWith("✅")) Color(0xFF1B4332).copy(alpha = 0.95f)
                            else Color(0xFF7F1D1D).copy(alpha = 0.95f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        msg,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            Text(
                "💾 Backup & Ripristino",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // ── Backup
                OutlinedButton(
                    onClick = { viewModel.backupDatabase(ctx) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Green400),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Green400.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.CloudUpload, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.height(4.dp))
                        Text("Backup", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
                        Text("Salva su Drive/Email", style = MaterialTheme.typography.labelSmall, color = TextMuted, textAlign = TextAlign.Center)
                    }
                }
                // ── Ripristino
                OutlinedButton(
                    onClick = { restoreLauncher.launch("application/zip") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Amber400),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Amber400.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.CloudDownload, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.height(4.dp))
                        Text("Ripristina", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
                        Text("Da file .zip", style = MaterialTheme.typography.labelSmall, color = TextMuted, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ─── TAB 2: Mensile ──────────────────────────────────────────────────
@Composable
private fun MensileTab(viewModel: SupabaseRentViewModel) {
    val selectedYear    by viewModel.selectedYear.collectAsState()
    val monthlyExp      by viewModel.reportMonthlyExpenses.collectAsState()
    val monthlyPay      by viewModel.reportMonthlyPayments.collectAsState()
    val availableYears  by viewModel.reportAvailableYears.collectAsState()

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
private fun ArchivioTab(viewModel: SupabaseRentViewModel, onViewMensile: (Int) -> Unit) {
    val yearlyExp by viewModel.reportYearlyExpenses.collectAsState()
    val yearlyPay by viewModel.reportYearlyPayments.collectAsState()

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

@Composable
private fun ExportOptionRow(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    sublabel: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Cyan400.copy(alpha = 0.10f) else DarkCard,
        border = if (selected)
            androidx.compose.foundation.BorderStroke(1.5.dp, Cyan400.copy(alpha = 0.6f))
        else
            androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF2D3748)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = Cyan400, unselectedColor = TextMuted),
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (selected) Cyan400 else TextPrimary
                )
                Text(
                    sublabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
    }
}

// ─── Grafico a barre mensile (Canvas puro) ────────────────────────────────────
@Composable
private fun MonthlyBarChart(
    monthlyPayments: List<MonthlyTotal>,
    monthlyExpenses: List<MonthlyTotal>,
    modifier: Modifier = Modifier
) {
    val allMonths = (1..12).toList()
    val payMap = monthlyPayments.associateBy { it.month }
    val expMap = monthlyExpenses.associateBy { it.month }
    val maxVal = (monthlyPayments + monthlyExpenses).maxOfOrNull { it.total } ?: 1.0

    // Animazione barre all'ingresso
    var animated by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animated = true }
    val animProgress by animateFloatAsState(
        targetValue = if (animated) 1f else 0f,
        animationSpec = tween(durationMillis = 700),
        label = "barAnim"
    )

    val payColor = android.graphics.Color.parseColor("#00C896")  // Green400
    val expColor = android.graphics.Color.parseColor("#FF6B6B")  // Red
    val textColor = android.graphics.Color.parseColor("#8899AA") // TextMuted
    val density = LocalDensity.current

    Canvas(modifier = modifier) {
        val barGroupW = size.width / 12f
        val barW      = barGroupW * 0.34f
        val maxH      = size.height - 28.dp.toPx()
        val bottomY   = size.height - 20.dp.toPx()

        // Linea asse X
        drawLine(
            color = Color(0xFF2D3748),
            start = Offset(0f, bottomY),
            end   = Offset(size.width, bottomY),
            strokeWidth = 1.dp.toPx()
        )

        allMonths.forEach { month ->
            val cx = (month - 1) * barGroupW + barGroupW / 2f

            val payH = ((payMap[month]?.total ?: 0.0) / maxVal * maxH * animProgress).toFloat()
            val expH = ((expMap[month]?.total ?: 0.0) / maxVal * maxH * animProgress).toFloat()

            // Barra Incassi (sinistra)
            if (payH > 0) {
                drawRoundRect(
                    color       = Color(0xFF00C896),
                    topLeft     = Offset(cx - barW - 1.dp.toPx(), bottomY - payH),
                    size        = Size(barW, payH),
                    cornerRadius = CornerRadius(3.dp.toPx())
                )
            }

            // Barra Spese (destra)
            if (expH > 0) {
                drawRoundRect(
                    color       = Color(0xFFFF6B6B),
                    topLeft     = Offset(cx + 1.dp.toPx(), bottomY - expH),
                    size        = Size(barW, expH),
                    cornerRadius = CornerRadius(3.dp.toPx())
                )
            }

            // Etichetta mese
            drawContext.canvas.nativeCanvas.drawText(
                MONTHS[month - 1],
                cx,
                size.height,
                android.graphics.Paint().apply {
                    color     = textColor
                    textSize  = with(density) { 9.sp.toPx() }
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
            )
        }
    }
}

// ─── Pallino legenda ──────────────────────────────────────────────────────────
@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
    }
}
