package com.condogest.app.ui.screens

import androidx.compose.foundation.background
 import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.condogest.app.data.model.*
import com.condogest.app.ui.components.*
import com.condogest.app.ui.theme.*
import com.condogest.app.viewmodel.CondoViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CedoliniScreen(viewModel: CondoViewModel) {
    val cedolini by viewModel.cedolini.collectAsState()
    val cedoliniWithItems by viewModel.cedoliniWithItems.collectAsState()
    val pendingCount by viewModel.pendingCedolini.collectAsState()
    val units by viewModel.units.collectAsState()
    val context = LocalContext.current

    var showGenerateDialog by remember { mutableStateOf(false) }
    var showSingleDialog by remember { mutableStateOf(false) }
    var showQuotaDialog by remember { mutableStateOf(false) }
    var showDetailDialog by remember { mutableStateOf<CedolinoWithItems?>(null) }
    var showConfirmSendDialog by remember { mutableStateOf<Cedolino?>(null) }
    var showPagamentoDialog by remember { mutableStateOf<Cedolino?>(null) }
    var deleteTarget by remember { mutableStateOf<Cedolino?>(null) }
    var filterStatus by remember { mutableStateOf<String?>(null) }

    val sentCount = cedolini.count { it.sentToResident }
    val filtered = if (filterStatus != null) cedolini.filter { it.status == filterStatus } else cedolini

    Box(modifier = Modifier.fillMaxSize()) {
        if (cedolini.isEmpty()) {
            // Empty state con due CTA
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.Description, null, tint = TextMuted, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(12.dp))
                Text("Nessun cedolino generato", color = TextMuted, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { showSingleDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan500),
                    modifier = Modifier.padding(horizontal = 32.dp).fillMaxWidth()
                ) {
                    Icon(Icons.Filled.PersonAdd, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cedolino per singolo condomino")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showGenerateDialog = true },
                    modifier = Modifier.padding(horizontal = 32.dp).fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan400)
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Genera per tutte le unità")
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Statistiche
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SummaryCard(
                            title = "Totale", value = "${cedolini.size}",
                            icon = Icons.Filled.Description, accentColor = Cyan400,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCard(
                            title = "In Sospeso", value = "$pendingCount",
                            icon = Icons.Filled.PendingActions, accentColor = Amber400,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCard(
                            title = "Inviati", value = "$sentCount",
                            icon = Icons.Filled.Send, accentColor = Green400,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Filtri stato
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        FilterChip(
                            selected = filterStatus == null, onClick = { filterStatus = null },
                            label = { Text("Tutti") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Cyan500.copy(alpha = 0.2f))
                        )
                        CedolinoStatuses.statuses.forEach { s ->
                            FilterChip(
                                selected = filterStatus == s, onClick = { filterStatus = if (filterStatus == s) null else s },
                                label = { Text(s) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Cyan500.copy(alpha = 0.2f))
                            )
                        }
                    }
                }

                items(filtered, key = { it.id }) { cedolino ->
                    ItemCard(onDelete = { deleteTarget = cedolino }) {
                        // Header: unità + badge invio
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    viewModel.getUnitName(cedolino.unitId),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextPrimary
                                )
                                Text(cedolino.period, style = MaterialTheme.typography.bodySmall, color = Cyan400)
                            }
                            StatusBadge(cedolino.status)
                            if (cedolino.sentToResident) {
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Green400.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.Send, null, tint = Green400, modifier = Modifier.size(11.dp))
                                        Spacer(Modifier.width(3.dp))
                                        Text("Inviato", style = MaterialTheme.typography.labelSmall, color = Green400)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                Formatters.currency(cedolino.total),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Spacer(Modifier.weight(1f))
                            Text("Scad: ${Formatters.date(cedolino.dueDate)}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                        if (cedolino.paidAmount > 0 && cedolino.status != "Pagato") {
                            Text("Versato: ${Formatters.currency(cedolino.paidAmount)}", style = MaterialTheme.typography.bodySmall, color = Green400)
                        }
                        Spacer(Modifier.height(10.dp))

                        // Azioni
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            // Dettaglio
                            OutlinedButton(
                                onClick = {
                                    showDetailDialog = cedoliniWithItems.find { it.cedolino.id == cedolino.id }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan400)
                            ) {
                                Icon(Icons.Filled.Visibility, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Dettaglio", style = MaterialTheme.typography.labelSmall)
                            }
                            // Duplica (copia mese successivo)
                            val cwi = cedoliniWithItems.find { it.cedolino.id == cedolino.id }
                            if (cwi != null) {
                                OutlinedButton(
                                    onClick = { viewModel.duplicateCedolino(cwi) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Amber400)
                                ) {
                                    Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Duplica", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            // Condividi via WhatsApp/Email
                            val shareText = buildString {
                                val unitName = viewModel.getUnitName(cedolino.unitId)
                                val cwi2 = cedoliniWithItems.find { it.cedolino.id == cedolino.id }
                                appendLine("📋 Cedolino Condominio")
                                appendLine("Intestato a: $unitName")
                                appendLine("Periodo: ${cedolino.period}")
                                cwi2?.items?.forEach { appendLine("• ${it.description}: ${Formatters.currency(it.amount)}") }
                                appendLine("─────────────────")
                                appendLine("TOTALE: ${Formatters.currency(cedolino.total)}")
                                appendLine("Scadenza: ${Formatters.date(cedolino.dueDate)}")
                                appendLine("Stato: ${cedolino.status}")
                            }
                            OutlinedButton(
                                onClick = {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Cedolino ${cedolino.period}")
                                    }
                                    context.startActivity(android.content.Intent.createChooser(intent, "Invia cedolino"))
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Green400)
                            ) {
                                Icon(Icons.Filled.Share, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Condividi", style = MaterialTheme.typography.labelSmall)
                            }
                            // Conferma Invio (solo se non già inviato)
                            if (!cedolino.sentToResident) {
                                Button(
                                    onClick = { showConfirmSendDialog = cedolino },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Cyan500)
                                ) {
                                    Icon(Icons.Filled.Send, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Invia", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            // Segna Pagato → apre dialog con scelta metodo
                            if (cedolino.status != "Pagato") {
                                Button(
                                    onClick = { showPagamentoDialog = cedolino },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Green500)
                                ) {
                                    Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Pagato", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(100.dp)) }
            }
        }

        // FAB menu con tre opzioni
        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // FAB: addebita quota diretta a singola unità
            SmallFloatingActionButton(
                onClick = { showQuotaDialog = true },
                containerColor = Amber400.copy(alpha = 0.9f),
                contentColor = DarkBg
            ) {
                Icon(Icons.Filled.EuroSymbol, "Addebita quota a unità")
            }
            // FAB secondario: singolo condomino (con millesimi)
            SmallFloatingActionButton(
                onClick = { showSingleDialog = true },
                containerColor = DarkSurface,
                contentColor = Cyan400
            ) {
                Icon(Icons.Filled.PersonAdd, "Cedolino singolo")
            }
            // FAB principale: genera per tutti
            @Suppress("DEPRECATION")
            GradientFab(
                icon = Icons.Filled.NoteAdd,
                contentDescription = "Genera cedolini per tutte le unità",
                onClick = { showGenerateDialog = true }
            )
        }
    }

    // Dialog: cedolino per singolo condomino
    if (showSingleDialog && units.isNotEmpty()) {
        SingleCedolinoDialog(
            units = units,
            onDismiss = { showSingleDialog = false },
            onCreate = { cedolino, items ->
                viewModel.addCedolinoWithItems(cedolino, items)
                showSingleDialog = false
            }
        )
    }

    // Dialog: addebita quota diretta a singola unità
    if (showQuotaDialog && units.isNotEmpty()) {
        QuotaDirectaDialog(
            units = units,
            onDismiss = { showQuotaDialog = false },
            onCreate = { unitId, importo, descrizione, categoria, periodo, dueDate ->
                viewModel.addQuotaDirecta(unitId, importo, descrizione, categoria, periodo, dueDate)
                showQuotaDialog = false
            }
        )
    }

    // Dialog: genera per tutte le unità
    if (showGenerateDialog) {
        GenerateCedoliniDialog(
            onDismiss = { showGenerateDialog = false },
            onGenerate = { period, dueDate ->
                viewModel.generateCedoliniForAllUnits(period, dueDate)
                showGenerateDialog = false
            }
        )
    }

    // Dialog: dettaglio cedolino
    showDetailDialog?.let { cwi ->
        CedolinoDetailDialog(
            cwi = cwi,
            unitName = viewModel.getUnitName(cwi.cedolino.unitId),
            onDismiss = { showDetailDialog = null }
        )
    }

    // Dialog: conferma invio al condomino
    showConfirmSendDialog?.let { cedolino ->
        AlertDialog(
            onDismissRequest = { showConfirmSendDialog = null },
            icon = { Icon(Icons.Filled.Send, null, tint = Cyan400) },
            title = { Text("Conferma Invio") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Stai per inviare il cedolino del periodo \"${cedolino.period}\" a:",
                        style = MaterialTheme.typography.bodyMedium, color = TextSecondary
                    )
                    Text(
                        viewModel.getUnitName(cedolino.unitId),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = Cyan400
                    )
                    Text(
                        "Importo: ${Formatters.currency(cedolino.total)}",
                        style = MaterialTheme.typography.bodyMedium, color = TextPrimary
                    )
                    HorizontalDivider(color = TextMuted.copy(alpha = 0.2f))
                    Text(
                        "Una volta inviato, sarà visibile al condomino nella sua area personale.",
                        style = MaterialTheme.typography.bodySmall, color = TextMuted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.markCedolinoSent(cedolino)
                        showConfirmSendDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan500)
                ) { Text("Conferma Invio") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmSendDialog = null }) { Text("Annulla") }
            },
            containerColor = DarkSurface
        )
    }

    // Dialog: registra pagamento con metodo
    showPagamentoDialog?.let { cedolino ->
        RegistraPagamentoDialog(
            cedolino = cedolino,
            unitName = viewModel.getUnitName(cedolino.unitId),
            onDismiss = { showPagamentoDialog = null },
            onConfirm = { method, reference ->
                viewModel.markCedolinoPaidWithPayment(cedolino, method, reference)
                showPagamentoDialog = null
            }
        )
    }

    // Dialog: elimina
    deleteTarget?.let { cedolino ->
        ConfirmDeleteDialog(
            itemName = "Cedolino ${cedolino.period} - ${viewModel.getUnitName(cedolino.unitId)}",
            onConfirm = { viewModel.deleteCedolino(cedolino); deleteTarget = null },
            onDismiss = { deleteTarget = null }
        )
    }
}

// ─── Dialog: Cedolino per singolo condomino ──────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleCedolinoDialog(
    units: List<CondoUnit>,
    onDismiss: () -> Unit,
    onCreate: (Cedolino, List<CedolinoItem>) -> Unit
) {
    var selectedUnit by remember { mutableStateOf(units.first()) }
    var unitExpanded by remember { mutableStateOf(false) }
    var period by remember { mutableStateOf("") }
    // Voci di spesa
    var items by remember { mutableStateOf(listOf(Pair("", ""))) }  // description to amount
    val cal = Calendar.getInstance().apply { add(Calendar.MONTH, 1) }
    val dueDate = cal.timeInMillis

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuovo Cedolino — Singolo Condomino") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.heightIn(max = 480.dp)
            ) {
                // Selezione unità
                item {
                    ExposedDropdownMenuBox(expanded = unitExpanded, onExpandedChange = { unitExpanded = it }) {
                        OutlinedTextField(
                            value = "Int. ${selectedUnit.number} - ${selectedUnit.ownerName}",
                            onValueChange = {}, readOnly = true,
                            label = { Text("Unità / Condomino") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(unitExpanded) }
                        )
                        ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                            units.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text("Int. ${u.number} - ${u.ownerName}") },
                                    onClick = { selectedUnit = u; unitExpanded = false }
                                )
                            }
                        }
                    }
                }
                // Periodo
                item {
                    OutlinedTextField(
                        value = period, onValueChange = { period = it },
                        label = { Text("Periodo (es. II Trimestre 2026)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                }
                // Scadenza (fissa a +1 mese, mostrata)
                item {
                    Text(
                        "Scadenza: ${Formatters.date(dueDate)}",
                        style = MaterialTheme.typography.bodySmall, color = TextMuted
                    )
                }
                // Voci di spesa
                item {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Voci di spesa",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = TextSecondary, modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { items = items + Pair("", "") }) {
                            Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Aggiungi")
                        }
                    }
                }
                itemsIndexed(items) { idx, item ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = item.first,
                            onValueChange = { desc ->
                                items = items.toMutableList().also { it[idx] = it[idx].copy(first = desc) }
                            },
                            label = { Text("Descrizione") },
                            modifier = Modifier.weight(2f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = item.second,
                            onValueChange = { amt ->
                                items = items.toMutableList().also { it[idx] = it[idx].copy(second = amt) }
                            },
                            label = { Text("€") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        if (items.size > 1) {
                            IconButton(
                                onClick = { items = items.toMutableList().also { it.removeAt(idx) } },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.Delete, null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                // Totale calcolato
                item {
                    val total = items.sumOf { it.second.toDoubleOrNull() ?: 0.0 }
                    if (total > 0) {
                        HorizontalDivider(color = Cyan400.copy(alpha = 0.3f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Totale", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                            Text(Formatters.currency(total), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Cyan400)
                        }
                    }
                }
            }
        },
        confirmButton = {
            val total = items.sumOf { it.second.toDoubleOrNull() ?: 0.0 }
            val isValid = period.isNotBlank() && total > 0 && items.all { it.first.isNotBlank() && (it.second.toDoubleOrNull() ?: 0.0) > 0 }
            Button(
                onClick = {
                    val cedolino = Cedolino(
                        unitId = selectedUnit.id,
                        period = period,
                        issueDate = System.currentTimeMillis(),
                        dueDate = dueDate,
                        total = total,
                        status = "Emesso"
                    )
                    val cedItems = items.map { CedolinoItem(cedolinoId = 0, description = it.first, amount = it.second.toDoubleOrNull() ?: 0.0) }
                    onCreate(cedolino, cedItems)
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = Cyan500)
            ) { Text("Crea Cedolino") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
        containerColor = DarkSurface
    )
}

// ─── Dialog: Genera per tutte le unità ──────────────────────────────
@Composable
private fun GenerateCedoliniDialog(onDismiss: () -> Unit, onGenerate: (String, Long) -> Unit) {
    var period by remember { mutableStateOf("") }
    val cal = Calendar.getInstance().apply { add(Calendar.MONTH, 1) }
    val dueDate = cal.timeInMillis

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Genera Cedolini — Tutte le Unità") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Verranno generati cedolini per tutte le unità con ripartizione millesimale automatica sulle spese registrate.",
                    style = MaterialTheme.typography.bodyMedium, color = TextSecondary
                )
                OutlinedTextField(
                    value = period, onValueChange = { period = it },
                    label = { Text("Periodo (es. II Trimestre 2026)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Text("Scadenza: ${Formatters.date(dueDate)}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        },
        confirmButton = {
            TextButton(onClick = { onGenerate(period, dueDate) }, enabled = period.isNotBlank()) { Text("Genera") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
        containerColor = DarkSurface
    )
}

// ─── Dialog: Dettaglio cedolino ──────────────────────────────────────
@Composable
private fun CedolinoDetailDialog(cwi: CedolinoWithItems, unitName: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cedolino — ${cwi.cedolino.period}") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item {
                    Text(unitName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = Cyan400)
                    Spacer(Modifier.height(4.dp))
                    Row { Text("Emesso: ", color = TextMuted, style = MaterialTheme.typography.bodySmall); Text(Formatters.date(cwi.cedolino.issueDate), style = MaterialTheme.typography.bodySmall, color = TextPrimary) }
                    Row { Text("Scadenza: ", color = TextMuted, style = MaterialTheme.typography.bodySmall); Text(Formatters.date(cwi.cedolino.dueDate), style = MaterialTheme.typography.bodySmall, color = TextPrimary) }
                    if (cwi.cedolino.sentToResident && cwi.cedolino.sentAt != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Send, null, tint = Green400, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Inviato il ${Formatters.date(cwi.cedolino.sentAt)}", color = Green400, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = TextMuted.copy(alpha = 0.3f))
                    Text("Voci di spesa", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                }
                items(cwi.items) { item ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text(item.description, style = MaterialTheme.typography.bodySmall, color = TextPrimary, modifier = Modifier.weight(1f))
                        Text(Formatters.currency(item.amount), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                    }
                }
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Cyan400.copy(alpha = 0.3f))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("TOTALE", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                        Spacer(Modifier.weight(1f))
                        Text(Formatters.currency(cwi.cedolino.total), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Cyan400)
                    }
                    Spacer(Modifier.height(8.dp))
                    StatusBadge(cwi.cedolino.status)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } },
        containerColor = DarkSurface
    )
}

// ─── Dialog: Quota Diretta a Singola Unità ───────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotaDirectaDialog(
    units: List<com.condogest.app.data.model.CondoUnit>,
    onDismiss: () -> Unit,
    onCreate: (unitId: Long, importo: Double, descrizione: String, categoria: String, periodo: String, dueDate: Long) -> Unit
) {
    var selectedUnit by remember { mutableStateOf(units.firstOrNull()) }
    var importo by remember { mutableStateOf("") }
    var descrizione by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf(com.condogest.app.data.model.ExpenseCategories.categories.firstOrNull()?.first ?: "") }
    var periodo by remember {
        val cal = java.util.Calendar.getInstance()
        val mesi = listOf("Gennaio","Febbraio","Marzo","Aprile","Maggio","Giugno",
            "Luglio","Agosto","Settembre","Ottobre","Novembre","Dicembre")
        mutableStateOf("${mesi[cal.get(java.util.Calendar.MONTH)]} ${cal.get(java.util.Calendar.YEAR)}")
    }
    var unitExpanded by remember { mutableStateOf(false) }
    var catExpanded by remember { mutableStateOf(false) }

    // Data scadenza default: fine mese prossimo
    val defaultDue = remember {
        val cal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.MONTH, 1); set(java.util.Calendar.DAY_OF_MONTH, getActualMaximum(java.util.Calendar.DAY_OF_MONTH)) }
        cal.timeInMillis
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = com.condogest.app.ui.theme.DarkSurface,
        icon = { Icon(Icons.Filled.EuroSymbol, null, tint = Amber400) },
        title = { Text("Addebita quota a unità", color = com.condogest.app.ui.theme.TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Selezione unità
                ExposedDropdownMenuBox(expanded = unitExpanded, onExpandedChange = { unitExpanded = it }) {
                    OutlinedTextField(
                        value = selectedUnit?.let { "Int. ${it.number} - ${it.ownerName}" } ?: "Seleziona unità",
                        onValueChange = {}, readOnly = true, label = { Text("Unità") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(unitExpanded) }
                    )
                    ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                        units.sortedBy { it.number }.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text("Int. ${unit.number} — ${unit.ownerName}", color = com.condogest.app.ui.theme.TextPrimary) },
                                onClick = { selectedUnit = unit; unitExpanded = false }
                            )
                        }
                    }
                }
                // Categoria
                ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
                    OutlinedTextField(
                        value = categoria, onValueChange = {}, readOnly = true, label = { Text("Categoria") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catExpanded) }
                    )
                    ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        com.condogest.app.data.model.ExpenseCategories.categories.forEach { (name, icon) ->
                            DropdownMenuItem(
                                text = { Text("$icon  $name", color = com.condogest.app.ui.theme.TextPrimary) },
                                onClick = { categoria = name; catExpanded = false }
                            )
                        }
                    }
                }
                OutlinedTextField(descrizione, { descrizione = it }, label = { Text("Descrizione") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(
                    importo, { importo = it }, label = { Text("Importo (€)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                )
                OutlinedTextField(periodo, { periodo = it }, label = { Text("Periodo (es. Maggio 2026)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val unit = selectedUnit ?: return@Button
                    val imp = importo.toDoubleOrNull() ?: return@Button
                    onCreate(unit.id, imp, descrizione.trim().ifBlank { "Quota" }, categoria, periodo.trim(), defaultDue)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Amber400, contentColor = com.condogest.app.ui.theme.DarkBg),
                enabled = selectedUnit != null && (importo.toDoubleOrNull() ?: 0.0) > 0
            ) { Text("Addebita", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla", color = com.condogest.app.ui.theme.TextSecondary) } }
    )
}

// ─── Dialog: Registra Pagamento con Metodo ───────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistraPagamentoDialog(
    cedolino: com.condogest.app.data.model.Cedolino,
    unitName: String,
    onDismiss: () -> Unit,
    onConfirm: (method: String, reference: String) -> Unit
) {
    var selectedMethod by remember { mutableStateOf(com.condogest.app.data.model.PaymentMethods.methods.first()) }
    var reference by remember { mutableStateOf("") }
    var methodExpanded by remember { mutableStateOf(false) }

    val methodIcons = mapOf(
        "Contanti" to "💵",
        "Bonifico" to "🏦",
        "Bollettino Postale" to "📮",
        "RID / Addebito diretto" to "🔄",
        "Assegno" to "📝"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = com.condogest.app.ui.theme.DarkSurface,
        icon = { Icon(Icons.Filled.CheckCircle, null, tint = Green400) },
        title = { Text("Registra pagamento", color = com.condogest.app.ui.theme.TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Riepilogo cedolino
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Green500.copy(alpha = 0.08f), androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(unitName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = com.condogest.app.ui.theme.TextPrimary)
                        Text("Periodo: ${cedolino.period}", style = MaterialTheme.typography.bodySmall, color = com.condogest.app.ui.theme.TextSecondary)
                        Text(
                            Formatters.currency(cedolino.total),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = Green400
                        )
                    }
                }
                // Metodo di pagamento
                ExposedDropdownMenuBox(expanded = methodExpanded, onExpandedChange = { methodExpanded = it }) {
                    OutlinedTextField(
                        value = "${methodIcons[selectedMethod] ?: "💳"} $selectedMethod",
                        onValueChange = {}, readOnly = true, label = { Text("Metodo di pagamento") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(methodExpanded) }
                    )
                    ExposedDropdownMenu(expanded = methodExpanded, onDismissRequest = { methodExpanded = false }) {
                        com.condogest.app.data.model.PaymentMethods.methods.forEach { method ->
                            DropdownMenuItem(
                                text = { Text("${methodIcons[method] ?: "💳"} $method", color = com.condogest.app.ui.theme.TextPrimary) },
                                onClick = { selectedMethod = method; methodExpanded = false }
                            )
                        }
                    }
                }
                // Riferimento (numero bollettino, CRO bonifico, ecc.)
                OutlinedTextField(
                    reference, { reference = it },
                    label = { Text("Riferimento (opzionale)") },
                    placeholder = { Text("es. CRO, n° bollettino...", color = com.condogest.app.ui.theme.TextMuted) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedMethod, reference.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = Green500)
            ) {
                Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Conferma pagamento", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla", color = com.condogest.app.ui.theme.TextSecondary) } }
    )
}
