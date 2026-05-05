package com.renttrack.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.renttrack.app.data.model.CedolinoWithItems
import com.renttrack.app.data.model.ExpenseCategories
import com.renttrack.app.ui.components.*
import com.renttrack.app.ui.theme.*
import com.renttrack.app.viewmodel.RentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: RentViewModel) {
    val totalExpenses by viewModel.totalExpenses.collectAsState()
    val totalPayments by viewModel.totalPayments.collectAsState()
    val units by viewModel.units.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val pendingCedolini by viewModel.pendingCedolini.collectAsState()
    val cedoliniWithItems by viewModel.cedoliniWithItems.collectAsState()
    val expensesByCategory by viewModel.expensesByCategory.collectAsState()
    val lastCedolinoByUnit by viewModel.lastCedolinoByUnit.collectAsState()
    val morositaByUnit by viewModel.morositaByUnit.collectAsState()
    val balance = totalPayments - totalExpenses

    // Cedolini aperti (non pagati)
    val openCedolini = remember(cedoliniWithItems) {
        cedoliniWithItems.filter { it.cedolino.status != "Pagato" }
    }
    val totalOpen = remember(openCedolini) { openCedolini.sumOf { it.cedolino.total } }

    // Scadenze contratti
    val now = System.currentTimeMillis()
    val expiredUnits = remember(units) {
        units.filter { it.leaseEndDate != null && it.leaseEndDate < now }
    }
    val urgentUnits = remember(units) {
        units.filter { it.leaseEndDate != null && it.leaseEndDate >= now &&
            ((it.leaseEndDate - now) / (1000 * 60 * 60 * 24)) < 30 }
    }
    val soonUnits = remember(units) {
        units.filter { it.leaseEndDate != null && it.leaseEndDate >= now &&
            ((it.leaseEndDate - now) / (1000 * 60 * 60 * 24)).let { d -> d in 30..59 } }
    }

    var showOpenCedoliniSheet by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ─── Summary Cards ──────────────────────────────────────
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard(
                    title = "Totale Spese",
                    value = Formatters.currency(totalExpenses),
                    icon = Icons.Filled.TrendingDown,
                    accentColor = Red400,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Totale Incassi",
                    value = Formatters.currency(totalPayments),
                    icon = Icons.Filled.TrendingUp,
                    accentColor = Green400,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard(
                    title = "Saldo",
                    value = Formatters.currency(balance),
                    icon = Icons.Filled.AccountBalance,
                    accentColor = if (balance >= 0) Green400 else Amber400,
                    modifier = Modifier.weight(1f),
                    subtitle = if (balance >= 0) "In positivo" else "In negativo"
                )
                // ── Cedolini Aperti — cliccabile ──
                ClickableSummaryCard(
                    title = "Da Incassare",
                    value = Formatters.currency(totalOpen),
                    icon = Icons.Filled.Description,
                    accentColor = if (pendingCedolini > 0) Amber400 else Green400,
                    modifier = Modifier.weight(1f),
                    subtitle = "$pendingCedolini avvisi aperti",
                    onClick = { if (pendingCedolini > 0) showOpenCedoliniSheet = true }
                )
            }
        }

        // ─── Alert Scadenze Contratti ────────────────────────────
        if (expiredUnits.isNotEmpty() || urgentUnits.isNotEmpty() || soonUnits.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CalendarMonth, null, tint = Amber400, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Scadenze contratti",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                        }
                        if (expiredUnits.isNotEmpty()) {
                            ContractAlertRow(
                                count = expiredUnits.size,
                                label = if (expiredUnits.size == 1) "contratto scaduto" else "contratti scaduti",
                                color = Color(0xFFFF6B6B),
                                names = expiredUnits.map { it.ownerName }
                            )
                        }
                        if (urgentUnits.isNotEmpty()) {
                            ContractAlertRow(
                                count = urgentUnits.size,
                                label = if (urgentUnits.size == 1) "contratto scade entro 30 giorni" else "contratti scadono entro 30 giorni",
                                color = Color(0xFFFF6B6B),
                                names = urgentUnits.map { it.ownerName }
                            )
                        }
                        if (soonUnits.isNotEmpty()) {
                            ContractAlertRow(
                                count = soonUnits.size,
                                label = if (soonUnits.size == 1) "contratto scade entro 60 giorni" else "contratti scadono entro 60 giorni",
                                color = Amber400,
                                names = soonUnits.map { it.ownerName }
                            )
                        }
                    }
                }
            }
        }

        // ─── Semaforo Affitti ─────────────────────────────────
        if (units.isNotEmpty()) {
            item { SectionHeader("🚦 Stato affitti inquilini") }
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard)
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        units.forEach { unit ->
                            val lastCed = lastCedolinoByUnit[unit.id]
                            val morosita = morositaByUnit[unit.id] ?: 0.0
                            val (dotColor, statusLabel) = when {
                                lastCed == null                    -> Pair(TextMuted,             "Nessun avviso")
                                lastCed.status == "Pagato"         -> Pair(Green400,              "Pagato")
                                lastCed.status == "Scaduto"        -> Pair(Color(0xFFFF6B6B),     "Scaduto")
                                lastCed.status == "Parziale"       -> Pair(Amber400,              "Parziale")
                                else                               -> Pair(Amber400,              "In attesa")
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Pallino semaforo
                                Surface(shape = androidx.compose.foundation.shape.CircleShape, color = dotColor.copy(alpha = 0.25f), modifier = Modifier.size(28.dp)) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Surface(shape = androidx.compose.foundation.shape.CircleShape, color = dotColor, modifier = Modifier.size(12.dp)) {}
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(unit.ownerName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                                    Text(unit.number, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(statusLabel, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = dotColor)
                                    if (morosita > 0) {
                                        Text(Formatters.currency(morosita), style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF6B6B))
                                    } else if (unit.millesimi > 0) {
                                        Text(Formatters.currency(unit.millesimi), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    }
                                }
                            }
                            if (unit != units.last()) {
                                HorizontalDivider(color = DarkSurface, modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }
        }

        // ─── Spese per Categoria ────────────────────────────────
        item { SectionHeader("Spese per categoria immobile") }

        if (expensesByCategory.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        expensesByCategory.forEach { ct ->
                            val color = CategoryColors[ct.category] ?: TextSecondary
                            val icon = ExpenseCategories.getIcon(ct.category)
                            val pct = if (totalExpenses > 0) (ct.total / totalExpenses * 100) else 0.0
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CategoryChip(ct.category, icon)
                                Spacer(Modifier.weight(1f))
                                Text(
                                    Formatters.currency(ct.total),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextPrimary
                                )
                                Text(
                                    "  ${String.format("%.0f", pct)}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                            LinearProgressIndicator(
                                progress = { (pct / 100).toFloat() },
                                modifier = Modifier.fillMaxWidth().height(4.dp).padding(top = 2.dp),
                                color = color,
                                trackColor = color.copy(alpha = 0.1f)
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        }

        // ─── Ultime Spese ───────────────────────────────────────
        item { SectionHeader("Ultime spese di manutenzione") }

        val recentExpenses = expenses.take(5)
        items(recentExpenses) { expense ->
            val icon = ExpenseCategories.getIcon(expense.category)
            ItemCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryChip(expense.category, icon)
                    Spacer(Modifier.weight(1f))
                    Text(Formatters.date(expense.date), style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
                Spacer(Modifier.height(8.dp))
                Text(expense.description, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Text(
                    Formatters.currency(expense.amount),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Red400
                )
            }
        }

        // ─── Ultimi Pagamenti ───────────────────────────────────
        item { SectionHeader("Ultimi affitti incassati") }

        val recentPayments = payments.take(5)
        items(recentPayments) { payment ->
            ItemCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(viewModel.getUnitName(payment.unitId), style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                    StatusBadge(payment.method)
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        Formatters.currency(payment.amount),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Green400
                    )
                    Spacer(Modifier.weight(1f))
                    Text(Formatters.date(payment.date), style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }

    // ── Bottom Sheet: Riepilogo cedolini aperti ──────────────────
    if (showOpenCedoliniSheet) {
        ModalBottomSheet(
            onDismissRequest = { showOpenCedoliniSheet = false },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            OpenCedoliniSheet(
                openCedolini = openCedolini,
                totalOpen = totalOpen,
                units = units,
                viewModel = viewModel
            )
        }
    }
}

// ─── Bottom Sheet content ─────────────────────────────────────────────
@Composable
private fun OpenCedoliniSheet(
    openCedolini: List<CedolinoWithItems>,
    totalOpen: Double,
    units: List<com.renttrack.app.data.model.CondoUnit>,
    viewModel: RentViewModel
) {
    // Raggruppa per unità
    val byUnit = remember(openCedolini) {
        openCedolini
            .groupBy { it.cedolino.unitId }
            .toList()
            .sortedBy { (unitId, _) ->
                val u = units.find { it.id == unitId }
                "${u?.scala}_${u?.number}"
            }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Warning, null, tint = Amber400, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Affitti da Incassare",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = TextPrimary
                )
                Text(
                    "${openCedolini.size} avvisi aperti · ${byUnit.size} inquilini",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            // Totale evidenziato
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Amber400.copy(alpha = 0.15f)
            ) {
                Text(
                    Formatters.currency(totalOpen),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = Amber400,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = TextMuted.copy(alpha = 0.2f))
        Spacer(Modifier.height(8.dp))

        // Lista per proprietario
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.heightIn(max = 500.dp)
        ) {
            byUnit.forEach { (unitId, ceds) ->
                val unit = units.find { it.id == unitId }
                val ownerLabel = unit?.let {
                    buildString {
                        if (it.scala.isNotBlank()) append("${it.scala} · ")
                        append("${it.ownerName} — ${it.number}")
                    }
                } ?: "Inquilino sconosciuto"
                val unitTotal = ceds.sumOf { it.cedolino.total }

                item(key = "sheet_unit_$unitId") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkBg),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Intestazione proprietario
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Person, null, tint = Cyan400, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    ownerLabel,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    Formatters.currency(unitTotal),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Amber400
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            // Dettaglio ogni cedolino
                            ceds.sortedByDescending { it.cedolino.issueDate }.forEach { cwi ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 22.dp, top = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Status chip colorato
                                    val statusColor = when (cwi.cedolino.status) {
                                        "Scaduto"  -> Color(0xFFFF6B6B)
                                        "Parziale" -> Amber400
                                        else       -> Cyan400
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = statusColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            cwi.cedolino.status,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = statusColor,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        cwi.cedolino.period,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        "Scad: ${Formatters.date(cwi.cedolino.dueDate)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (cwi.cedolino.status == "Scaduto") Color(0xFFFF6B6B) else TextMuted
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        Formatters.currency(cwi.cedolino.total),
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Footer totale
            item {
                HorizontalDivider(color = Amber400.copy(alpha = 0.3f))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TOTALE DA INCASSARE", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold), color = TextSecondary)
                    Text(Formatters.currency(totalOpen), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = Amber400)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ─── Riga alert scadenza contratto ───────────────────────────────────────────
@Composable
private fun ContractAlertRow(count: Int, label: String, color: Color, names: List<String>) {
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.08f)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = androidx.compose.foundation.shape.CircleShape, color = color, modifier = Modifier.size(8.dp)) {}
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "$count $label",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = color
                )
                if (names.isNotEmpty()) {
                    Text(
                        names.joinToString(", "),
                        style = MaterialTheme.typography.labelSmall,
                        color = color.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
