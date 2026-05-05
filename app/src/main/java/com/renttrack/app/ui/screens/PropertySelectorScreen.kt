package com.renttrack.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renttrack.app.data.model.Condominio
import com.renttrack.app.ui.components.Formatters
import com.renttrack.app.ui.components.condoTextFieldColors
import com.renttrack.app.ui.theme.*
import com.renttrack.app.viewmodel.PropertySummary
import com.renttrack.app.viewmodel.RentViewModel

private val propertyGradients = listOf(
    listOf(Color(0xFF00D4FF), Color(0xFF6C63FF)),
    listOf(Color(0xFF10B981), Color(0xFF00D4FF)),
    listOf(Color(0xFFF59E0B), Color(0xFFEC4899)),
    listOf(Color(0xFFA78BFA), Color(0xFF00D4FF)),
    listOf(Color(0xFF34D399), Color(0xFF6C63FF)),
    listOf(Color(0xFFF87171), Color(0xFFF59E0B))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertySelectorScreen(
    viewModel: RentViewModel,
    onCondominioSelected: (Long) -> Unit,
    onResidentAccess: () -> Unit = {}
) {
    val proprieta by viewModel.allCondomini.collectAsState()
    val summaryMap by viewModel.propertySummaryMap.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var toEdit by remember { mutableStateOf<Condominio?>(null) }
    var toDelete by remember { mutableStateOf<Condominio?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(DarkBg)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Hero Header ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xFF0D1B2A), DarkBg)))
                    .padding(horizontal = 24.dp, vertical = 32.dp)
            ) {
                Column {
                    Text("🏠", fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Le mie proprietà",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold, color = TextPrimary
                        )
                    )
                    Text(
                        if (proprieta.isEmpty()) "Aggiungi il primo immobile"
                        else "${proprieta.size} ${if (proprieta.size == 1) "immobile" else "immobili"} in gestione",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }

            HorizontalDivider(color = DarkSurface)

            if (proprieta.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("🏗️", fontSize = 56.sp)
                        Text(
                            "Nessuna proprietà ancora",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "Aggiungi il tuo primo immobile per iniziare",
                            color = TextMuted.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = { showAddSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = DarkBg)
                        ) {
                            Icon(Icons.Filled.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Aggiungi prima proprietà", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(proprieta) { index, prop ->
                        PropertyCard(
                            proprieta = prop,
                            summary   = summaryMap[prop.id],
                            gradient  = propertyGradients[index % propertyGradients.size],
                            onClick   = { onCondominioSelected(prop.id) },
                            onEdit    = { toEdit = prop },
                            onDelete  = { toDelete = prop }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        // FAB — aggiungi proprietà
        FloatingActionButton(
            onClick = { showAddSheet = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = Cyan400, contentColor = DarkBg
        ) { Icon(Icons.Filled.Add, "Aggiungi proprietà") }
        // RIMOSSO: bottone "Area Condomino"
    }

    if (showAddSheet) {
        PropertyFormSheet(
            proprieta = null,
            onDismiss = { showAddSheet = false },
            onConfirm = { nome, indirizzo, citta, note ->
                viewModel.addCondominio(
                    Condominio(nome = nome, indirizzo = indirizzo, citta = citta, note = note),
                    andSelect = false
                )
                showAddSheet = false
            }
        )
    }

    toEdit?.let { prop ->
        PropertyFormSheet(
            proprieta = prop,
            onDismiss = { toEdit = null },
            onConfirm = { nome, indirizzo, citta, note ->
                viewModel.updateCondominio(prop.copy(nome = nome, indirizzo = indirizzo, citta = citta, note = note))
                toEdit = null
            }
        )
    }

    toDelete?.let { prop ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            containerColor = DarkSurface,
            icon = { Icon(Icons.Filled.DeleteForever, null, tint = Red400) },
            title = { Text("Elimina proprietà", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Eliminare \"${prop.nome}\"?\nTutti i dati (inquilini, spese, documenti) verranno eliminati definitivamente.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteCondominio(prop); toDelete = null }) {
                    Text("Elimina", color = Red400, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text("Annulla", color = TextSecondary) }
            }
        )
    }
}

// ─── Card proprietà ──────────────────────────────────────────────────────
@Composable
fun PropertyCard(
    proprieta: Condominio,
    summary: PropertySummary?,
    gradient: List<Color>,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, gradient[0].copy(alpha = 0.3f))
    ) {
        Column {
            // Barra colore superiore
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(Brush.horizontalGradient(gradient))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(Brush.linearGradient(gradient.map { it.copy(alpha = 0.2f) }), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Home, null, tint = gradient[0], modifier = Modifier.size(28.dp))
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        proprieta.nome,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold, color = TextPrimary
                        ),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${proprieta.indirizzo}, ${proprieta.citta}",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    // ── Badge riepilogo ──
                    if (summary != null) {
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            // Inquilini + canone
                            Surface(shape = RoundedCornerShape(6.dp), color = gradient[0].copy(alpha = 0.12f)) {
                                Text(
                                    "👤 ${summary.unitCount} · ${Formatters.currency(summary.totalMonthlyRent)}/mese",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = gradient[0],
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                            // Badge morosità
                            if (summary.totalMorosita > 0) {
                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFFF6B6B).copy(alpha = 0.15f)) {
                                    Text(
                                        "⚠️ ${Formatters.currency(summary.totalMorosita)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFFFF6B6B),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            // Badge scadenze
                            if (summary.expiringContracts > 0) {
                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFF59E0B).copy(alpha = 0.15f)) {
                                    Text(
                                        "📅 ${summary.expiringContracts} scad.",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFFF59E0B),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Edit, "Modifica", tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Delete, "Elimina", tint = Red400.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    }
                }
            }

            HorizontalDivider(color = gradient[0].copy(alpha = 0.12f))
            TextButton(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    "Gestisci →",
                    color = gradient[0],
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}


// ─── Form aggiunta/modifica proprietà ─────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyFormSheet(
    proprieta: Condominio?,
    onDismiss: () -> Unit,
    onConfirm: (nome: String, indirizzo: String, citta: String, note: String) -> Unit
) {
    var nome      by remember { mutableStateOf(proprieta?.nome ?: "") }
    var indirizzo by remember { mutableStateOf(proprieta?.indirizzo ?: "") }
    var citta     by remember { mutableStateOf(proprieta?.citta ?: "") }
    var note      by remember { mutableStateOf(proprieta?.note ?: "") }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = DarkSurface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                if (proprieta == null) "📍 Nuova proprietà" else "✏️ Modifica proprietà",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
            )
            OutlinedTextField(
                nome, { nome = it },
                label = { Text("Nome immobile *  (es. Via Roma 12 - Milano)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                colors = condoTextFieldColors()
            )
            OutlinedTextField(
                indirizzo, { indirizzo = it },
                label = { Text("Indirizzo *") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                colors = condoTextFieldColors()
            )
            OutlinedTextField(
                citta, { citta = it },
                label = { Text("Città *") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                colors = condoTextFieldColors()
            )
            OutlinedTextField(
                note, { note = it },
                label = { Text("Note (es. 2° piano, arredato, ...)") },
                modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 3,
                colors = condoTextFieldColors()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss, modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, TextMuted.copy(alpha = 0.4f))
                ) { Text("Annulla", color = TextSecondary) }
                Button(
                    onClick = { onConfirm(nome.trim(), indirizzo.trim(), citta.trim(), note.trim()) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = DarkBg),
                    enabled = nome.isNotBlank() && indirizzo.isNotBlank() && citta.isNotBlank()
                ) {
                    Icon(Icons.Filled.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Salva", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
