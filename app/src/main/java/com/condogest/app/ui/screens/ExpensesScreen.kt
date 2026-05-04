package com.condogest.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.condogest.app.data.model.*
import com.condogest.app.ui.components.*
import com.condogest.app.ui.theme.*
import com.condogest.app.viewmodel.CondoViewModel

@Composable
fun ExpensesScreen(viewModel: CondoViewModel) {
    val expenses by viewModel.expenses.collectAsState()
    val totalExpenses by viewModel.totalExpenses.collectAsState()
    val activeCondominioId by viewModel.activeCondominioId.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var deleteTarget by remember { mutableStateOf<Expense?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (expenses.isEmpty()) {
            EmptyState("Nessuna spesa registrata", Icons.Filled.Receipt)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    SummaryCard(
                        title = "Totale Spese", value = Formatters.currency(totalExpenses),
                        icon = Icons.Filled.Receipt, accentColor = Red400,
                        subtitle = "${expenses.size} spese registrate"
                    )
                    Spacer(Modifier.height(8.dp))
                }

                items(expenses, key = { it.id }) { expense ->
                    val icon = ExpenseCategories.getIcon(expense.category)
                    ItemCard(
                        onEdit = { editingExpense = expense; showDialog = true },
                        onDelete = { deleteTarget = expense }
                    ) {
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
                        if (expense.notes.isNotBlank()) {
                            Text("📝 ${expense.notes}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
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
            expense = editingExpense,
            condominioId = activeCondominioId,
            onDismiss = { showDialog = false; editingExpense = null },
            onSave = { exp ->
                if (editingExpense != null) viewModel.updateExpense(exp) else viewModel.addExpense(exp)
                showDialog = false; editingExpense = null
            }
        )
    }

    deleteTarget?.let { expense ->
        ConfirmDeleteDialog(
            itemName = expense.description,
            onConfirm = { viewModel.deleteExpense(expense); deleteTarget = null },
            onDismiss = { deleteTarget = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseFormDialog(expense: Expense?, condominioId: Long, onDismiss: () -> Unit, onSave: (Expense) -> Unit) {
    var category by remember { mutableStateOf(expense?.category ?: ExpenseCategories.categories.first().first) }
    var description by remember { mutableStateOf(expense?.description ?: "") }
    var amount by remember { mutableStateOf(expense?.amount?.toString() ?: "") }
    var notes by remember { mutableStateOf(expense?.notes ?: "") }
    var categoryExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (expense != null) "Modifica Spesa" else "Nuova Spesa") },
        text = {
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
                                text = { Text("$icon  $name") },
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
                    val e = Expense(
                        id = expense?.id ?: 0,
                        condominioId = expense?.condominioId ?: condominioId,
                        date = expense?.date ?: System.currentTimeMillis(),
                        category = category, description = description,
                        amount = amount.toDoubleOrNull() ?: 0.0, notes = notes
                    )
                    onSave(e)
                },
                enabled = description.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0
            ) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
        containerColor = DarkSurface
    )
}
