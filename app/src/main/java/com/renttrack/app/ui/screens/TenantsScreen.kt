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
    val collapsedScales by viewModel.collapsedScales.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingUnit by remember { mutableStateOf<CondoUnit?>(null) }
    var deleteTarget by remember { mutableStateOf<CondoUnit?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    // UnitÃ  filtrate dalla ricerca
    val filteredUnits = remember(units, searchQuery) {
        if (searchQuery.isBlank()) units
        else units.filter { unit ->
            unit.ownerName.contains(searchQuery, ignoreCase = true) ||
            unit.number.contains(searchQuery, ignoreCase = true) ||
            unit.scala.contains(searchQuery, ignoreCase = true)
        }
    }

    // Raggruppamento per scala, poi ordinamento per piano dentro ogni scala
    // UnitÃ  senza scala vanno in un gruppo "â€”" posto alla fine
    val groupedUnits = remember(filteredUnits) {
        val withScala = filteredUnits
            .filter { it.scala.isNotBlank() }
            .groupBy { it.scala }
            .toSortedMap()
            .mapValues { (_, v) -> v.sortedBy { it.floor } }

        val noScala = filteredUnits
            .filter { it.scala.isBlank() }
            .sortedBy { it.floor }

        // Se tutte le unitÃ  non hanno scala â†’ un solo gruppo senza etichetta
        if (withScala.isEmpty()) {
            if (noScala.isEmpty()) emptyMap() else mapOf("" to noScala)
        } else {
            val result = mutableMapOf<String, List<CondoUnit>>()
            result.putAll(withScala)
            if (noScala.isNotEmpty()) result["â€”"] = noScala
            result
        }
    }

    // Stato di espansione dal ViewModel (persiste tra navigazioni)

    val totalMillesimi = units.sumOf { it.millesimi }

    Box(modifier = Modifier.fillMaxSize()) {
        if (units.isEmpty()) {
            EmptyState("Nessuna unitÃ  registrata", Icons.Filled.Apartment)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Summary card
                item {
                    SummaryCard(
                        title = "Totale UnitÃ ",
                        value = "${units.size}",
                        icon = Icons.Filled.Apartment,
                        accentColor = Cyan400,
                        subtitle = "Millesimi: ${totalMillesimi.toInt()}/1000 Â· ${groupedUnits.keys.count { s -> s.isNotBlank() && s != "â€”" }} scale"
                    )
                }

                // Barra di ricerca
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { newValue -> searchQuery = newValue },
                        placeholder = { Text("Cerca per proprietario, interno o scalaâ€¦", style = MaterialTheme.typography.bodyMedium, color = TextMuted) },
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

                // Nessun risultato
                if (filteredUnits.isEmpty() && searchQuery.isNotBlank()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.SearchOff, null, tint = TextMuted, modifier = Modifier.size(40.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Nessuna unitÃ  trovata per \"$searchQuery\"", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                // Sezioni per scala
                groupedUnits.forEach { (scala, unitsInScala) ->
                    val isExpanded = scala !in collapsedScales
                    val scalaMillesimi = unitsInScala.sumOf { it.millesimi }
                    val hasLabel = scala.isNotBlank() && scala != "â€”" || scala == "â€”"

                    // Header scala (solo se c'Ã¨ piÃ¹ di un gruppo o il gruppo ha un nome)
                    if (groupedUnits.size > 1 || (groupedUnits.size == 1 && hasLabel)) {
                        item(key = "header_$scala") {
                            ScalaHeader(
                                scala = scala,
                                unitCount = unitsInScala.size,
                                millesimi = scalaMillesimi,
                                isExpanded = scala !in collapsedScales,
                                onToggle = { viewModel.toggleScala(scala) }
                            )
                        }
                    }

                    // UnitÃ  della scala (con animazione collasso)
                    if (scala !in collapsedScales) {
                        items(unitsInScala, key = { it.id }) { unit ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                UnitCard(
                                    unit = unit,
                                    showScalaBadge = groupedUnits.size == 1, // mostra scala nella card solo se unico gruppo
                                    onEdit = { editingUnit = unit; showDialog = true },
                                    onDelete = { deleteTarget = unit }
                                )
                            }
                        }
                    }
                }
            }
        }

        GradientFab(
            icon = Icons.Filled.Add,
            contentDescription = "Aggiungi unitÃ ",
            onClick = { editingUnit = null; showDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }

    if (showDialog) {
        UnitFormDialog(
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
            itemName = "Int. ${unit.number} - ${unit.ownerName}",
            onConfirm = { viewModel.deleteUnit(unit); deleteTarget = null },
            onDismiss = { deleteTarget = null }
        )
    }
}

// â”€â”€â”€ Header collassabile per scala â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
@Composable
private fun ScalaHeader(
    scala: String,
    unitCount: Int,
    millesimi: Double,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val displayName = when {
        scala.isBlank() || scala == "â€”" -> "UnitÃ  senza raggruppamento"
        else -> scala  // mostra esattamente ciÃ² che l'admin ha scritto (A, Nord, Corpo B, ecc.)
    }

    Card(
        onClick = onToggle,
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icona scala con lettera
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(Cyan500.copy(alpha = 0.3f), Cyan400.copy(alpha = 0.1f)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (scala.isBlank() || scala == "â€”") "?" else scala.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Cyan400
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    displayName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    "$unitCount ${if (unitCount == 1) "unitÃ " else "unitÃ "} Â· ${millesimi.toInt()} mill.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            Icon(
                if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (isExpanded) "Comprimi" else "Espandi",
                tint = Cyan400
            )
        }
    }
}

// â”€â”€â”€ Card unitÃ  â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
@Composable
private fun UnitCard(
    unit: CondoUnit,
    showScalaBadge: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ItemCard(onEdit = onEdit, onDelete = onDelete) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Piano indicator
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurface),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "P${unit.floor}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = Cyan400
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Int. ${unit.number}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(Modifier.width(6.dp))
                    StatusBadge(unit.type)
                    if (showScalaBadge && unit.scala.isNotBlank()) {
                        Spacer(Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Cyan400.copy(alpha = 0.12f)
                        ) {
                            Text(
                                unit.scala,   // valore libero: A, Nord, Corpo B...
                                style = MaterialTheme.typography.labelSmall,
                                color = Cyan400,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(unit.ownerName, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                if (unit.ownerPhone.isNotBlank()) {
                    Text("ðŸ“ž ${unit.ownerPhone}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${unit.areaMq.toInt()} mÂ²",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Text(
                    "${unit.millesimi.toInt()} mill.",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Purple400
                )
            }
        }
    }
}

// â”€â”€â”€ Form dialog â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitFormDialog(unit: CondoUnit?, condominioId: Long, onDismiss: () -> Unit, onSave: (CondoUnit) -> Unit) {
    var number by remember { mutableStateOf(unit?.number ?: "") }
    var scala by remember { mutableStateOf(unit?.scala ?: "") }
    var floor by remember { mutableStateOf(unit?.floor?.toString() ?: "0") }
    var type by remember { mutableStateOf(unit?.type ?: "Appartamento") }
    var areaMq by remember { mutableStateOf(unit?.areaMq?.toString() ?: "") }
    var millesimi by remember { mutableStateOf(unit?.millesimi?.toString() ?: "") }
    var ownerName by remember { mutableStateOf(unit?.ownerName ?: "") }
    var ownerEmail by remember { mutableStateOf(unit?.ownerEmail ?: "") }
    var ownerPhone by remember { mutableStateOf(unit?.ownerPhone ?: "") }
    var typeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (unit != null) "Modifica UnitÃ " else "Nuova UnitÃ ") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = scala,
                            onValueChange = { scala = it },  // libero: A, B, Nord, Corpo 1, Torre Est...
                            label = { Text("Scala / Blocco / Corpo") },
                            modifier = Modifier.weight(1f), singleLine = true,
                            placeholder = { Text("es. A, Nord, Corpo 1", color = TextMuted) }
                        )
                        OutlinedTextField(
                            value = number, onValueChange = { number = it },
                            label = { Text("Interno") },
                            modifier = Modifier.weight(1f), singleLine = true
                        )
                        OutlinedTextField(
                            value = floor, onValueChange = { floor = it },
                            label = { Text("Piano") },
                            modifier = Modifier.weight(1f), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
                item {
                    ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                        OutlinedTextField(
                            value = type, onValueChange = {}, readOnly = true,
                            label = { Text("Tipo") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) }
                        )
                        ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                            UnitTypes.types.forEach { t ->
                                DropdownMenuItem(text = { Text(t) }, onClick = { type = t; typeExpanded = false })
                            }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = areaMq, onValueChange = { areaMq = it },
                            label = { Text("mÂ²") }, modifier = Modifier.weight(1f), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = millesimi, onValueChange = { millesimi = it },
                            label = { Text("Quota %") }, modifier = Modifier.weight(1f), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = ownerName, onValueChange = { ownerName = it },
                        label = { Text("Proprietario *") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = ownerEmail, onValueChange = { ownerEmail = it },
                        label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                }
                item {
                    OutlinedTextField(
                        value = ownerPhone, onValueChange = { ownerPhone = it },
                        label = { Text("Telefono") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val u = CondoUnit(
                        id = unit?.id ?: 0,
                        condominioId = unit?.condominioId ?: condominioId,
                        number = number, floor = floor.toIntOrNull() ?: 0,
                        type = type, areaMq = areaMq.toDoubleOrNull() ?: 0.0,
                        millesimi = millesimi.toDoubleOrNull() ?: 0.0,
                        ownerName = ownerName, ownerEmail = ownerEmail, ownerPhone = ownerPhone,
                        scala = scala.trim()
                    )
                    onSave(u)
                },
                enabled = number.isNotBlank() && ownerName.isNotBlank()
            ) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
        containerColor = DarkSurface
    )
}


