package com.condogest.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.condogest.app.data.model.*
import com.condogest.app.ui.components.*
import com.condogest.app.ui.theme.*
import com.condogest.app.viewmodel.CondoViewModel
import java.util.Calendar

@Composable
fun PaymentsScreen(viewModel: CondoViewModel) {
    val payments  by viewModel.payments.collectAsState()
    val cedolini  by viewModel.cedolini.collectAsState()
    val units     by viewModel.units.collectAsState()
    val totalPayments by viewModel.totalPayments.collectAsState()

    val activeView   by viewModel.paymentsView.collectAsState()
    val filterMethod by viewModel.paymentsFilterMethod.collectAsState()
    val filterScala  by viewModel.paymentsFilterScala.collectAsState()

    var searchQuery    by rememberSaveable { mutableStateOf("") }
    var showDialog     by remember { mutableStateOf(false) }
    var editingPayment by remember { mutableStateOf<Payment?>(null) }
    var deleteTarget   by remember { mutableStateOf<Payment?>(null) }

    val availableScale = remember(units) {
        units.map { it.scala }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val filteredPayments = remember(payments, filterMethod, filterScala, searchQuery, units) {
        val scalaIds = filterScala?.let { sc -> units.filter { it.scala == sc }.map { it.id }.toSet() }
        payments
            .filter { p -> filterMethod == null || p.method == filterMethod }
            .filter { p -> scalaIds == null || p.unitId in scalaIds }
            .filter { p ->
                if (searchQuery.isBlank()) true
                else {
                    val u = units.find { it.id == p.unitId }
                    val name = u?.let { "Int. ${it.number} ${it.ownerName}" } ?: ""
                    name.contains(searchQuery, ignoreCase = true) ||
                    p.reference.contains(searchQuery, ignoreCase = true)
                }
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Summary ──
            item {
                SummaryCard(
                    title = "Totale Incassato",
                    value = Formatters.currency(totalPayments),
                    icon = Icons.Filled.AccountBalanceWallet,
                    accentColor = Green400,
                    subtitle = "${payments.size} pagamenti registrati"
                )
            }

            // ── Ricerca ──
            item {
                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    placeholder = { Text("Cerca proprietario, riferimento…", style = MaterialTheme.typography.bodySmall, color = TextMuted) },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = TextMuted, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Filled.Close, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Cyan400, unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
                        focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface, cursorColor = Cyan400
                    )
                )
            }

            // ── Filtri compatti su un'unica riga scrollabile ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Toggle vista
                    Surface(shape = RoundedCornerShape(10.dp), color = DarkSurface) {
                        Row(modifier = Modifier.padding(2.dp)) {
                            listOf(0 to "🏠 Unità", 1 to "📅 Mese").forEach { (idx, label) ->
                                Surface(
                                    onClick = { viewModel.setPaymentsView(idx) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (activeView == idx) Cyan400.copy(alpha = 0.18f) else Color.Transparent
                                ) {
                                    Text(label,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (activeView == idx) FontWeight.Bold else FontWeight.Normal),
                                        color = if (activeView == idx) Cyan400 else TextMuted
                                    )
                                }
                            }
                        }
                    }

                    VerticalDivider(modifier = Modifier.height(28.dp), color = TextMuted.copy(alpha = 0.3f))

                    // Metodi
                    FilterChip(selected = filterMethod == null, onClick = { viewModel.setPaymentsFilterMethod(null) },
                        label = { Text("Tutti") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Cyan500.copy(alpha = 0.2f)))
                    PaymentMethods.methods.forEach { m ->
                        FilterChip(selected = filterMethod == m,
                            onClick = { viewModel.setPaymentsFilterMethod(if (filterMethod == m) null else m) },
                            label = { Text(m) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Cyan500.copy(alpha = 0.2f)))
                    }

                    // Scale (se presenti)
                    if (availableScale.isNotEmpty()) {
                        VerticalDivider(modifier = Modifier.height(28.dp), color = TextMuted.copy(alpha = 0.3f))
                        availableScale.forEach { sc ->
                            FilterChip(selected = filterScala == sc,
                                onClick = { viewModel.setPaymentsFilterScala(if (filterScala == sc) null else sc) },
                                label = { Text(sc) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Purple400.copy(alpha = 0.2f)))
                        }
                    }
                }
            }

            if (filteredPayments.isEmpty() && units.isEmpty()) {
                item { EmptyState("Nessun pagamento registrato", Icons.Filled.CreditCard) }
            } else if (activeView == 0) {
                // ── VISTA PER UNITÀ — Posizione contabile ──
                val sortedUnits = units.sortedBy { "${it.scala}_${it.number}" }
                sortedUnits.forEach { unit ->
                    val unitPayments = filteredPayments.filter { it.unitId == unit.id }
                    val unitCedolini = cedolini.filter { it.unitId == unit.id }
                    val addebitato  = unitCedolini.sumOf { it.total }
                    val incassato   = unitPayments.sumOf { it.amount }
                    val saldo       = addebitato - incassato

                    item(key = "uc_${unit.id}") {
                        UnitAccountCard(
                            unit        = unit,
                            addebitato  = addebitato,
                            incassato   = incassato,
                            saldo       = saldo,
                            cedolini    = unitCedolini,
                            payments    = unitPayments,
                            onAddPayment = { editingPayment = null; showDialog = true; /* pre-select unit handled in dialog */ }
                        )
                    }
                }

            } else {
                // ── VISTA PER MESE ──
                val monthNames = listOf("Gennaio","Febbraio","Marzo","Aprile","Maggio","Giugno",
                    "Luglio","Agosto","Settembre","Ottobre","Novembre","Dicembre")
                val grouped = filteredPayments
                    .groupBy { p ->
                        val cal = Calendar.getInstance().apply { timeInMillis = p.date }
                        cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.MONTH) + 1
                    }.toSortedMap(compareByDescending { it })

                grouped.forEach { (ym, monthPayments) ->
                    val year  = ym / 100
                    val month = ym % 100
                    val label = "${monthNames[month - 1]} $year"
                    item(key = "hm_$ym") {
                        PaymentGroupHeader(label = label, count = monthPayments.size,
                            total = monthPayments.sumOf { it.amount }, icon = Icons.Filled.CalendarToday)
                    }
                    items(monthPayments.sortedByDescending { it.date }, key = { "m_${it.id}" }) { p ->
                        PaymentRow(payment = p, showUnit = true, unitName = viewModel.getUnitName(p.unitId),
                            onEdit = { editingPayment = p; showDialog = true },
                            onDelete = { deleteTarget = p })
                    }
                }
            }
        }

        GradientFab(icon = Icons.Filled.Add, contentDescription = "Registra pagamento",
            onClick = { editingPayment = null; showDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp))
    }

    if (showDialog && units.isNotEmpty()) {
        PaymentFormDialog(payment = editingPayment, units = units,
            onDismiss = { showDialog = false; editingPayment = null },
            onSave = { p ->
                if (editingPayment != null) viewModel.updatePayment(p) else viewModel.addPayment(p)
                showDialog = false; editingPayment = null
            })
    }

    deleteTarget?.let { p ->
        ConfirmDeleteDialog(
            itemName = "${Formatters.currency(p.amount)} - ${viewModel.getUnitName(p.unitId)}",
            onConfirm = { viewModel.deletePayment(p); deleteTarget = null },
            onDismiss = { deleteTarget = null })
    }
}

