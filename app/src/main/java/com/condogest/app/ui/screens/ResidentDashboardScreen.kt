package com.condogest.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.condogest.app.data.model.CedolinoWithItems
import com.condogest.app.data.model.Payment
import com.condogest.app.ui.components.Formatters
import com.condogest.app.ui.components.StatusBadge
import com.condogest.app.ui.theme.*
import com.condogest.app.viewmodel.CondoViewModel

/**
 * Dashboard lato condomino con 4 tab:
 * - Appartamento: dati della propria unità
 * - Cedolini: cedolini ricevuti (sentToResident = true)
 * - Pagamenti: storico pagamenti effettuati
 * - Documenti: documenti del condominio
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResidentDashboardScreen(
    viewModel: CondoViewModel,
    onLogout: () -> Unit
) {
    val residentUnitId by viewModel.residentUnitId.collectAsState()
    val residentCedolini by viewModel.residentCedolini.collectAsState()
    val residentPayments by viewModel.residentPayments.collectAsState()
    val documenti by viewModel.documenti.collectAsState()
    val activeCondominio by viewModel.activeCondominio.collectAsState()

    // Unità del condomino — cerca nelle unità caricate
    val allUnits by viewModel.units.collectAsState()
    val myUnit = remember(residentUnitId, allUnits) { allUnits.find { it.id == residentUnitId } }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("🏠 Appartamento", "📄 Cedolini", "💳 Pagamenti", "📁 Documenti")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // ── Header condomino ──────────────────────────────────────────
        Surface(color = DarkSurface) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Avatar
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Cyan400.copy(alpha = 0.15f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Person, null, tint = Cyan400, modifier = Modifier.size(28.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            myUnit?.ownerName ?: "Condomino",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            buildString {
                                myUnit?.let {
                                    if (it.scala.isNotBlank()) append("Sc.${it.scala} · ")
                                    append("Int. ${it.number}")
                                }
                                activeCondominio?.let { append(" — ${it.nome}") }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Cyan400
                        )
                    }
                    // Logout
                    IconButton(onClick = {
                        viewModel.logoutResident()
                        onLogout()
                    }) {
                        Icon(Icons.Filled.Logout, "Esci", tint = TextMuted)
                    }
                }
            }
        }

        // ── Tab Row ───────────────────────────────────────────────────
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkSurface,
            contentColor = Cyan400,
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Cyan400
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selectedTab == index) Cyan400 else TextMuted
                        )
                    }
                )
            }
        }

        // ── Contenuto Tab ─────────────────────────────────────────────
        AnimatedContent(targetState = selectedTab, label = "resident_tabs") { tab ->
            when (tab) {
                0 -> AppartamentoTab(viewModel, myUnit)
                1 -> CedoliniRicevutiTab(residentCedolini)
                2 -> MieiPagamentiTab(residentPayments)
                3 -> DocumentiCondominioTab(viewModel)
            }
        }
    }
}

// ─── TAB 1: Il mio appartamento ──────────────────────────────────────
@Composable
private fun AppartamentoTab(viewModel: CondoViewModel, unit: com.condogest.app.data.model.CondoUnit?) {
    val activeCondominio by viewModel.activeCondominio.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val allUnits by viewModel.units.collectAsState()

    if (unit == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nessuna unità selezionata", color = TextMuted)
        }
        return
    }

    val totalMillesimi = allUnits.sumOf { it.millesimi }.takeIf { it > 0 } ?: 1000.0
    val myShare = unit.millesimi / totalMillesimi
    val totalSpese = expenses.sumOf { it.amount }
    val myShareAmount = totalSpese * myShare

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Dati appartamento
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Home, null, tint = Cyan400, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Il mio Appartamento", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                    }
                    HorizontalDivider(color = TextMuted.copy(alpha = 0.2f))
                    InfoRow("Proprietario", unit.ownerName)
                    InfoRow("Unità", "Int. ${unit.number}")
                    if (unit.scala.isNotBlank()) InfoRow("Scala", unit.scala)
                    InfoRow("Piano", "Piano ${unit.floor}")
                    InfoRow("Tipo", unit.type)
                    InfoRow("Superficie", "${unit.areaMq.toInt()} m²")
                    InfoRow("Millesimi", "${unit.millesimi.toInt()}/1000")
                    if (unit.ownerEmail.isNotBlank()) InfoRow("Email", unit.ownerEmail)
                    if (unit.ownerPhone.isNotBlank()) InfoRow("Telefono", unit.ownerPhone)
                }
            }
        }

        // Condominio
        activeCondominio?.let { condo ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Apartment, null, tint = Purple400, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Il Condominio", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                        }
                        HorizontalDivider(color = TextMuted.copy(alpha = 0.2f))
                        InfoRow("Nome", condo.nome)
                        InfoRow("Indirizzo", "${condo.indirizzo}, ${condo.citta}")
                        if (condo.cf.isNotBlank()) InfoRow("CF", condo.cf)
                    }
                }
            }
        }

        // Quota spese
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.PieChart, null, tint = Green400, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("La mia Quota Spese", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                    }
                    HorizontalDivider(color = TextMuted.copy(alpha = 0.2f))
                    InfoRow("Totale spese condominio", Formatters.currency(totalSpese))
                    InfoRow("Percentuale millesimale", "${String.format("%.1f", myShare * 100)}%")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("La mia quota", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text(Formatters.currency(myShareAmount), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Green400)
                    }
                }
            }
        }
    }
}

// ─── TAB 2: Cedolini ricevuti ─────────────────────────────────────────
@Composable
private fun CedoliniRicevutiTab(cedolini: List<CedolinoWithItems>) {
    if (cedolini.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Description, null, tint = TextMuted, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(8.dp))
                Text("Nessun cedolino ricevuto", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                Text("I cedolini appariranno qui quando l'amministratore li invia", color = TextMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Riepilogo
        item {
            val pending = cedolini.count { it.cedolino.status != "Pagato" }
            val totalDue = cedolini.filter { it.cedolino.status != "Pagato" }.sumOf { it.cedolino.total }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ResidentStatCard("Ricevuti", "${cedolini.size}", Cyan400, Modifier.weight(1f))
                ResidentStatCard("Da pagare", "$pending", Amber400, Modifier.weight(1f))
                ResidentStatCard("Importo", Formatters.currency(totalDue), Color(0xFFFF6B6B), Modifier.weight(1f))
            }
        }

        items(cedolini.sortedByDescending { it.cedolino.issueDate }) { cwi ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(cwi.cedolino.period, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary, modifier = Modifier.weight(1f))
                        StatusBadge(cwi.cedolino.status)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Emesso: ${Formatters.date(cwi.cedolino.issueDate)}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Text("Scad: ${Formatters.date(cwi.cedolino.dueDate)}", style = MaterialTheme.typography.bodySmall, color = if (cwi.cedolino.status != "Pagato") Color(0xFFFF6B6B) else TextMuted)
                    }
                    HorizontalDivider(color = TextMuted.copy(alpha = 0.2f))
                    // Voci
                    cwi.items.forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(item.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.weight(1f))
                            Text(Formatters.currency(item.amount), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                        }
                    }
                    HorizontalDivider(color = Cyan400.copy(alpha = 0.3f))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("TOTALE", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                        Spacer(Modifier.weight(1f))
                        Text(Formatters.currency(cwi.cedolino.total), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Cyan400)
                    }
                }
            }
        }
    }
}

// ─── TAB 3: I miei pagamenti ─────────────────────────────────────────
@Composable
private fun MieiPagamentiTab(payments: List<Payment>) {
    if (payments.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.CreditCard, null, tint = TextMuted, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(8.dp))
                Text("Nessun pagamento registrato", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }

    val totalPaid = payments.sumOf { it.amount }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AccountBalanceWallet, null, tint = Green400, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Totale pagato", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Text(Formatters.currency(totalPaid), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = Green400)
                    }
                    Spacer(Modifier.weight(1f))
                    Text("${payments.size} pag.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
            }
        }

        items(payments.sortedByDescending { it.date }) { payment ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(Formatters.currency(payment.amount), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Green400)
                            Spacer(Modifier.width(8.dp))
                            StatusBadge(payment.method)
                        }
                        if (payment.reference.isNotBlank()) {
                            Text("Rif: ${payment.reference}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                        if (payment.notes.isNotBlank()) {
                            Text(payment.notes, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                    Text(Formatters.date(payment.date), style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
            }
        }
    }
}

// ─── TAB 4: Documenti condominio ──────────────────────────────────────
@Composable
private fun DocumentiCondominioTab(viewModel: CondoViewModel) {
    val documenti by viewModel.documenti.collectAsState()
    val residentUnitId by viewModel.residentUnitId.collectAsState()

    // Filtra per destinatario: mostra solo "Tutti" o quelli indirizzati all'unità del residente
    val miei = remember(documenti, residentUnitId) {
        documenti.filter { doc ->
            doc.visibilita == "Tutti" ||
            doc.destinatariUnitIds.split(",").mapNotNull { it.trim().toLongOrNull() }.contains(residentUnitId)
        }
    }

    if (miei.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Folder, null, tint = TextMuted, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(8.dp))
                Text("Nessun documento disponibile", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("${miei.size} documenti disponibili", style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        items(miei) { doc ->
            var sommarioExpanded by remember { mutableStateOf(true) }  // default aperto per il residente
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(com.condogest.app.data.model.FileTypes.getIcon(doc.fileType), fontSize = 20.sp)
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(doc.titolo, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text("${doc.categoria} · ${com.condogest.app.ui.components.Formatters.date(doc.dataInserimento)}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                    }

                    // Sommario (visibile di default prima di aprire)
                    if (doc.sommario.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Amber400.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Amber400.copy(alpha = 0.25f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Notes, null, tint = Amber400, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Sintesi", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Amber400)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(doc.sommario, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }

                    // Bottone apri
                    Button(
                        onClick = {
                            val file = java.io.File(doc.filePath)
                            if (file.exists()) {
                                val fileUri = androidx.core.content.FileProvider.getUriForFile(
                                    context, "${context.packageName}.provider", file)
                                val mimeType = when (doc.fileType) {
                                    "Word" -> "application/msword"
                                    "Foto" -> "image/*"
                                    else -> "application/pdf"
                                }
                                context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                    setDataAndType(fileUri, mimeType)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan400.copy(alpha = 0.15f), contentColor = Cyan400)
                    ) {
                        Icon(Icons.Filled.OpenInNew, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Apri documento", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

// ─── Componenti helpers ───────────────────────────────────────────────
@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
    }
}

@Composable
private fun ResidentStatCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold), color = color)
            Text(title, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}
