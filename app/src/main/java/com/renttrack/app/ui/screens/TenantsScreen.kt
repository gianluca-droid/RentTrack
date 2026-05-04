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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renttrack.app.data.model.*
import com.renttrack.app.ui.components.*
import com.renttrack.app.ui.theme.*
import com.renttrack.app.viewmodel.RentViewModel

@Composable
fun TenantsScreen(viewModel: RentViewModel) {
    val units by viewModel.units.collectAsState()
    val activeCondominioId by viewModel.activeCondominioId.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingUnit by remember { mutableStateOf<CondoUnit?>(null) }
    var deleteTarget by remember { mutableStateOf<CondoUnit?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val filteredUnits = remember(units, searchQuery) {
        if (searchQuery.isBlank()) units
        else units.filter { u ->
            u.ownerName.contains(searchQuery, ignoreCase = true) ||
            u.number.contains(searchQuery, ignoreCase = true) ||
            u.ownerEmail.contains(searchQuery, ignoreCase = true) ||
            u.ownerPhone.contains(searchQuery, ignoreCase = true)
        }
    }

    // Canone totale mensile = somma dei "millesimi" (riutilizzato come canone)
    val totalMonthlyRent = units.sumOf { it.millesimi }

    Box(modifier = Modifier.fillMaxSize()) {
        if (units.isEmpty()) {
            EmptyState("Nessun inquilino registrato", Icons.Filled.Person)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Summary card
                item {
                    SummaryCard(
                        title = "Inquilini attivi",
                        value = "${units.size}",
                        icon = Icons.Filled.People,
                        accentColor = Cyan400,
                        subtitle = "Entrata mensile totale: ${Formatters.currency(totalMonthlyRent)}"
                    )
                }

                // Barra di ricerca
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cerca per nome, email o telefono…", style = MaterialTheme.typography.bodyMedium, color = TextMuted) },
                        leadingIcon = { Icon(Icons.Filled.Search, null, tint = TextMuted) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Close, "Cancella ricerca", tint = TextMuted)
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
                        onEdit = { editingUnit = unit; showDialog = true },
                        onDelete = { deleteTarget = unit }
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
}

// ─── Card inquilino ───────────────────────────────────────────────────
@Composable
private fun TenantCard(unit: CondoUnit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val hasCanonemensile = unit.millesimi > 0
    ItemCard(onEdit = onEdit, onDelete = onDelete) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar iniziali
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(Cyan500.copy(alpha = 0.3f), Purple500.copy(alpha = 0.1f)))
                    ),
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
            Column(horizontalAlignment = Alignment.End) {
                if (hasCanonemensile) {
                    Text(
                        Formatters.currency(unit.millesimi),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Green400
                    )
                    Text("/mese", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
                if (unit.areaMq > 0) {
                    Text("${unit.areaMq.toInt()} m²", style = MaterialTheme.typography.labelSmall, color = TextMuted)
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
    var number       by remember { mutableStateOf(unit?.number ?: "") }       // es. "Ap. 1", "Int. 2"
    var canone       by remember { mutableStateOf(if ((unit?.millesimi ?: 0.0) > 0) unit!!.millesimi.toString() else "") }
    var areaMq       by remember { mutableStateOf(if ((unit?.areaMq ?: 0.0) > 0) unit!!.areaMq.toString() else "") }
    var tipo         by remember { mutableStateOf(unit?.type ?: "Appartamento") }
    var tipoExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
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
                item {
                    Text("Dati inquilino", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = Cyan400)
                }
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

                // ── Dati appartamento ──
                item { Spacer(Modifier.height(4.dp)) }
                item {
                    Text("Appartamento", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = Cyan400)
                }
                item {
                    OutlinedTextField(
                        value = number, onValueChange = { number = it },
                        label = { Text("Identificativo (es. Ap. 1, Piano 2)") },
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
                            label = { Text("Canone mensile (€)") },
                            modifier = Modifier.weight(1f), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = areaMq, onValueChange = { areaMq = it },
                            label = { Text("m² (opzionale)") },
                            modifier = Modifier.weight(1f), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
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
                        millesimi    = canone.toDoubleOrNull() ?: 0.0,  // canone mensile salvato qui
                        ownerName    = ownerName,
                        ownerEmail   = ownerEmail,
                        ownerPhone   = ownerPhone,
                        scala        = ""
                    ))
                },
                enabled = ownerName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan500)
            ) { Text("Salva", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla", color = TextSecondary) } }
    )
}