// ─── Card posizione contabile per unità ───────────────────────────────────────
@Composable
private fun UnitAccountCard(
    unit: CondoUnit,
    addebitato: Double,
    incassato: Double,
    saldo: Double,
    cedolini: List<Cedolino>,
    payments: List<Payment>,
    onAddPayment: () -> Unit
) {
    var expanded by remember { mutableStateOf(saldo > 0.01) } // auto-espande se c'è saldo
    val saldoColor = when {
        saldo <= 0.01 -> Green400
        saldo < addebitato * 0.5 -> Amber400
        else -> Red400
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, saldoColor.copy(alpha = 0.3f))
    ) {
        Column {
            // Header unità
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp)
                        .background(saldoColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Home, null, tint = saldoColor, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        buildString {
                            if (unit.scala.isNotBlank()) append("Sc.${unit.scala} · ")
                            append("Int. ${unit.number}")
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(unit.ownerName, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(32.dp)) {
                    Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        null, tint = TextMuted, modifier = Modifier.size(20.dp))
                }
            }

            // Riepilogo cifre (sempre visibile)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AccountStat("Addebitato", addebitato, TextSecondary, Modifier.weight(1f))
                AccountStat("Incassato", incassato, Green400, Modifier.weight(1f))
                AccountStat(if (saldo <= 0.01) "In regola" else "Da incassare", saldo, saldoColor, Modifier.weight(1f))
            }

            // Dettaglio espandibile
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = saldoColor.copy(alpha = 0.2f))
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {

                        // Cedolini
                        if (cedolini.isNotEmpty()) {
                            Text("📋 Cedolini", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Cyan400)
                            cedolini.sortedByDescending { it.issueDate }.forEach { c ->
                                Row(modifier = Modifier.fillMaxWidth()
                                    .background(DarkBg, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(c.period, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                                        Text("Scad. ${Formatters.date(c.dueDate)}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    }
                                    StatusBadge(c.status)
                                    Spacer(Modifier.width(8.dp))
                                    Text(Formatters.currency(c.total), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (c.status == "Pagato") Green400 else Amber400)
                                }
                            }
                        }

                        // Pagamenti
                        if (payments.isNotEmpty()) {
                            Spacer(Modifier.height(2.dp))
                            Text("✅ Pagamenti ricevuti", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Green400)
                            payments.sortedByDescending { it.date }.forEach { p ->
                                Row(modifier = Modifier.fillMaxWidth()
                                    .background(DarkBg, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(Formatters.currency(p.amount), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Green400)
                                            Spacer(Modifier.width(6.dp))
                                            StatusBadge(p.method)
                                        }
                                        if (p.reference.isNotBlank()) Text("Rif: ${p.reference}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    }
                                    Text(Formatters.date(p.date), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                }
                            }
                        }

                        if (saldo > 0.01) {
                            Spacer(Modifier.height(2.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = Red400.copy(alpha = 0.08f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Red400.copy(alpha = 0.3f))
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Warning, null, tint = Red400, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Saldo aperto: ${Formatters.currency(saldo)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = Red400)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountStat(label: String, value: Double, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(10.dp), color = color.copy(alpha = 0.07f)) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(Formatters.currency(value), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold), color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}

// ─── Header gruppo mese ───────────────────────────────────────────────
@Composable
private fun PaymentGroupHeader(label: String, count: Int, total: Double, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(colors = CardDefaults.cardColors(containerColor = DarkSurface), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Green400, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                Text("$count ${if (count == 1) "pagamento" else "pagamenti"}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Text(Formatters.currency(total), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Green400)
        }
    }
}

@Composable
private fun PaymentRow(payment: Payment, showUnit: Boolean, unitName: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    ItemCard(onEdit = onEdit, onDelete = onDelete) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                if (showUnit) Text(unitName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(Formatters.currency(payment.amount), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Green400)
                    Spacer(Modifier.width(8.dp))
                    StatusBadge(payment.method)
                }
                if (payment.reference.isNotBlank()) Text("Rif: ${payment.reference}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Text(Formatters.date(payment.date), style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
    }
}

// ─── Form dialog ──────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentFormDialog(payment: Payment?, units: List<CondoUnit>, onDismiss: () -> Unit, onSave: (Payment) -> Unit) {
    var selectedUnit  by remember { mutableStateOf(units.find { it.id == payment?.unitId } ?: units.first()) }
    var amount        by remember { mutableStateOf(payment?.amount?.toString() ?: "") }
    var method        by remember { mutableStateOf(payment?.method ?: PaymentMethods.methods.first()) }
    var reference     by remember { mutableStateOf(payment?.reference ?: "") }
    var notes         by remember { mutableStateOf(payment?.notes ?: "") }
    var unitExpanded  by remember { mutableStateOf(false) }
    var methodExpanded by remember { mutableStateOf(false) }

    val methodIcons = mapOf("Contanti" to "💵","Bonifico" to "🏦","Bollettino Postale" to "📮","RID / Addebito diretto" to "🔄","Assegno" to "📝")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = { Text(if (payment != null) "Modifica Pagamento" else "Nuovo Pagamento", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(expanded = unitExpanded, onExpandedChange = { unitExpanded = it }) {
                    OutlinedTextField(
                        value = buildString { if (selectedUnit.scala.isNotBlank()) append("Sc.${selectedUnit.scala} · "); append("Int. ${selectedUnit.number} - ${selectedUnit.ownerName}") },
                        onValueChange = {}, readOnly = true, label = { Text("Unità") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(unitExpanded) }
                    )
                    ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                        units.sortedBy { "${it.scala}_${it.number}" }.forEach { u ->
                            DropdownMenuItem(
                                text = { Text(buildString { if (u.scala.isNotBlank()) append("Sc.${u.scala} · "); append("Int. ${u.number} - ${u.ownerName}") }, color = TextPrimary) },
                                onClick = { selectedUnit = u; unitExpanded = false })
                        }
                    }
                }
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Importo (€)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                ExposedDropdownMenuBox(expanded = methodExpanded, onExpandedChange = { methodExpanded = it }) {
                    OutlinedTextField(value = "${methodIcons[method] ?: "💳"} $method", onValueChange = {},
                        readOnly = true, label = { Text("Metodo") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(methodExpanded) })
                    ExposedDropdownMenu(expanded = methodExpanded, onDismissRequest = { methodExpanded = false }) {
                        PaymentMethods.methods.forEach { m ->
                            DropdownMenuItem(text = { Text("${methodIcons[m] ?: "💳"} $m", color = TextPrimary) },
                                onClick = { method = m; methodExpanded = false })
                        }
                    }
                }
                OutlinedTextField(value = reference, onValueChange = { reference = it },
                    label = { Text("Riferimento (es. CRO, n° bollettino)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = notes, onValueChange = { notes = it },
                    label = { Text("Note") }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(Payment(id = payment?.id ?: 0, unitId = selectedUnit.id, amount = amount.toDoubleOrNull() ?: 0.0, date = payment?.date ?: System.currentTimeMillis(), method = method, reference = reference, notes = notes)) },
                enabled = (amount.toDoubleOrNull() ?: 0.0) > 0,
                colors = ButtonDefaults.buttonColors(containerColor = Green500)
            ) { Text("Salva", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla", color = TextSecondary) } }
    )
}
