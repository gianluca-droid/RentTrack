package com.renttrack.app.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.renttrack.app.data.model.*
import com.renttrack.app.ui.components.*
import com.renttrack.app.ui.theme.*
import com.renttrack.app.viewmodel.SupabaseRentViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(viewModel: SupabaseRentViewModel) {
    val expenses           by viewModel.expenses.collectAsState()
    val activeCondominioId by viewModel.activeCondominioId.collectAsState()
    var showDialog    by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<SExpense?>(null) }
    var deleteTarget   by remember { mutableStateOf<SExpense?>(null) }

    // ── Filtri ─────────────────────────────────────────────────────────────
    var filterCategory by remember { mutableStateOf<String?>(null) }   // null = tutte
    var filterYear     by remember { mutableStateOf<Int?>(null) }       // null = tutti

    // Anni disponibili dall'elenco spese
    val availableYears = remember(expenses) {
        val cal = Calendar.getInstance()
        expenses.map { cal.apply { timeInMillis = it.date }.get(Calendar.YEAR) }
            .distinct().sortedDescending()
    }

    // Categorie disponibili dall'elenco spese
    val availableCategories = remember(expenses) {
        expenses.map { it.category }.distinct().sorted()
    }

    // Lista filtrata
    val cal = Calendar.getInstance()
    val filtered = remember(expenses, filterCategory, filterYear) {
        expenses.filter { exp ->
            val catOk  = filterCategory == null || exp.category == filterCategory
            val yearOk = filterYear == null ||
                cal.apply { timeInMillis = exp.date }.get(Calendar.YEAR) == filterYear
            catOk && yearOk
        }.sortedByDescending { it.date }
    }
    val totalFiltered = remember(filtered) { filtered.sumOf { it.amount } }
    val isFiltered = filterCategory != null || filterYear != null

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Card totale ──────────────────────────────────────────────
            item {
                SummaryCard(
                    title = if (isFiltered) "Spese filtrate" else "Totale Spese",
                    value = Formatters.currency(totalFiltered),
                    icon  = Icons.Filled.Receipt,
                    accentColor = Red400,
                    subtitle = "${filtered.size} spese${if (isFiltered) " (filtrate)" else " registrate"}"
                )
                Spacer(Modifier.height(8.dp))
            }

            // ── Barra filtri anno ────────────────────────────────────────
            if (availableYears.size > 1) {
                item {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = filterYear == null,
                            onClick  = { filterYear = null },
                            label    = { Text("Tutti gli anni", style = MaterialTheme.typography.labelSmall) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Cyan400.copy(alpha = 0.18f),
                                selectedLabelColor     = Cyan400
                            )
                        )
                        availableYears.forEach { year ->
                            FilterChip(
                                selected = filterYear == year,
                                onClick  = { filterYear = if (filterYear == year) null else year },
                                label    = { Text("$year", style = MaterialTheme.typography.labelSmall) },
                                colors   = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Cyan400.copy(alpha = 0.18f),
                                    selectedLabelColor     = Cyan400
                                )
                            )
                        }
                    }
                }
            }

            // ── Barra filtri categoria ───────────────────────────────────
            if (availableCategories.size > 1) {
                item {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = filterCategory == null,
                            onClick  = { filterCategory = null },
                            label    = { Text("Tutte", style = MaterialTheme.typography.labelSmall) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Red400.copy(alpha = 0.18f),
                                selectedLabelColor     = Red400
                            )
                        )
                        availableCategories.forEach { cat ->
                            val icon = ExpenseCategories.getIcon(cat)
                            FilterChip(
                                selected = filterCategory == cat,
                                onClick  = { filterCategory = if (filterCategory == cat) null else cat },
                                label    = {
                                    Text(
                                        "$icon $cat",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors   = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Red400.copy(alpha = 0.18f),
                                    selectedLabelColor     = Red400
                                )
                            )
                        }
                    }
                }
            }

            // ── Banner "reset filtri" ────────────────────────────────────
            if (isFiltered) {
                item {
                    AnimatedVisibility(visible = true, enter = fadeIn() + expandVertically()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Cyan400.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.FilterAlt, null, tint = Cyan400, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    buildString {
                                        filterCategory?.let { append(it) }
                                        if (filterCategory != null && filterYear != null) append(" · ")
                                        filterYear?.let { append(it) }
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Cyan400,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = { filterCategory = null; filterYear = null },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text("Rimuovi filtri", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                }
                            }
                        }
                    }
                }
            }

            // ── Lista spese filtrate ─────────────────────────────────────
            if (filtered.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.SearchOff, null, tint = TextMuted, modifier = Modifier.size(40.dp))
                            Text(
                                if (isFiltered) "Nessuna spesa per i filtri selezionati" else "Nessuna spesa registrata",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { expense ->
                    val icon = ExpenseCategories.getIcon(expense.category)
                    ItemCard(
                        onEdit   = { editingExpense = expense; showDialog = true },
                        onDelete = { deleteTarget = expense }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CategoryChip(expense.category, icon)
                            Spacer(Modifier.weight(1f))
                            Text(
                                Formatters.date(expense.date),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(expense.description, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text(
                            Formatters.currency(expense.amount),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Red400
                        )
                        if (expense.notes.isNotBlank()) {
                            Text("📝 ${expense.notes}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                    }
                }
            }
        }

        GradientFab(
            icon = Icons.Filled.Add, contentDescription = "Registra spesa",
            onClick = { editingExpense = null; showDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }

    if (showDialog) {
        ExpenseFormDialog(
            expense      = editingExpense,
            condominioId = activeCondominioId,
            onDismiss    = { showDialog = false; editingExpense = null },
            onSave       = { exp ->
                if (editingExpense != null) viewModel.updateExpense(exp) else viewModel.addExpense(exp)
                showDialog = false; editingExpense = null
            }
        )
    }

    deleteTarget?.let { expense ->
        ConfirmDeleteDialog(
            itemName  = expense.description,
            onConfirm = { viewModel.deleteExpense(expense); deleteTarget = null },
            onDismiss = { deleteTarget = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseFormDialog(
    expense: SExpense?,
    condominioId: String,
    onDismiss: () -> Unit,
    onSave: (SExpense) -> Unit
) {
    var category         by remember { mutableStateOf(expense?.category ?: ExpenseCategories.categories.first().first) }
    var description      by remember { mutableStateOf(expense?.description ?: "") }
    var amount           by remember { mutableStateOf(expense?.amount?.toString() ?: "") }
    var notes            by remember { mutableStateOf(expense?.notes ?: "") }
    var categoryExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { /* blocca chiusura accidentale */ },
        title   = { Text(if (expense != null) "Modifica Spesa" else "Nuova Spesa") },
        text    = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
                    OutlinedTextField(
                        value = category, onValueChange = {}, readOnly = true,
                        label = { Text("Categoria") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) }
                    )
                    ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        ExpenseCategories.categories.forEach { (name, icon) ->
                            DropdownMenuItem(
                                text    = { Text("$icon  $name") },
                                onClick = { category = name; categoryExpanded = false }
                            )
                        }
                    }
                }
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descrizione") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Importo (€)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Note (opzionale)") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(SExpense(
                        id           = expense?.id ?: "",
                        condominioId = expense?.condominioId ?: condominioId,
                        date         = expense?.date ?: System.currentTimeMillis(),
                        category     = category, description = description,
                        amount       = amount.toDoubleOrNull() ?: 0.0, notes = notes
                    ))
                },
                enabled = description.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0
            ) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
        containerColor = DarkSurface
    )
}
