package com.renttrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renttrack.app.data.model.Condominio
import com.renttrack.app.data.model.CondoUnit
import com.renttrack.app.ui.theme.*
import com.renttrack.app.viewmodel.RentViewModel

/**
 * Schermata di accesso lato condomino (modalitÃ  MOCK).
 * L'utente seleziona il condominio e poi la propria unitÃ .
 * In produzione sarÃ  sostituita da un login Supabase.
 */
@Composable
fun TenantLoginScreen(
    viewModel: RentViewModel,
    onLogin: () -> Unit,
    onBackToAdmin: () -> Unit
) {
    val condomini by viewModel.allCondomini.collectAsState()
    var selectedCondo by remember { mutableStateOf<Condominio?>(null) }
    var condoExpanded by remember { mutableStateOf(false) }

    // UnitÃ  del condominio selezionato: dobbiamo caricarle temporaneamente
    // In mock usiamo le unitÃ  giÃ  in memoria (se il condo Ã¨ quello attivo) o la lista globale
    val allUnits by viewModel.units.collectAsState()

    // Se l'utente seleziona un condo diverso da quello attivo, le unitÃ  potrebbero non essere caricate.
    // Per il mock, forziamo la selezione dello stesso condo attivo oppure mostriamo avviso.
    val activeCondoId by viewModel.activeCondominioId.collectAsState()
    val unitsForSelected = remember(selectedCondo, allUnits, activeCondoId) {
        if (selectedCondo?.id == activeCondoId) allUnits
        else allUnits.filter { it.condominioId == selectedCondo?.id }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(24.dp))
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                Brush.radialGradient(listOf(Cyan500.copy(alpha = 0.3f), Color.Transparent)),
                                RoundedCornerShape(20.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Person, null, tint = Cyan400, modifier = Modifier.size(44.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Accesso Condomino",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = TextPrimary
                    )
                    Text(
                        "Seleziona il tuo condominio e appartamento",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                    Spacer(Modifier.height(8.dp))
                    // Badge MOCK
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Amber400.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Science, null, tint = Amber400, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("ModalitÃ  Demo", style = MaterialTheme.typography.labelSmall, color = Amber400)
                        }
                    }
                }
            }

            // Selezione condominio
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "1. Seleziona il condominio",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextSecondary
                        )
                        if (condomini.isEmpty()) {
                            Text("Nessun condominio disponibile", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                        } else {
                            condomini.forEach { condo ->
                                val isSelected = selectedCondo?.id == condo.id
                                OutlinedCard(
                                    onClick = { selectedCondo = condo },
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) Cyan400 else TextMuted.copy(alpha = 0.2f)
                                    ),
                                    colors = CardDefaults.outlinedCardColors(
                                        containerColor = if (isSelected) Cyan400.copy(alpha = 0.08f) else DarkBg
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.Apartment, null,
                                            tint = if (isSelected) Cyan400 else TextMuted,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(condo.nome, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${condo.indirizzo}, ${condo.citta}", style = MaterialTheme.typography.bodySmall, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        if (isSelected) {
                                            Icon(Icons.Filled.CheckCircle, null, tint = Cyan400, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Selezione unitÃ  (visibile solo dopo aver scelto il condominio)
            if (selectedCondo != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "2. Seleziona il tuo appartamento",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextSecondary
                            )
                            if (unitsForSelected.isEmpty()) {
                                Text(
                                    "Nessuna unitÃ  trovata.\nAssicurati che il condominio selezionato sia quello attivo nell'app amministratore.",
                                    color = TextMuted, style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                val unitGroups = unitsForSelected
                                    .groupBy { if (it.scala.isNotBlank()) "Scala ${it.scala}" else "UnitÃ " }
                                    .toSortedMap()

                                unitGroups.forEach { (scalaLabel, scalaUnits) ->
                                    if (unitGroups.size > 1) {
                                        Text(scalaLabel, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = Cyan400, modifier = Modifier.padding(top = 4.dp))
                                    }
                                    scalaUnits.sortedBy { it.floor }.forEach { unit ->
                                        UnitSelectCard(
                                            unit = unit,
                                            onSelect = {
                                                viewModel.loginAsResident(selectedCondo!!.id, unit.id)
                                                // Imposta anche il condominio attivo per caricare i dati
                                                viewModel.setActiveCondominio(selectedCondo!!.id)
                                                onLogin()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottone torna admin
            item {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onBackToAdmin,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.AdminPanelSettings, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Torna all'area Amministratore", color = TextMuted)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun UnitSelectCard(unit: CondoUnit, onSelect: () -> Unit) {
    Card(
        onClick = onSelect,
        colors = CardDefaults.cardColors(containerColor = DarkBg),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Piano badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Cyan400.copy(alpha = 0.12f)
            ) {
                Text(
                    "P${unit.floor}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Cyan400,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Int. ${unit.number} â€” ${unit.ownerName}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary
                )
                Text(
                    "${unit.type} Â· ${unit.areaMq.toInt()} mÂ² Â· ${unit.millesimi.toInt()} mill.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            Icon(Icons.Filled.ArrowForward, null, tint = Cyan400, modifier = Modifier.size(18.dp))
        }
    }
}


