package com.renttrack.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.renttrack.app.ui.theme.*
import com.renttrack.app.ui.components.Formatters
import com.renttrack.app.viewmodel.SupabaseRentViewModel
import java.text.SimpleDateFormat
import java.util.*

// ─── Tipi di risultato ────────────────────────────────────────────────────────
sealed class SearchResult {
    data class Tenant(val unitId: String, val name: String, val email: String, val phone: String) : SearchResult()
    data class Expense(val id: String, val description: String, val category: String, val amount: Double, val date: Long) : SearchResult()
    data class Cedolino(val id: String, val period: String, val status: String, val total: Double, val tenantName: String) : SearchResult()
    data class Documento(val id: String, val titolo: String, val categoria: String, val fileName: String) : SearchResult()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: SupabaseRentViewModel, onBack: () -> Unit) {
    val units      by viewModel.units.collectAsState()
    val expenses   by viewModel.expenses.collectAsState()
    val cedolini   by viewModel.cedoliniWithItems.collectAsState()
    val documenti  by viewModel.documenti.collectAsState()

    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val dateFmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN) }

    // ── Calcola risultati in tempo reale ────────────────────────────────
    val results = remember(query, units, expenses, cedolini, documenti) {
        if (query.length < 2) return@remember emptyList<SearchResult>()
        val q = query.trim().lowercase()
        val list = mutableListOf<SearchResult>()

        // Inquilini
        units.filter {
            it.ownerName.lowercase().contains(q) ||
            it.ownerEmail.lowercase().contains(q) ||
            it.ownerPhone.contains(q) ||
            it.number.lowercase().contains(q)
        }.forEach {
            list += SearchResult.Tenant(it.id, it.ownerName, it.ownerEmail, it.ownerPhone)
        }

        // Spese
        expenses.filter {
            it.description.lowercase().contains(q) ||
            it.category.lowercase().contains(q) ||
            it.notes.lowercase().contains(q)
        }.forEach {
            list += SearchResult.Expense(it.id, it.description, it.category, it.amount, it.date)
        }

        // Cedolini
        cedolini.filter {
            it.cedolino.period.lowercase().contains(q) ||
            it.cedolino.status.lowercase().contains(q)
        }.forEach { cwi ->
            val tenant = units.find { it.id == cwi.cedolino.unitId }?.ownerName ?: ""
            list += SearchResult.Cedolino(cwi.cedolino.id, cwi.cedolino.period, cwi.cedolino.status, cwi.cedolino.total, tenant)
        }

        // Documenti
        documenti.filter {
            it.titolo.lowercase().contains(q) ||
            it.categoria.lowercase().contains(q) ||
            it.note.lowercase().contains(q) ||
            it.fileName.lowercase().contains(q)
        }.forEach {
            list += SearchResult.Documento(it.id, it.titolo, it.categoria, it.fileName)
        }

        list
    }

    // Richiede focus automaticamente all'apertura
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Search bar con back ──────────────────────────────────────────
        Surface(
            color  = DarkSurface,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "Indietro", tint = TextMuted)
                }
                OutlinedTextField(
                    value         = query,
                    onValueChange = { query = it },
                    placeholder   = { Text("Cerca inquilini, spese, cedolini…", color = TextMuted) },
                    leadingIcon   = { Icon(Icons.Filled.Search, null, tint = TextMuted) },
                    trailingIcon  = {
                        AnimatedVisibility(visible = query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Clear, "Cancella", tint = TextMuted)
                            }
                        }
                    },
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier      = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    shape  = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Cyan400,
                        unfocusedBorderColor = Color(0xFF2D3748),
                        focusedTextColor     = TextPrimary,
                        unfocusedTextColor   = TextPrimary,
                        cursorColor          = Cyan400
                    )
                )
            }
        }

        // ── Corpo risultati ──────────────────────────────────────────────
        when {
            // Stato iniziale
            query.length < 2 -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.ManageSearch, null, tint = TextMuted.copy(alpha = 0.5f), modifier = Modifier.size(56.dp))
                        Text("Digita almeno 2 caratteri per cercare", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Inquilini · Spese · Affitti · Documenti",
                            color = TextMuted.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            // Nessun risultato
            results.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.SearchOff, null, tint = TextMuted.copy(alpha = 0.5f), modifier = Modifier.size(56.dp))
                        Text("Nessun risultato per \"$query\"", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Risultati
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            "${results.size} risultati per \"$query\"",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    items(results) { result ->
                        when (result) {
                            is SearchResult.Tenant   -> SearchResultCard(
                                icon      = Icons.Filled.Person,
                                iconColor = Cyan400,
                                tag       = "Inquilino",
                                title     = highlightQuery(result.name, query),
                                lines     = listOfNotNull(
                                    result.email.ifBlank { null },
                                    result.phone.ifBlank { null }
                                )
                            )
                            is SearchResult.Expense  -> SearchResultCard(
                                icon      = Icons.Filled.Receipt,
                                iconColor = Red400,
                                tag       = result.category,
                                title     = highlightQuery(result.description, query),
                                lines     = listOf(
                                    "${Formatters.currency(result.amount)}  ·  ${dateFmt.format(Date(result.date))}"
                                )
                            )
                            is SearchResult.Cedolino -> SearchResultCard(
                                icon      = Icons.Filled.EuroSymbol,
                                iconColor = Amber400,
                                tag       = result.status,
                                title     = highlightQuery(result.period, query),
                                lines     = listOfNotNull(
                                    result.tenantName.ifBlank { null },
                                    Formatters.currency(result.total)
                                )
                            )
                            is SearchResult.Documento -> SearchResultCard(
                                icon      = Icons.Filled.Folder,
                                iconColor = Green400,
                                tag       = result.categoria,
                                title     = highlightQuery(result.titolo, query),
                                lines     = listOf(result.fileName)
                            )
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

// ─── Card singolo risultato ───────────────────────────────────────────────────
@Composable
private fun SearchResultCard(
    icon: ImageVector,
    iconColor: Color,
    tag: String,
    title: AnnotatedString,
    lines: List<String>
) {
    Surface(
        shape  = RoundedCornerShape(14.dp),
        color  = DarkCard,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconColor.copy(alpha = 0.12f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = iconColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        tag,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = iconColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
                Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                lines.forEach { line ->
                    Text(line, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
            }
        }
    }
}

// ─── Highlight testo cercato ──────────────────────────────────────────────────
private fun highlightQuery(text: String, query: String): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    val q = query.trim().lowercase()
    return buildAnnotatedString {
        var cursor = 0
        val lower = text.lowercase()
        while (cursor < text.length) {
            val idx = lower.indexOf(q, cursor)
            if (idx < 0) {
                append(text.substring(cursor))
                break
            }
            append(text.substring(cursor, idx))
            withStyle(SpanStyle(color = Cyan400, fontWeight = FontWeight.Bold, background = Cyan400.copy(alpha = 0.12f))) {
                append(text.substring(idx, idx + q.length))
            }
            cursor = idx + q.length
        }
    }
}
