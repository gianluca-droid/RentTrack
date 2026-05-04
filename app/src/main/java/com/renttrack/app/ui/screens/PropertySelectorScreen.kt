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
import com.renttrack.app.ui.components.condoTextFieldColors
import com.renttrack.app.ui.theme.*
import com.renttrack.app.viewmodel.RentViewModel

private val condoGradients = listOf(
    listOf(Color(0xFF6C63FF), Color(0xFF3DDC84)),
    listOf(Color(0xFF54A0FF), Color(0xFF00C896)),
    listOf(Color(0xFFFF9F43), Color(0xFFFF6B9D)),
    listOf(Color(0xFFA29BFE), Color(0xFF54A0FF)),
    listOf(Color(0xFF00C896), Color(0xFF6C63FF)),
    listOf(Color(0xFFFF6B6B), Color(0xFFFF9F43))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertySelectorScreen(
    viewModel: RentViewModel,
    onCondominioSelected: (Long) -> Unit,
    onResidentAccess: () -> Unit = {}
) {
    val condomini by viewModel.allCondomini.collectAsState()
    val units by viewModel.units.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var condominioToEdit by remember { mutableStateOf<Condominio?>(null) }
    var condominioToDelete by remember { mutableStateOf<Condominio?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(DarkBg)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Hero Header ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF1A1A2E), DarkBg))
                    )
                    .padding(horizontal = 24.dp, vertical = 32.dp)
            ) {
                Column {
                    Text("🏢", fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "I tuoi Condomini",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold, color = TextPrimary
                        )
                    )
                    Text(
                        "${condomini.size} condomini${if (condomini.size != 1) "" else "o"} gestito${if (condomini.size != 1) "i" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }

            HorizontalDivider(color = DarkSurface)

            if (condomini.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("🏗️", fontSize = 56.sp)
                        Text("Nessun condominio ancora", color = TextMuted,
                            style = MaterialTheme.typography.bodyLarge)
                        Button(
                            onClick = { showAddSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = DarkBg)
                        ) {
                            Icon(Icons.Filled.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Crea il primo condominio", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(condomini) { index, condo ->
                        // Calcola stats per questa card
                        val condoUnits = viewModel.allCondomini.collectAsState().value
                        val unitCount = units.count()   // units è già filtered per activeCondominio, usiamo allUnits
                        // Otteniamo le info dai flow globali: per semplicità usiamo conteggi da cedolini in memoria
                        val pendingForThis = 0   // placeholder — i dati vengono caricati solo per il condominio attivo
                        CondominioCard(
                            condominio = condo,
                            gradient = condoGradients[index % condoGradients.size],
                            onClick = { onCondominioSelected(condo.id) },
                            onEdit = { condominioToEdit = condo },
                            onDelete = { condominioToDelete = condo }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddSheet = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = Cyan400, contentColor = DarkBg
        ) { Icon(Icons.Filled.Add, "Aggiungi condominio") }

        // Accesso condomino (area riservata mock)
        OutlinedButton(
            onClick = onResidentAccess,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Purple400),
            border = androidx.compose.foundation.BorderStroke(1.dp, Purple400.copy(alpha = 0.5f))
        ) {
            Icon(Icons.Filled.Person, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Area Condomino", style = MaterialTheme.typography.labelMedium)
        }
    }

    if (showAddSheet) {
        AddCondominioSheet(
            condominio = null,
            onDismiss = { showAddSheet = false },
            onConfirm = { nome, indirizzo, citta, cf, note ->
                viewModel.addCondominio(
                    Condominio(nome = nome, indirizzo = indirizzo, citta = citta, cf = cf, note = note),
                    andSelect = false
                )
                showAddSheet = false
            }
        )
    }

    condominioToEdit?.let { condo ->
        AddCondominioSheet(
            condominio = condo,
            onDismiss = { condominioToEdit = null },
            onConfirm = { nome, indirizzo, citta, cf, note ->
                viewModel.updateCondominio(condo.copy(nome = nome, indirizzo = indirizzo, citta = citta, cf = cf, note = note))
                condominioToEdit = null
            }
        )
    }

    condominioToDelete?.let { condo ->
        AlertDialog(
            onDismissRequest = { condominioToDelete = null },
            containerColor = DarkSurface,
            icon = { Icon(Icons.Filled.DeleteForever, null, tint = Color(0xFFFF6B6B)) },
            title = { Text("Elimina condominio", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text("Eliminare \"${condo.nome}\"?\nTutti i dati (unità, spese, documenti) verranno eliminati definitivamente.",
                    color = TextSecondary)
            },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteCondominio(condo); condominioToDelete = null }) {
                    Text("Elimina", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { condominioToDelete = null }) { Text("Annulla", color = TextSecondary) }
            }
        )
    }
}

@Composable
fun CondominioCard(
    condominio: Condominio,
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
                    .height(6.dp)
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
                // Icona
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(Brush.linearGradient(gradient.map { it.copy(alpha = 0.2f) }), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Apartment, null, tint = gradient[0], modifier = Modifier.size(28.dp))
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        condominio.nome,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold, color = TextPrimary
                        ),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${condominio.indirizzo}, ${condominio.citta}",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    if (condominio.cf.isNotBlank()) {
                        Text("CF: ${condominio.cf}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Edit, "Modifica", tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Delete, "Elimina", tint = Color(0xFFFF6B6B).copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Footer con bottone "Entra"
            HorizontalDivider(color = gradient[0].copy(alpha = 0.15f))
            TextButton(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    "Entra nel condominio",
                    color = gradient[0],
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Filled.ArrowForward, null, tint = gradient[0], modifier = Modifier.size(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCondominioSheet(
    condominio: Condominio?,
    onDismiss: () -> Unit,
    onConfirm: (nome: String, indirizzo: String, citta: String, cf: String, note: String) -> Unit
) {
    var nome by remember { mutableStateOf(condominio?.nome ?: "") }
    var indirizzo by remember { mutableStateOf(condominio?.indirizzo ?: "") }
    var citta by remember { mutableStateOf(condominio?.citta ?: "") }
    var cf by remember { mutableStateOf(condominio?.cf ?: "") }
    var note by remember { mutableStateOf(condominio?.note ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                if (condominio == null) "Nuovo Condominio" else "Modifica Condominio",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold, color = TextPrimary
                )
            )
            OutlinedTextField(nome, { nome = it }, label = { Text("Nome condominio *") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, colors = condoTextFieldColors())
            OutlinedTextField(indirizzo, { indirizzo = it }, label = { Text("Indirizzo *") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, colors = condoTextFieldColors())
            OutlinedTextField(citta, { citta = it }, label = { Text("Città *") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, colors = condoTextFieldColors())
            OutlinedTextField(cf, { cf = it }, label = { Text("Codice fiscale condominio") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, colors = condoTextFieldColors())
            OutlinedTextField(note, { note = it }, label = { Text("Note") },
                modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 3, colors = condoTextFieldColors())
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss, modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, TextMuted.copy(alpha = 0.4f))
                ) { Text("Annulla", color = TextSecondary) }
                Button(
                    onClick = { onConfirm(nome.trim(), indirizzo.trim(), citta.trim(), cf.trim(), note.trim()) },
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
