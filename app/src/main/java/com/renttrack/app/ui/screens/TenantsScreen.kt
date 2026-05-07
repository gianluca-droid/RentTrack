package com.renttrack.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renttrack.app.data.model.*
import com.renttrack.app.ui.components.*
import com.renttrack.app.ui.theme.*
import com.renttrack.app.viewmodel.RentViewModel
import java.text.SimpleDateFormat
import java.util.*

private val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN)

@Composable
fun TenantsScreen(viewModel: RentViewModel) {
    val units by viewModel.units.collectAsState()
    val activeCondominioId by viewModel.activeCondominioId.collectAsState()
    val morositaByUnit by viewModel.morositaByUnit.collectAsState()
    val mesiArretratiByUnit by viewModel.mesiArretratiByUnit.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var editingUnit by remember { mutableStateOf<CondoUnit?>(null) }
    var deleteTarget by remember { mutableStateOf<CondoUnit?>(null) }
    var changeTenantUnit by remember { mutableStateOf<CondoUnit?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val tenantHistory by viewModel.tenantHistory.collectAsState()

    val filteredUnits = remember(units, searchQuery) {
        if (searchQuery.isBlank()) units
        else units.filter { u ->
            u.ownerName.contains(searchQuery, ignoreCase = true) ||
            u.number.contains(searchQuery, ignoreCase = true) ||
            u.ownerEmail.contains(searchQuery, ignoreCase = true) ||
            u.ownerPhone.contains(searchQuery, ignoreCase = true)
        }
    }

    val totalMonthlyRent = units.sumOf { it.millesimi }
    val totalMorosity = morositaByUnit.values.sum()
    val moroseCount = morositaByUnit.keys.size

    Box(modifier = Modifier.fillMaxSize()) {
        if (units.isEmpty()) {
            EmptyState("Nessun inquilino registrato", Icons.Filled.Person)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Summary cards
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SummaryCard(
                            title = "Inquilini",
                            value = "${units.size}",
                            icon = Icons.Filled.People,
                            accentColor = Cyan400,
                            subtitle = "${Formatters.currency(totalMonthlyRent)}/mese attesi",
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCard(
                            title = "Morosità",
                            value = if (totalMorosity > 0) Formatters.currency(totalMorosity) else "—",
                            icon = Icons.Filled.Warning,
                            accentColor = if (totalMorosity > 0) Color(0xFFFF6B6B) else Green400,
                            subtitle = if (moroseCount > 0) "$moroseCount inquilini in ritardo" else "Tutto in regola",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Barra di ricerca
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cerca inquilino…", style = MaterialTheme.typography.bodyMedium, color = TextMuted) },
                        leadingIcon = { Icon(Icons.Filled.Search, null, tint = TextMuted) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Close, "Cancella", tint = TextMuted)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Cyan400,
                            unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface,
                            cursorColor = Cyan400
                        )
                    )
                }

                if (filteredUnits.isEmpty() && searchQuery.isNotBlank()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.SearchOff, null, tint = TextMuted, modifier = Modifier.size(40.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Nessun inquilino trovato per \"$searchQuery\"", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                items(filteredUnits, key = { it.id }) { unit ->
                    TenantCard(
                        unit = unit,
                        morosita = morositaByUnit[unit.id] ?: 0.0,
                        mesiArretrati = mesiArretratiByUnit[unit.id] ?: 0,
                        tenantHistory = tenantHistory.filter { it.unitId == unit.id },
                        onEdit = { editingUnit = unit; showDialog = true },
                        onDelete = { deleteTarget = unit },
                        onChangeTenant = { changeTenantUnit = unit },
                        onGeneratePlan = { viewModel.generateMonthlyPaymentPlan(unit) },
                        onDeleteHistory = { viewModel.deleteTenantHistory(it) }
                    )
                }
            }
        }

        GradientFab(
            icon = Icons.Filled.Add,
            contentDescription = "Aggiungi inquilino",
            onClick = { editingUnit = null; showDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }

    if (showDialog) {
        TenantFormDialog(
            unit = editingUnit,
            condominioId = activeCondominioId,
            onDismiss = { showDialog = false; editingUnit = null },
            onSave = { unit ->
                if (editingUnit != null) viewModel.updateUnit(unit) else viewModel.addUnit(unit)
                showDialog = false; editingUnit = null
            }
        )
    }

    deleteTarget?.let { unit ->
        ConfirmDeleteDialog(
            itemName = "${unit.ownerName} — ${unit.number}",
            onConfirm = { viewModel.deleteUnit(unit); deleteTarget = null },
            onDismiss = { deleteTarget = null }
        )
    }

    changeTenantUnit?.let { unit ->
        ChangeTenantDialog(
            unit = unit,
            onDismiss = { changeTenantUnit = null },
            onConfirm = { exitNotes, newName, newEmail, newPhone, newStart, newEnd, newRent ->
                viewModel.changeTenant(
                    unit = unit,
                    exitNotes = exitNotes,
                    newOwnerName = newName,
                    newOwnerEmail = newEmail,
                    newOwnerPhone = newPhone,
                    newLeaseStart = newStart,
                    newLeaseEnd = newEnd,
                    newMonthlyRent = newRent
                )
                changeTenantUnit = null
            }
        )
    }
}

// ─── Card inquilino ───────────────────────────────────────────────────
@Composable
private fun TenantCard(
    unit: CondoUnit,
    morosita: Double,
    mesiArretrati: Int,
    tenantHistory: List<TenantHistory>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onChangeTenant: () -> Unit,
    onGeneratePlan: () -> Unit,
    onDeleteHistory: (TenantHistory) -> Unit
) {
    val canone = unit.millesimi
    val now = System.currentTimeMillis()
    val daysToExpiry = unit.leaseEndDate?.let { ((it - now) / (1000 * 60 * 60 * 24)).toInt() }
    val expiryColor = when {
        daysToExpiry == null -> null
        daysToExpiry < 0    -> Color(0xFFFF6B6B)   // scaduto
        daysToExpiry < 30   -> Color(0xFFFF6B6B)   // rosso < 30gg
        daysToExpiry < 60   -> Amber400             // arancio < 60gg
        else                -> Green400             // verde OK
    }

    ItemCard(onEdit = onEdit, onDelete = onDelete) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar iniziali
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Cyan500.copy(alpha = 0.3f), Purple500.copy(alpha = 0.1f)))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    unit.ownerName.split(" ").take(2).mapNotNull { it.firstOrNull()?.toString() }.joinToString(""),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    color = Cyan400
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(unit.ownerName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                Text(unit.number, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                if (unit.ownerPhone.isNotBlank()) {
                    Text("📞 ${unit.ownerPhone}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
                if (unit.ownerEmail.isNotBlank()) {
                    Text("✉ ${unit.ownerEmail}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (canone > 0) {
                    Text(Formatters.currency(canone), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Green400)
                    Text("/mese", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
                if (unit.areaMq > 0) {
                    Text("${unit.areaMq.toInt()} m²", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
        }

        // Badge morosità
        if (morosita > 0) {
            Spacer(Modifier.height(6.dp))
            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFFF6B6B).copy(alpha = 0.12f)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "$mesiArretrati ${if (mesiArretrati == 1) "avviso" else "avvisi"} in ritardo — ${Formatters.currency(morosita)} da incassare",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFFFF6B6B)
                    )
                }
            }
        }

        // Badge scadenza contratto
        if (expiryColor != null) {
            Spacer(Modifier.height(4.dp))
            Surface(shape = RoundedCornerShape(6.dp), color = expiryColor.copy(alpha = 0.1f)) {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CalendarMonth, null, tint = expiryColor, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    val label = when {
                        daysToExpiry!! < 0   -> "Contratto scaduto il ${dateFmt.format(Date(unit.leaseEndDate!!))}"
                        daysToExpiry == 0    -> "Contratto scade oggi"
                        daysToExpiry < 30    -> "Contratto scade tra $daysToExpiry giorni"
                        else                 -> "Scadenza: ${dateFmt.format(Date(unit.leaseEndDate!!))}"
                    }
                    Text(label, style = MaterialTheme.typography.labelSmall, color = expiryColor)
                }
            }
        }

        // Bottone "Genera piano pagamenti" se il contratto è configurato
        val canGenerate = unit.leaseStartDate != null && unit.leaseEndDate != null && unit.millesimi > 0
        if (canGenerate) {
            var generated by remember { mutableStateOf(false) }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = { onGeneratePlan(); generated = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan400),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (generated) "Piano generato ✓" else "Genera piano pagamenti",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }

        // Bottone "Cambia Inquilino"
        Spacer(Modifier.height(6.dp))
        OutlinedButton(
            onClick = onChangeTenant,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Amber400),
            border = androidx.compose.foundation.BorderStroke(1.dp, Amber400.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Filled.SwapHoriz, null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("Cambia inquilino", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
        }

        // Sezione storico inquilini collassabile
        if (tenantHistory.isNotEmpty()) {
            var expanded by remember { mutableStateOf(false) }
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = { expanded = !expanded },
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp)
            ) {
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    null, tint = TextMuted, modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "${tenantHistory.size} ex-inquilin${if (tenantHistory.size == 1) "o" else "i"}",
                    style = MaterialTheme.typography.labelSmall, color = TextMuted
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    tenantHistory.forEach { h ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkBg.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(h.ownerName, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = TextSecondary)
                                    val start = h.leaseStartDate?.let { dateFmt.format(Date(it)) } ?: "?"
                                    val end   = h.leaseEndDate?.let { dateFmt.format(Date(it)) } ?: "?"
                                    Text("$start → $end", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    if (h.monthlyRent > 0) Text("${Formatters.currency(h.monthlyRent)}/mese", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    if (h.exitNotes.isNotBlank()) Text("Note: ${h.exitNotes}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                }
                                IconButton(onClick = { onDeleteHistory(h) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Filled.Delete, "Rimuovi", tint = TextMuted.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Form dialog inquilino ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TenantFormDialog(unit: CondoUnit?, condominioId: Long, onDismiss: () -> Unit, onSave: (CondoUnit) -> Unit) {
    var ownerName    by remember { mutableStateOf(unit?.ownerName ?: "") }
    var ownerEmail   by remember { mutableStateOf(unit?.ownerEmail ?: "") }
    var ownerPhone   by remember { mutableStateOf(unit?.ownerPhone ?: "") }
    var number       by remember { mutableStateOf(unit?.number ?: "") }
    var canone       by remember { mutableStateOf(if ((unit?.millesimi ?: 0.0) > 0) unit!!.millesimi.toString() else "") }
    var areaMq       by remember { mutableStateOf(if ((unit?.areaMq ?: 0.0) > 0) unit!!.areaMq.toString() else "") }
    var tipo         by remember { mutableStateOf(unit?.type ?: "Appartamento") }
    var leaseStart   by remember { mutableStateOf(unit?.leaseStartDate) }
    var leaseEnd     by remember { mutableStateOf(unit?.leaseEndDate) }
    var paymentDay   by remember { mutableStateOf((unit?.paymentDayOfMonth ?: 5).toString()) }
    var tipoExpanded by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    // Rileva se l'utente ha modificato qualcosa rispetto allo stato iniziale
    val hasChanges = remember(ownerName, ownerEmail, ownerPhone, number, canone, areaMq, leaseStart, leaseEnd, paymentDay) {
        ownerName != (unit?.ownerName ?: "") ||
        ownerEmail != (unit?.ownerEmail ?: "") ||
        ownerPhone != (unit?.ownerPhone ?: "") ||
        number != (unit?.number ?: "") ||
        canone != (if ((unit?.millesimi ?: 0.0) > 0) unit!!.millesimi.toString() else "") ||
        areaMq != (if ((unit?.areaMq ?: 0.0) > 0) unit!!.areaMq.toString() else "") ||
        leaseStart != unit?.leaseStartDate ||
        leaseEnd != unit?.leaseEndDate ||
        paymentDay != (unit?.paymentDayOfMonth ?: 5).toString()
    }

    // Dialog di conferma abbandono modifiche
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            containerColor = DarkSurface,
            icon = { Icon(Icons.Filled.Warning, null, tint = Amber400) },
            title = { Text("Modifiche non salvate", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Hai inserito dei dati che non sono stati salvati. Vuoi uscire lo stesso?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = { showDiscardDialog = false; onDismiss() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B))
                ) { Text("Esci senza salvare", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Continua a modificare", color = Cyan400)
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = { /* blocca chiusura accidentale — usare Annulla */ },
        containerColor = DarkSurface,
        title = {
            Text(
                if (unit != null) "Modifica inquilino" else "Nuovo inquilino",
                color = TextPrimary, fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                // ── Dati personali ──
                item { Text("Dati inquilino", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = Cyan400) }
                item {
                    OutlinedTextField(
                        value = ownerName, onValueChange = { ownerName = it },
                        label = { Text("Nome e cognome *") },
                        leadingIcon = { Icon(Icons.Filled.Person, null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = ownerEmail, onValueChange = { ownerEmail = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Filled.Email, null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                }
                item {
                    OutlinedTextField(
                        value = ownerPhone, onValueChange = { ownerPhone = it },
                        label = { Text("Telefono") },
                        leadingIcon = { Icon(Icons.Filled.Phone, null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                }

                // ── Appartamento ──
                item { Spacer(Modifier.height(4.dp)) }
                item { Text("Appartamento", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = Cyan400) }
                item {
                    OutlinedTextField(
                        value = number, onValueChange = { number = it },
                        label = { Text("Identificativo (es. Ap. 1, Int. 2)") },
                        leadingIcon = { Icon(Icons.Filled.Home, null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                }
                item {
                    ExposedDropdownMenuBox(expanded = tipoExpanded, onExpandedChange = { tipoExpanded = it }) {
                        OutlinedTextField(
                            value = tipo, onValueChange = {}, readOnly = true,
                            label = { Text("Tipo immobile") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(tipoExpanded) }
                        )
                        ExposedDropdownMenu(expanded = tipoExpanded, onDismissRequest = { tipoExpanded = false }) {
                            UnitTypes.types.forEach { t ->
                                DropdownMenuItem(text = { Text(t, color = TextPrimary) }, onClick = { tipo = t; tipoExpanded = false })
                            }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = canone, onValueChange = { canone = it },
                            label = { Text("Canone €/mese") },
                            modifier = Modifier.weight(1f), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = areaMq, onValueChange = { areaMq = it },
                            label = { Text("m² (opz.)") },
                            modifier = Modifier.weight(1f), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                }

                // ── Contratto ──
                item { Spacer(Modifier.height(4.dp)) }
                item { Text("Contratto", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = Cyan400) }
                item {
                    DatePickerField(
                        label = "Data inizio contratto",
                        dateMillis = leaseStart,
                        onDateSelected = { leaseStart = it }
                    )
                }
                item {
                    DatePickerField(
                        label = "Data fine contratto",
                        dateMillis = leaseEnd,
                        onDateSelected = { leaseEnd = it }
                    )
                }
                item {
                    OutlinedTextField(
                        value = paymentDay,
                        onValueChange = { v -> if (v.length <= 2 && (v.toIntOrNull() ?: 0) <= 28) paymentDay = v },
                        label = { Text("Giorno scadenza pagamento") },
                        placeholder = { Text("es. 5", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Filled.CalendarMonth, null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                        supportingText = { Text("Il ${paymentDay.ifBlank { "5" }} di ogni mese", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(CondoUnit(
                        id           = unit?.id ?: 0,
                        condominioId = unit?.condominioId ?: condominioId,
                        number       = number.ifBlank { "Ap. 1" },
                        floor        = 0,
                        type         = tipo,
                        areaMq       = areaMq.toDoubleOrNull() ?: 0.0,
                        millesimi    = canone.toDoubleOrNull() ?: 0.0,
                        ownerName    = ownerName,
                        ownerEmail   = ownerEmail,
                        ownerPhone   = ownerPhone,
                        scala        = "",
                        leaseStartDate     = leaseStart,
                        leaseEndDate       = leaseEnd,
                        paymentDayOfMonth  = paymentDay.toIntOrNull()?.coerceIn(1, 28) ?: 5
                    ))
                },
                enabled = ownerName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan500)
            ) { Text("Salva", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla", color = TextSecondary) } }
    )
}

// ─── DatePicker field riutilizzabile ─────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(label: String, dateMillis: Long?, onDateSelected: (Long?) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val dateStr = dateMillis?.let { dateFmt.format(Date(it)) } ?: ""

    OutlinedTextField(
        value = dateStr,
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        trailingIcon = {
            Row {
                if (dateMillis != null) {
                    IconButton(onClick = { onDateSelected(null) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Clear, "Cancella", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
                IconButton(onClick = { showPicker = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.CalendarMonth, "Seleziona data", tint = Cyan400, modifier = Modifier.size(18.dp))
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Cyan400, unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
            focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface
        )
    )

    if (showPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onDateSelected(pickerState.selectedDateMillis)
                    showPicker = false
                }) { Text("OK", color = Cyan400) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Annulla", color = TextSecondary) }
            }
        ) {
            DatePicker(
                state = pickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = DarkSurface,
                    selectedDayContainerColor = Cyan400,
                    todayDateBorderColor = Cyan400
                )
            )
        }
    }
}

// ─── Dialog Cambia Inquilino ───────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangeTenantDialog(
    unit: CondoUnit,
    onDismiss: () -> Unit,
    onConfirm: (exitNotes: String, newName: String, newEmail: String, newPhone: String,
                newLeaseStart: Long?, newLeaseEnd: Long?, newRent: Double) -> Unit
) {
    var exitNotes  by remember { mutableStateOf("") }
    var newName    by remember { mutableStateOf("") }
    var newEmail   by remember { mutableStateOf("") }
    var newPhone   by remember { mutableStateOf("") }
    var newRent    by remember { mutableStateOf(unit.millesimi.toString()) }
    var newStart   by remember { mutableStateOf<Long?>(null) }
    var newEnd     by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        icon = { Icon(Icons.Filled.SwapHoriz, null, tint = Amber400) },
        title = {
            Column {
                Text("Cambia inquilino", color = TextPrimary, fontWeight = FontWeight.Bold)
                Text("Int. ${unit.number}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // ── Uscita inquilino attuale ──
                item {
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFF6B6B).copy(alpha = 0.08f)) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Inquilino uscente", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFFFF6B6B))
                            Text(unit.ownerName, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            if (unit.millesimi > 0) Text("Canone: ${Formatters.currency(unit.millesimi)}/mese", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = exitNotes, onValueChange = { exitNotes = it },
                        label = { Text("Note uscita (caparra, danni, ecc.)") },
                        leadingIcon = { Icon(Icons.Filled.Notes, null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth(), maxLines = 3,
                        colors = condoTextFieldColors()
                    )
                }

                // ── Nuovo inquilino ──
                item { Spacer(Modifier.height(4.dp)) }
                item {
                    Text("Nuovo inquilino *", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = Cyan400)
                }
                item {
                    OutlinedTextField(
                        value = newName, onValueChange = { newName = it },
                        label = { Text("Nome e cognome *") },
                        leadingIcon = { Icon(Icons.Filled.Person, null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        colors = condoTextFieldColors()
                    )
                }
                item {
                    OutlinedTextField(
                        value = newEmail, onValueChange = { newEmail = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Filled.Email, null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = condoTextFieldColors()
                    )
                }
                item {
                    OutlinedTextField(
                        value = newPhone, onValueChange = { newPhone = it },
                        label = { Text("Telefono") },
                        leadingIcon = { Icon(Icons.Filled.Phone, null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = condoTextFieldColors()
                    )
                }
                item {
                    OutlinedTextField(
                        value = newRent, onValueChange = { newRent = it },
                        label = { Text("Canone mensile €") },
                        leadingIcon = { Icon(Icons.Filled.EuroSymbol, null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = condoTextFieldColors()
                    )
                }
                item { DatePickerField("Inizio contratto nuovo", newStart) { newStart = it } }
                item { DatePickerField("Fine contratto nuovo", newEnd) { newEnd = it } }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        exitNotes, newName, newEmail, newPhone,
                        newStart, newEnd, newRent.toDoubleOrNull() ?: unit.millesimi
                    )
                },
                enabled = newName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Amber400)
            ) { Text("Conferma cambio", fontWeight = FontWeight.Bold, color = DarkBg) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla", color = TextSecondary) } }
    )
}
