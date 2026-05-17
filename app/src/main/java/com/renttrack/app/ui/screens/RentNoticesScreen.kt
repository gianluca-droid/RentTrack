@file:OptIn(ExperimentalMaterial3Api::class)

package com.renttrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.renttrack.app.data.model.*
import com.renttrack.app.ui.components.*
import com.renttrack.app.ui.theme.*
import com.renttrack.app.util.CedolinoPdfGenerator
import com.renttrack.app.viewmodel.SupabaseRentViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentNoticesScreen(viewModel: SupabaseRentViewModel) {
    val cedolini by viewModel.cedolini.collectAsState()
    val cedoliniWithItems by viewModel.cedoliniWithItems.collectAsState()
    val pendingCount by viewModel.pendingCedolini.collectAsState()
    val units by viewModel.units.collectAsState()
    val context = LocalContext.current

    var showGenerateDialog by remember { mutableStateOf(false) }
    var showSingleDialog by remember { mutableStateOf(false) }
    var showQuotaDialog by remember { mutableStateOf(false) }
    var showDetailDialog by remember { mutableStateOf<SCedolinoWithItems?>(null) }
    var showConfirmSendDialog by remember { mutableStateOf<SCedolino?>(null) }
    var showPagamentoDialog by remember { mutableStateOf<SCedolino?>(null) }
    // Ricevuta post-pagamento (4 var separate per evitare data class locale)
    var receiptCedolino  by remember { mutableStateOf<SCedolino?>(null) }
    var receiptMethod    by remember { mutableStateOf("") }
    var receiptReference by remember { mutableStateOf("") }
    var receiptPaid      by remember { mutableStateOf(0.0) }
    var deleteTarget by remember { mutableStateOf<SCedolino?>(null) }
    var editTarget   by remember { mutableStateOf<SCedolino?>(null) }
    var filterStatus by remember { mutableStateOf<String?>(null) }
    var showArchive by remember { mutableStateOf(false) }
    // ── Selezione multipla ────────────────────────────────────────────
    var selectionMode     by remember { mutableStateOf(false) }
    var selectedIds       by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showBulkDueDateDialog by remember { mutableStateOf(false) }

    val openCedolini = remember(cedolini) { cedolini.filter { it.status != "Pagato" }.sortedBy { it.dueDate } }
    val paidCedolini = remember(cedolini) { cedolini.filter { it.status == "Pagato" }.sortedByDescending { it.paidDate } }
    val sentCount = cedolini.count { it.sentToResident }

    Box(modifier = Modifier.fillMaxSize()) {
        if (cedolini.isEmpty()) {
            // Empty state con due CTA
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.Description, null, tint = TextMuted, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(12.dp))
                Text("Nessun avviso affitto", color = TextMuted, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { showSingleDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan500),
                    modifier = Modifier.padding(horizontal = 32.dp).fillMaxWidth()
                ) {
                    Icon(Icons.Filled.PersonAdd, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Avviso per singolo inquilino")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showGenerateDialog = true },
                    modifier = Modifier.padding(horizontal = 32.dp).fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan400)
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Genera per tutti gli inquilini")
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ─── Statistiche ───────────────────────────────────────
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Triple("${openCedolini.size}", "Da incassare", Amber400),
                            Triple("${paidCedolini.size}", "Pagati",       Green400),
                            Triple("$sentCount",           "Inviati",      Cyan400)
                        ).forEach { (value, label, color) ->
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkCard)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold), color = color, maxLines = 1)
                                    Spacer(Modifier.height(4.dp))
                                    Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
                                }
                            }
                        }
                    }
                    // Bottone Seleziona
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectionMode) {
                            TextButton(onClick = {
                                selectedIds = if (selectedIds.size == openCedolini.size)
                                    emptySet() else openCedolini.map { it.id }.toSet()
                            }) {
                                Text(
                                    if (selectedIds.size == openCedolini.size) "Deseleziona tutti" else "Seleziona tutti",
                                    color = Cyan400, style = MaterialTheme.typography.labelMedium
                                )
                            }
                            TextButton(onClick = { selectionMode = false; selectedIds = emptySet() }) {
                                Text("Annulla", color = TextMuted, style = MaterialTheme.typography.labelMedium)
                            }
                        } else if (openCedolini.isNotEmpty()) {
                            TextButton(onClick = { selectionMode = true }) {
                                Icon(Icons.Filled.CheckBox, null, tint = Cyan400, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Seleziona", color = Cyan400, style = MaterialTheme.typography.labelMedium)
                            }
                            if (selectedIds.isNotEmpty()) {
                                Text("${selectedIds.size} selezionati", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // ─── Cedolini aperti ────────────────────────────────────
                if (openCedolini.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Green400.copy(alpha = 0.08f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.CheckCircle, null, tint = Green400, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(10.dp))
                                Text("Tutto in regola! Nessun affitto in sospeso.", style = MaterialTheme.typography.bodyMedium, color = Green400)
                            }
                        }
                    }
                }

                items(openCedolini, key = { it.id }) { cedolino ->
                    val isOverdue = cedolino.dueDate < System.currentTimeMillis()
                    var showMenu by remember { mutableStateOf(false) }
                    val cwi = cedoliniWithItems.find { it.cedolino.id == cedolino.id }

                    val isSelected = cedolino.id in selectedIds
                    ItemCard(
                        modifier = if (selectionMode) Modifier
                            .clickable {
                                selectedIds = if (isSelected) selectedIds - cedolino.id
                                             else selectedIds + cedolino.id
                            }
                            .then(if (isSelected) Modifier.border(1.5.dp, Cyan400, RoundedCornerShape(14.dp)) else Modifier)
                        else Modifier,
                        onDelete = if (!selectionMode) { { deleteTarget = cedolino } } else null
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selectionMode) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedIds = if (checked) selectedIds + cedolino.id
                                                      else selectedIds - cedolino.id
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = Cyan400)
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(viewModel.getUnitName(cedolino.unitId), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                                Text(cedolino.period, style = MaterialTheme.typography.bodySmall, color = Cyan400)
                            }
                            StatusBadge(cedolino.status)
                            Spacer(Modifier.width(4.dp))
                            Box {
                                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Filled.MoreVert, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                                }
                                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Dettaglio", color = TextPrimary) },
                                        leadingIcon = { Icon(Icons.Filled.Visibility, null, tint = Cyan400, modifier = Modifier.size(18.dp)) },
                                        onClick = { showDetailDialog = cwi; showMenu = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Modifica", color = TextPrimary) },
                                        leadingIcon = { Icon(Icons.Filled.Edit, null, tint = Cyan400, modifier = Modifier.size(18.dp)) },
                                        onClick = { editTarget = cedolino; showMenu = false }
                                    )
                                    if (cwi != null) {
                                        DropdownMenuItem(
                                            text = { Text("Duplica mese successivo", color = TextPrimary) },
                                            leadingIcon = { Icon(Icons.Filled.ContentCopy, null, tint = Amber400, modifier = Modifier.size(18.dp)) },
                                            onClick = { viewModel.duplicateCedolino(cwi); showMenu = false }
                                        )
                                        // ── Genera PDF ──────────────────────────────────
                                        val tenantUnitPdf = units.find { it.id == cedolino.unitId }
                                        DropdownMenuItem(
                                            text = { Text("Genera PDF", color = TextPrimary) },
                                            leadingIcon = { Icon(Icons.Filled.PictureAsPdf, null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(18.dp)) },
                                            onClick = {
                                                try {
                                                    val uri = CedolinoPdfGenerator.generateAndShare(
                                                        context       = context,
                                                        cedolino      = cedolino,
                                                        items         = cwi.items,
                                                        tenantName    = tenantUnitPdf?.ownerName ?: viewModel.getUnitName(cedolino.unitId),
                                                        propertyName  = tenantUnitPdf?.number ?: ""
                                                    )
                                                    val pdfIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                        setDataAndType(uri, "application/pdf")
                                                        flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                                                android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                                    }
                                                    try {
                                                        context.startActivity(pdfIntent)
                                                    } catch (e: Exception) {
                                                        // Nessun visualizzatore PDF: offri condivisione
                                                        context.startActivity(
                                                            android.content.Intent.createChooser(
                                                                android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                                    type = "application/pdf"
                                                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                                    flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                                },
                                                                "Condividi PDF"
                                                            )
                                                        )
                                                    }
                                                } catch (e: Exception) {
                                                    android.util.Log.e("PDF", "Errore generazione PDF: ${e.message}")
                                                }
                                                showMenu = false
                                            }
                                        )
                                    }
                                    val shareText = buildString {
                                        appendLine("📋 Avviso Affitto")
                                        appendLine("Intestato a: ${viewModel.getUnitName(cedolino.unitId)}")
                                        appendLine("Periodo: ${cedolino.period}")
                                        cwi?.items?.forEach { appendLine("• ${it.description}: ${Formatters.currency(it.amount)}") }
                                        appendLine("─────────────────")
                                        appendLine("TOTALE: ${Formatters.currency(cedolino.total)}")
                                        appendLine("Scadenza: ${Formatters.date(cedolino.dueDate)}")
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Condividi", color = TextPrimary) },
                                        leadingIcon = { Icon(Icons.Filled.Share, null, tint = Green400, modifier = Modifier.size(18.dp)) },
                                        onClick = {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                                putExtra(android.content.Intent.EXTRA_SUBJECT, "Avviso affitto ${cedolino.period}")
                                            }
                                            context.startActivity(android.content.Intent.createChooser(intent, "Invia avviso"))
                                            showMenu = false
                                        }
                                    )
                                    // ── Invia via Email ──────────────────────────────
                                    val tenantUnit = units.find { it.id == cedolino.unitId }
                                    val tenantEmail = tenantUnit?.ownerEmail ?: ""
                                    DropdownMenuItem(
                                        text = { Text("Invia Email", color = TextPrimary) },
                                        leadingIcon = { Icon(Icons.Filled.Email, null, tint = Cyan400, modifier = Modifier.size(18.dp)) },
                                        onClick = {
                                            val subject = "Avviso affitto ${cedolino.period}"
                                            val body = buildString {
                                                appendLine("Gentile ${tenantUnit?.ownerName ?: "Inquilino"},")
                                                appendLine()
                                                appendLine("Le inviamo l'avviso di pagamento per il periodo: ${cedolino.period}")
                                                appendLine()
                                                cwi?.items?.forEach { appendLine("• ${it.description}: ${Formatters.currency(it.amount)}") }
                                                appendLine("─────────────────────")
                                                appendLine("TOTALE DA VERSARE: ${Formatters.currency(cedolino.total)}")
                                                appendLine("Scadenza: ${Formatters.date(cedolino.dueDate)}")
                                                appendLine()
                                                appendLine("Cordiali saluti")
                                            }
                                            val emailIntent = if (tenantEmail.isNotBlank()) {
                                                android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                                    data = android.net.Uri.parse("mailto:")
                                                    putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(tenantEmail))
                                                    putExtra(android.content.Intent.EXTRA_SUBJECT, subject)
                                                    putExtra(android.content.Intent.EXTRA_TEXT, body)
                                                }
                                            } else {
                                                // Fallback: condivisione generica se email non impostata
                                                android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(android.content.Intent.EXTRA_SUBJECT, subject)
                                                    putExtra(android.content.Intent.EXTRA_TEXT, body)
                                                }
                                            }
                                            try {
                                                context.startActivity(emailIntent)
                                            } catch (e: Exception) {
                                                context.startActivity(
                                                    android.content.Intent.createChooser(
                                                        android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                            type = "text/plain"
                                                            putExtra(android.content.Intent.EXTRA_TEXT, body)
                                                        },
                                                        "Invia avviso"
                                                    )
                                                )
                                            }
                                            showMenu = false
                                        }
                                    )
                                    // ── Invia via WhatsApp ────────────────────────────
                                    val tenantPhone = tenantUnit?.ownerPhone ?: ""
                                    DropdownMenuItem(
                                        text = { Text("WhatsApp", color = TextPrimary) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Filled.Chat, null,
                                                tint = androidx.compose.ui.graphics.Color(0xFF25D366),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            val waBody = buildString {
                                                appendLine("Ciao ${tenantUnit?.ownerName?.split(" ")?.firstOrNull() ?: ""},")
                                                appendLine()
                                                appendLine("ecco l'avviso per *${cedolino.period}*:")
                                                appendLine()
                                                cwi?.items?.forEach { appendLine("• ${it.description}: ${Formatters.currency(it.amount)}") }
                                                appendLine("────────────────")
                                                appendLine("*TOTALE: ${Formatters.currency(cedolino.total)}*")
                                                appendLine("Scadenza: ${Formatters.date(cedolino.dueDate)}")
                                            }
                                            try {
                                                if (tenantPhone.isNotBlank()) {
                                                    val cleanPhone = tenantPhone
                                                        .replace(Regex("[\\s\\-().+]"), "")
                                                        .let { if (it.startsWith("0")) "39$it" else it }
                                                    val encoded = java.net.URLEncoder.encode(waBody, "UTF-8")
                                                    val waIntent = android.content.Intent(
                                                        android.content.Intent.ACTION_VIEW,
                                                        android.net.Uri.parse("https://wa.me/$cleanPhone?text=$encoded")
                                                    )
                                                    context.startActivity(waIntent)
                                                } else {
                                                    context.startActivity(
                                                        android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                            type = "text/plain"
                                                            setPackage("com.whatsapp")
                                                            putExtra(android.content.Intent.EXTRA_TEXT, waBody)
                                                        }
                                                    )
                                                }
                                            } catch (e: Exception) {
                                                context.startActivity(
                                                    android.content.Intent.createChooser(
                                                        android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                            type = "text/plain"
                                                            putExtra(android.content.Intent.EXTRA_TEXT, waBody)
                                                        }, "Invia avviso"
                                                    )
                                                )
                                            }
                                            showMenu = false
                                        }
                                    )
                                    if (!cedolino.sentToResident) {
                                        DropdownMenuItem(
                                            text = { Text("Segna come inviato", color = TextPrimary) },
                                            leadingIcon = { Icon(Icons.Filled.Send, null, tint = Cyan400, modifier = Modifier.size(18.dp)) },
                                            onClick = { showConfirmSendDialog = cedolino; showMenu = false }
                                        )
                                    }

                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(Formatters.currency(cedolino.total), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = TextPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Scad. ${Formatters.date(cedolino.dueDate)}", style = MaterialTheme.typography.bodySmall, color = if (isOverdue) Color(0xFFFF6B6B) else TextMuted)
                                if (isOverdue) Text("⚠ SCADUTO", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFFFF6B6B))
                            }
                        }
                        if (cedolino.paidAmount > 0) {
                            Spacer(Modifier.height(4.dp))
                            Text("Versato: ${Formatters.currency(cedolino.paidAmount)} / ${Formatters.currency(cedolino.total)}", style = MaterialTheme.typography.bodySmall, color = Green400)
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { showPagamentoDialog = cedolino },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Green500),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Segna Pagato", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                // ─── Archivio pagamenti collassabile ────────────────────
                if (paidCedolini.isNotEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkCard),
                            onClick = { showArchive = !showArchive }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Archive, null, tint = Green400, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Storico pagamenti (${paidCedolini.size})",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    if (showArchive) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    null, tint = TextMuted, modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    if (showArchive) {
                        items(paidCedolini, key = { "paid_${it.id}" }) { cedolino ->
                            var showMenu by remember { mutableStateOf(false) }
                            val cwi = cedoliniWithItems.find { it.cedolino.id == cedolino.id }
                            ItemCard(onDelete = { deleteTarget = cedolino }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(viewModel.getUnitName(cedolino.unitId), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                                        Text(cedolino.period, style = MaterialTheme.typography.bodySmall, color = Cyan400)
                                    }
                                    StatusBadge(cedolino.status)
                                    Spacer(Modifier.width(4.dp))
                                    Box {
                                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Filled.MoreVert, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                                        }
                                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                            DropdownMenuItem(
                                                text = { Text("Dettaglio", color = TextPrimary) },
                                                leadingIcon = { Icon(Icons.Filled.Visibility, null, tint = Cyan400, modifier = Modifier.size(18.dp)) },
                                                onClick = { showDetailDialog = cwi; showMenu = false }
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(Formatters.currency(cedolino.total), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Green400, modifier = Modifier.weight(1f))
                                    cedolino.paidDate?.let {
                                        Text("Pagato il ${Formatters.date(it)}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                    }
                                }
                            }
                        }
                    }
                }

            item { Spacer(Modifier.height(if (selectionMode) 140.dp else 100.dp)) }
            }
        }

        // ── Barra azioni bulk (visibile in modalità selezione) ────────────
        androidx.compose.animation.AnimatedVisibility(
            visible = selectionMode,
            enter = androidx.compose.animation.slideInVertically { it },
            exit = androidx.compose.animation.slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkSurface,
                shadowElevation = 12.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        if (selectedIds.isEmpty()) "Seleziona gli avvisi" else "${selectedIds.size} avvisi selezionati",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (selectedIds.isEmpty()) TextMuted else Cyan400
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { if (selectedIds.isNotEmpty()) showBulkDueDateDialog = true },
                            enabled = selectedIds.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = DarkBg),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.CalendarMonth, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Cambia scadenza", style = MaterialTheme.typography.labelMedium)
                        }
                        Button(
                            onClick = {
                                if (selectedIds.isNotEmpty()) {
                                    viewModel.bulkMarkSent(selectedIds)
                                    selectionMode = false; selectedIds = emptySet()
                                }
                            },
                            enabled = selectedIds.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = Green500, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Send, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Segna inviati", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // FAB menu — nascosto in modalità selezione
        if (!selectionMode) Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // FAB: addebita quota diretta a singola unità
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurface.copy(alpha = 0.92f)
                ) {
                    Text(
                        "Quota extra inquilino",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                SmallFloatingActionButton(
                    onClick = { showQuotaDialog = true },
                    containerColor = Amber400.copy(alpha = 0.9f),
                    contentColor = DarkBg
                ) {
                    Icon(Icons.Filled.EuroSymbol, "Addebita importo a inquilino")
                }
            }
            // FAB secondario: singolo condomino
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurface.copy(alpha = 0.92f)
                ) {
                    Text(
                        "Avviso singolo inquilino",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                SmallFloatingActionButton(
                    onClick = { showSingleDialog = true },
                    containerColor = DarkSurface,
                    contentColor = Cyan400
                ) {
                    Icon(Icons.Filled.PersonAdd, "Nuovo avviso")
                }
            }
            // FAB principale: genera per tutti
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurface.copy(alpha = 0.92f)
                ) {
                    Text(
                        "Genera per tutti",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                @Suppress("DEPRECATION")
                GradientFab(
                    icon = Icons.Filled.NoteAdd,
                    contentDescription = "Genera avvisi affitto",
                    onClick = { showGenerateDialog = true }
                )
            }
        }
    }

    // Dialog: cedolino per singolo condomino
    if (showSingleDialog && units.isNotEmpty()) {
        SingleCedolinoDialog(
            units = units,
            onDismiss = { showSingleDialog = false },
            onCreate = { cedolino, items ->
                viewModel.addCedolinoWithItems(cedolino, items)
                showSingleDialog = false
            }
        )
    }

    // Dialog: modifica data scadenza su avvisi selezionati
    if (showBulkDueDateDialog) {
        var dayInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showBulkDueDateDialog = false },
            containerColor = DarkSurface,
            icon = { Icon(Icons.Filled.CalendarMonth, null, tint = Cyan400) },
            title = {
                Text(
                    "Cambia giorno di scadenza",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Applica a ${selectedIds.size} avvisi selezionati.\nInserisci il nuovo giorno del mese (1–31):",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = dayInput,
                        onValueChange = { dayInput = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Giorno (es. 15)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Cyan400,
                            unfocusedBorderColor = TextMuted.copy(alpha = 0.4f),
                            focusedLabelColor = Cyan400,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newDay = dayInput.toIntOrNull()?.coerceIn(1, 31)
                        if (newDay != null) {
                            viewModel.bulkUpdateDueDayForCedolini(selectedIds, newDay)
                            showBulkDueDateDialog = false
                            selectionMode = false
                            selectedIds = emptySet()
                        }
                    },
                    enabled = dayInput.toIntOrNull()?.let { it in 1..31 } == true
                ) {
                    Text("Applica", color = Cyan400, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDueDateDialog = false }) {
                    Text("Annulla", color = TextSecondary)
                }
            }
        )
    }

    // Dialog: modifica cedolino (2 step: modifica + eventuale propagazione scadenza)
    editTarget?.let { ced ->
        val sameUnitOthers = openCedolini.filter { it.unitId == ced.unitId && it.id != ced.id }
        EditCedolinoDialog(
            cedolino       = ced,
            sameUnitOthers = sameUnitOthers,
            onDismiss      = { editTarget = null },
            onSave         = { updated, propagateIds ->
                viewModel.updateCedolino(updated)
                if (propagateIds.isNotEmpty()) {
                    val day = Calendar.getInstance()
                        .apply { timeInMillis = updated.dueDate }
                        .get(Calendar.DAY_OF_MONTH)
                    viewModel.bulkUpdateDueDayForCedolini(propagateIds, day)
                }
                editTarget = null
            }
        )
    }

    // Dialog: addebita quota diretta a singola unità
    if (showQuotaDialog && units.isNotEmpty()) {
        QuotaDirectaDialog(
            units = units,
            onDismiss = { showQuotaDialog = false },
            onCreate = { unitId, importo, descrizione, categoria, periodo, dueDate ->
                viewModel.addQuotaDirecta(unitId, importo, descrizione, categoria, periodo, dueDate)
                showQuotaDialog = false
            }
        )
    }

    // Dialog: genera per tutte le unità (range di mesi)
    if (showGenerateDialog) {
        GenerateCedoliniDialog(
            onDismiss = { showGenerateDialog = false },
            onGenerate = { periods ->
                // periods = lista di Pair(periodo: String, dueDate: Long)
                periods.forEach { (period, dueDate) ->
                    viewModel.generateCedoliniForAllUnits(period, dueDate)
                }
                showGenerateDialog = false
            }
        )
    }

    // Dialog: dettaglio cedolino
    showDetailDialog?.let { cwi ->
        CedolinoDetailDialog(
            cwi = cwi,
            unitName = viewModel.getUnitName(cwi.cedolino.unitId),
            onDismiss = { showDetailDialog = null }
        )
    }

    // Dialog: conferma invio al condomino
    showConfirmSendDialog?.let { cedolino ->
        AlertDialog(
            onDismissRequest = { showConfirmSendDialog = null },
            icon = { Icon(Icons.Filled.Send, null, tint = Cyan400) },
            title = { Text("Conferma Invio") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Stai per inviare l'avviso affitto del periodo \"${cedolino.period}\" a:",
                        style = MaterialTheme.typography.bodyMedium, color = TextSecondary
                    )
                    Text(
                        viewModel.getUnitName(cedolino.unitId),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = Cyan400
                    )
                    Text(
                        "Importo: ${Formatters.currency(cedolino.total)}",
                        style = MaterialTheme.typography.bodyMedium, color = TextPrimary
                    )
                    HorizontalDivider(color = TextMuted.copy(alpha = 0.2f))
                    Text(
                        "Una volta inviato, sarà disponibile per l'inquilino.",
                        style = MaterialTheme.typography.bodySmall, color = TextMuted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.markCedolinoSent(cedolino)
                        showConfirmSendDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan500)
                ) { Text("Conferma Invio") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmSendDialog = null }) { Text("Annulla") }
            },
            containerColor = DarkSurface
        )
    }

    // Dialog: registra pagamento con metodo
    showPagamentoDialog?.let { ced ->
        RegistraPagamentoDialog(
            ced,
            viewModel.getUnitName(ced.unitId),
            { showPagamentoDialog = null },
            { method, reference ->
                viewModel.markCedolinoPaidWithPayment(ced, method, reference)
                // Prepara dati ricevuta
                receiptCedolino  = ced
                receiptMethod    = method
                receiptReference = reference
                receiptPaid      = ced.total - ced.paidAmount
                showPagamentoDialog = null
            }
        )
    }

    // Dialog: genera ricevuta post-pagamento
    receiptCedolino?.let { ced ->
        val context = androidx.compose.ui.platform.LocalContext.current
        AlertDialog(
            onDismissRequest = { receiptCedolino = null },
            containerColor = com.renttrack.app.ui.theme.DarkSurface,
            icon = {
                androidx.compose.material3.Icon(
                    androidx.compose.material.icons.Icons.Filled.PictureAsPdf,
                    contentDescription = null,
                    tint = com.renttrack.app.ui.theme.Green400
                )
            },
            title = {
                androidx.compose.material3.Text(
                    "Pagamento registrato! ✅",
                    color = com.renttrack.app.ui.theme.TextPrimary,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            },
            text = {
                androidx.compose.material3.Text(
                    "Vuoi generare e condividere la ricevuta PDF per ${viewModel.getUnitName(ced.unitId)}?",
                    color = com.renttrack.app.ui.theme.TextSecondary
                )
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        val cedolinoSnap   = ced
                        val methodSnap     = receiptMethod
                        val referenceSnap  = receiptReference
                        val paidSnap       = receiptPaid
                        receiptCedolino = null
                        try {
                            val cwi = cedoliniWithItems.find { it.cedolino.id == cedolinoSnap.id }
                            val receiptItems = cwi?.items ?: emptyList()
                            val uri = com.renttrack.app.util.CedolinoPdfGenerator.generateReceiptAndShare(
                                context,
                                cedolinoSnap,
                                receiptItems,
                                viewModel.getUnitName(cedolinoSnap.unitId),
                                viewModel.getCondoName(),
                                methodSnap,
                                referenceSnap,
                                paidSnap
                            )
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                android.content.Intent.createChooser(shareIntent, "Condividi ricevuta")
                                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("Receipt", "Errore ricevuta: ${e.message}")
                        }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = com.renttrack.app.ui.theme.Green400,
                        contentColor   = com.renttrack.app.ui.theme.DarkBg
                    )
                ) {
                    androidx.compose.material3.Text("Genera PDF", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { receiptCedolino = null }) {
                    androidx.compose.material3.Text("No grazie", color = com.renttrack.app.ui.theme.TextSecondary)
                }
            }
        )
    }

    // Dialog: elimina
    deleteTarget?.let { cedolino ->
        ConfirmDeleteDialog(
            itemName = "Avviso ${cedolino.period} - ${viewModel.getUnitName(cedolino.unitId)}",
            onConfirm = { viewModel.deleteCedolino(cedolino); deleteTarget = null },
            onDismiss = { deleteTarget = null }
        )
    }
}

// ─── Dialog: Cedolino per singolo condomino ──────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleCedolinoDialog(
    units: List<SCondoUnit>,
    onDismiss: () -> Unit,
    onCreate: (SCedolino, List<SCedolinoItem>) -> Unit
) {
    var selectedUnit by remember { mutableStateOf(units.first()) }
    var unitExpanded by remember { mutableStateOf(false) }
    var period by remember { mutableStateOf("") }
    var items by remember { mutableStateOf(listOf(Pair("", ""))) }
    // dueDate reattivo all'unità selezionata: giorno configurato nell'anagrafica, mese successivo
    val dueDate by remember(selectedUnit) {
        derivedStateOf {
            Calendar.getInstance().apply {
                add(Calendar.MONTH, 1)
                set(Calendar.DAY_OF_MONTH, minOf(selectedUnit.paymentDayOfMonth, getActualMaximum(Calendar.DAY_OF_MONTH)))
                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59)
            }.timeInMillis
        }
    }

    AlertDialog(
        onDismissRequest = { /* blocca chiusura accidentale — usare Annulla */ },
        title = { Text("Nuovo Avviso Affitto") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.heightIn(max = 480.dp)
            ) {
                // Selezione unità
                item {
                    ExposedDropdownMenuBox(expanded = unitExpanded, onExpandedChange = { unitExpanded = it }) {
                        OutlinedTextField(
                            value = "${selectedUnit.ownerName} (${selectedUnit.number})",
                            onValueChange = {}, readOnly = true,
                            label = { Text("Inquilino") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(unitExpanded) }
                        )
                        ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                            units.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text("${u.ownerName} (${u.number})") },
                                    onClick = { selectedUnit = u; unitExpanded = false }
                                )
                            }
                        }
                    }
                }
                // Periodo
                item {
                    OutlinedTextField(
                        value = period, onValueChange = { period = it },
                        label = { Text("Periodo (es. II Trimestre 2026)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                }
                // Scadenza — calcolata dal giorno configurato nell'anagrafica inquilino
                item {
                    Text(
                        "Scadenza: ${Formatters.date(dueDate)}  (giorno ${selectedUnit.paymentDayOfMonth} da anagrafica)",
                        style = MaterialTheme.typography.bodySmall, color = Cyan400
                    )
                }
                // Voci di spesa
                item {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Voci avviso",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = TextSecondary, modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { items = items + Pair("", "") }) {
                            Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Aggiungi")
                        }
                    }
                }
                itemsIndexed(items) { idx, item ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = item.first,
                            onValueChange = { desc ->
                                items = items.toMutableList().also { it[idx] = it[idx].copy(first = desc) }
                            },
                            label = { Text("Descrizione") },
                            modifier = Modifier.weight(2f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = item.second,
                            onValueChange = { amt ->
                                items = items.toMutableList().also { it[idx] = it[idx].copy(second = amt) }
                            },
                            label = { Text("€") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        if (items.size > 1) {
                            IconButton(
                                onClick = { items = items.toMutableList().also { it.removeAt(idx) } },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.Delete, null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                // Totale calcolato
                item {
                    val total = items.sumOf { it.second.toDoubleOrNull() ?: 0.0 }
                    if (total > 0) {
                        HorizontalDivider(color = Cyan400.copy(alpha = 0.3f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Totale", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                            Text(Formatters.currency(total), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Cyan400)
                        }
                    }
                }
            }
        },
        confirmButton = {
            val total = items.sumOf { it.second.toDoubleOrNull() ?: 0.0 }
            val isValid = period.isNotBlank() && total > 0 && items.all { it.first.isNotBlank() && (it.second.toDoubleOrNull() ?: 0.0) > 0 }
            Button(
                onClick = {
                    val cedolino = SCedolino(
                        unitId = selectedUnit.id,
                        condominioId = selectedUnit.condominioId,
                        period = period,
                        issueDate = System.currentTimeMillis(),
                        dueDate = dueDate,
                        total = total,
                        status = "Emesso"
                    )
                    val cedItems = items.map { SCedolinoItem(description = it.first, amount = it.second.toDoubleOrNull() ?: 0.0) }
                    onCreate(cedolino, cedItems)
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = Cyan500)
            ) { Text("Crea Avviso") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
        containerColor = DarkSurface
    )
}

// ─── Dialog: Genera per tutte le unità (range mesi) ──────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenerateCedoliniDialog(
    onDismiss: () -> Unit,
    onGenerate: (List<Pair<String, Long>>) -> Unit
) {
    val nomiMesi = listOf(
        "Gennaio","Febbraio","Marzo","Aprile","Maggio","Giugno",
        "Luglio","Agosto","Settembre","Ottobre","Novembre","Dicembre"
    )
    val now = Calendar.getInstance()
    val currentMonth = now.get(Calendar.MONTH)   // 0-based
    val currentYear  = now.get(Calendar.YEAR)

    // Anno range ±2 anni
    val years = (currentYear - 1..currentYear + 2).toList()

    // Stato: mese/anno di inizio e fine
    var fromMonth by remember { mutableIntStateOf(currentMonth) }
    var fromYear  by remember { mutableIntStateOf(currentYear) }
    var toMonth   by remember { mutableIntStateOf(currentMonth) }
    var toYear    by remember { mutableIntStateOf(currentYear) }

    // Giorno di scadenza (default 5 del mese successivo — modificabile)
    var dueDay by remember { mutableStateOf("5") }

    var fromMonthExpanded by remember { mutableStateOf(false) }
    var fromYearExpanded  by remember { mutableStateOf(false) }
    var toMonthExpanded   by remember { mutableStateOf(false) }
    var toYearExpanded    by remember { mutableStateOf(false) }

    // Calcola i periodi nel range selezionato
    val periodsInRange = remember(fromMonth, fromYear, toMonth, toYear) {
        val result = mutableListOf<Pair<String, Long>>()
        val cal = Calendar.getInstance().apply {
            set(Calendar.MONTH, fromMonth)
            set(Calendar.YEAR, fromYear)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val endCal = Calendar.getInstance().apply {
            set(Calendar.MONTH, toMonth)
            set(Calendar.YEAR, toYear)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        while (!cal.after(endCal)) {
            val m = cal.get(Calendar.MONTH)
            val y = cal.get(Calendar.YEAR)
            val period = "${nomiMesi[m]} $y"
            // dueDate = giorno [dueDay] del mese successivo
            val dueCal = Calendar.getInstance().apply {
                set(Calendar.YEAR,  if (m == 11) y + 1 else y)
                set(Calendar.MONTH, if (m == 11) 0 else m + 1)
                set(Calendar.DAY_OF_MONTH, minOf((dueDay.toIntOrNull() ?: 5).coerceIn(1, 31), getActualMaximum(Calendar.DAY_OF_MONTH)))
                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59)
            }
            result.add(period to dueCal.timeInMillis)
            cal.add(Calendar.MONTH, 1)
        }
        result
    }

    val isRangeValid = fromYear < toYear || (fromYear == toYear && fromMonth <= toMonth)

    AlertDialog(
        onDismissRequest = { /* blocca chiusura accidentale — usare Annulla */ },
        containerColor = DarkSurface,
        icon = { Icon(Icons.Filled.AutoAwesome, null, tint = Cyan400) },
        title = { Text("Genera avvisi affitto", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Verrà generato un avviso per ogni inquilino registrato per ogni mese nel periodo selezionato.",
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary
                )

                // ── Da ──────────────────────────────────────────────
                Text("Da:", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Mese inizio
                    ExposedDropdownMenuBox(
                        expanded = fromMonthExpanded,
                        onExpandedChange = { fromMonthExpanded = it },
                        modifier = Modifier.weight(3f)
                    ) {
                        OutlinedTextField(
                            value = nomiMesi[fromMonth],
                            onValueChange = {}, readOnly = true,
                            label = { Text("Mese") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(fromMonthExpanded) },
                            colors = condoTextFieldColors()
                        )
                        ExposedDropdownMenu(expanded = fromMonthExpanded, onDismissRequest = { fromMonthExpanded = false }) {
                            nomiMesi.forEachIndexed { idx, nome ->
                                DropdownMenuItem(
                                    text = { Text(nome, color = TextPrimary) },
                                    onClick = { fromMonth = idx; fromMonthExpanded = false }
                                )
                            }
                        }
                    }
                    // Anno inizio — weight maggiore per mostrare '2026' senza troncamento
                    ExposedDropdownMenuBox(
                        expanded = fromYearExpanded,
                        onExpandedChange = { fromYearExpanded = it },
                        modifier = Modifier.weight(2f)
                    ) {
                        OutlinedTextField(
                            value = fromYear.toString(),
                            onValueChange = {}, readOnly = true,
                            label = { Text("Anno") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            colors = condoTextFieldColors()
                        )
                        ExposedDropdownMenu(expanded = fromYearExpanded, onDismissRequest = { fromYearExpanded = false }) {
                            years.forEach { y ->
                                DropdownMenuItem(
                                    text = { Text(y.toString(), color = TextPrimary) },
                                    onClick = { fromYear = y; fromYearExpanded = false }
                                )
                            }
                        }
                    }
                }

                // ── A ───────────────────────────────────────────────
                Text("A:", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Mese fine
                    ExposedDropdownMenuBox(
                        expanded = toMonthExpanded,
                        onExpandedChange = { toMonthExpanded = it },
                        modifier = Modifier.weight(3f)
                    ) {
                        OutlinedTextField(
                            value = nomiMesi[toMonth],
                            onValueChange = {}, readOnly = true,
                            label = { Text("Mese") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(toMonthExpanded) },
                            colors = condoTextFieldColors()
                        )
                        ExposedDropdownMenu(expanded = toMonthExpanded, onDismissRequest = { toMonthExpanded = false }) {
                            nomiMesi.forEachIndexed { idx, nome ->
                                DropdownMenuItem(
                                    text = { Text(nome, color = TextPrimary) },
                                    onClick = { toMonth = idx; toMonthExpanded = false }
                                )
                            }
                        }
                    }
                    // Anno fine
                    ExposedDropdownMenuBox(
                        expanded = toYearExpanded,
                        onExpandedChange = { toYearExpanded = it },
                        modifier = Modifier.weight(2f)
                    ) {
                        OutlinedTextField(
                            value = toYear.toString(),
                            onValueChange = {}, readOnly = true,
                            label = { Text("Anno") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            colors = condoTextFieldColors()
                        )
                        ExposedDropdownMenu(expanded = toYearExpanded, onDismissRequest = { toYearExpanded = false }) {
                            years.forEach { y ->
                                DropdownMenuItem(
                                    text = { Text(y.toString(), color = TextPrimary) },
                                    onClick = { toYear = y; toYearExpanded = false }
                                )
                            }
                        }
                    }
                }

                // ── Giorno scadenza ─────────────────────────────────
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Cyan400.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Info, null, tint = Cyan400, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "La data di scadenza viene letta automaticamente dal giorno configurato nell'anagrafica di ogni inquilino.",
                            style = MaterialTheme.typography.bodySmall, color = TextSecondary
                        )
                    }
                }

                // ── Riepilogo ───────────────────────────────────────
                if (isRangeValid && periodsInRange.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Cyan400.copy(alpha = 0.08f)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "✅ ${periodsInRange.size} ${if (periodsInRange.size == 1) "mese" else "mesi"} selezionati:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Cyan400
                            )
                            periodsInRange.forEach { (period, dueTs) ->
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(period, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                    Text("scad. ${Formatters.date(dueTs)}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                }
                            }
                        }
                    }
                } else if (!isRangeValid) {
                    Text(
                        "⚠ La data di inizio deve essere precedente o uguale alla data di fine.",
                        style = MaterialTheme.typography.bodySmall, color = Color(0xFFFF6B6B)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onGenerate(periodsInRange) },
                enabled = isRangeValid && periodsInRange.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan500)
            ) {
                Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (periodsInRange.size > 1) "Genera ${periodsInRange.size} mesi" else "Genera",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla", color = TextSecondary) } }
    )
}


// ─── Dialog: Dettaglio cedolino ──────────────────────────────────────
@Composable
private fun CedolinoDetailDialog(cwi: SCedolinoWithItems, unitName: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Avviso Affitto — ${cwi.cedolino.period}") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item {
                    Text(unitName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = Cyan400)
                    Spacer(Modifier.height(4.dp))
                    Row { Text("Emesso: ", color = TextMuted, style = MaterialTheme.typography.bodySmall); Text(Formatters.date(cwi.cedolino.issueDate), style = MaterialTheme.typography.bodySmall, color = TextPrimary) }
                    Row { Text("Scadenza: ", color = TextMuted, style = MaterialTheme.typography.bodySmall); Text(Formatters.date(cwi.cedolino.dueDate), style = MaterialTheme.typography.bodySmall, color = TextPrimary) }
                    if (cwi.cedolino.sentToResident && cwi.cedolino.sentAt != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Send, null, tint = Green400, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Inviato il ${Formatters.date(cwi.cedolino.sentAt)}", color = Green400, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = TextMuted.copy(alpha = 0.3f))
                    Text("Voci avviso", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                }
                items(cwi.items) { item ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text(item.description, style = MaterialTheme.typography.bodySmall, color = TextPrimary, modifier = Modifier.weight(1f))
                        Text(Formatters.currency(item.amount), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                    }
                }
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Cyan400.copy(alpha = 0.3f))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("TOTALE", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                        Spacer(Modifier.weight(1f))
                        Text(Formatters.currency(cwi.cedolino.total), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Cyan400)
                    }
                    Spacer(Modifier.height(8.dp))
                    StatusBadge(cwi.cedolino.status)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } },
        containerColor = DarkSurface
    )
}

// ─── Dialog: Quota Diretta a Singola Unità ───────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotaDirectaDialog(
    units: List<SCondoUnit>,
    onDismiss: () -> Unit,
    onCreate: (unitId: String, importo: Double, descrizione: String, categoria: String, periodo: String, dueDate: Long) -> Unit
) {
    var selectedUnit by remember { mutableStateOf(units.firstOrNull()) }
    var importo by remember { mutableStateOf("") }
    var descrizione by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf(com.renttrack.app.data.model.ExpenseCategories.categories.firstOrNull()?.first ?: "") }
    var periodo by remember {
        val cal = java.util.Calendar.getInstance()
        val mesi = listOf("Gennaio","Febbraio","Marzo","Aprile","Maggio","Giugno",
            "Luglio","Agosto","Settembre","Ottobre","Novembre","Dicembre")
        mutableStateOf("${mesi[cal.get(java.util.Calendar.MONTH)]} ${cal.get(java.util.Calendar.YEAR)}")
    }
    var unitExpanded by remember { mutableStateOf(false) }
    var catExpanded by remember { mutableStateOf(false) }

    // Scadenza reattiva all'unità selezionata: usa paymentDayOfMonth dall'anagrafica
    val defaultDue by remember(selectedUnit) {
        derivedStateOf {
            java.util.Calendar.getInstance().apply {
                add(java.util.Calendar.MONTH, 1)
                set(java.util.Calendar.DAY_OF_MONTH, minOf((selectedUnit?.paymentDayOfMonth ?: 5).coerceIn(1, 31), getActualMaximum(java.util.Calendar.DAY_OF_MONTH)))
                set(java.util.Calendar.HOUR_OF_DAY, 23); set(java.util.Calendar.MINUTE, 59); set(java.util.Calendar.SECOND, 59)
            }.timeInMillis
        }
    }

    AlertDialog(
        onDismissRequest = { /* blocca chiusura accidentale — usare Annulla */ },
        containerColor = com.renttrack.app.ui.theme.DarkSurface,
        icon = { Icon(Icons.Filled.EuroSymbol, null, tint = Amber400) },
        title = { Text("Addebita importo a inquilino", color = com.renttrack.app.ui.theme.TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Selezione unità
                ExposedDropdownMenuBox(expanded = unitExpanded, onExpandedChange = { unitExpanded = it }) {
                    OutlinedTextField(
                        value = selectedUnit?.let { "Int. ${it.number} - ${it.ownerName}" } ?: "Seleziona unità",
                        onValueChange = {}, readOnly = true, label = { Text("Inquilino") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(unitExpanded) }
                    )
                    ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                        units.sortedBy { it.number }.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text("${unit.ownerName} — ${unit.number}", color = com.renttrack.app.ui.theme.TextPrimary) },
                                onClick = { selectedUnit = unit; unitExpanded = false }
                            )
                        }
                    }
                }
                // Categoria
                ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
                    OutlinedTextField(
                        value = categoria, onValueChange = {}, readOnly = true, label = { Text("Categoria") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catExpanded) }
                    )
                    ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        com.renttrack.app.data.model.ExpenseCategories.categories.forEach { (name, icon) ->
                            DropdownMenuItem(
                                text = { Text("$icon  $name", color = com.renttrack.app.ui.theme.TextPrimary) },
                                onClick = { categoria = name; catExpanded = false }
                            )
                        }
                    }
                }
                OutlinedTextField(descrizione, { descrizione = it }, label = { Text("Descrizione") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(
                    importo, { importo = it }, label = { Text("Importo (€)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                )
                OutlinedTextField(periodo, { periodo = it }, label = { Text("Periodo (es. Maggio 2026)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                // Scadenza calcolata dall'anagrafica
                selectedUnit?.let { u ->
                    Surface(shape = RoundedCornerShape(8.dp), color = Amber400.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CalendarMonth, null, tint = Amber400, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Scadenza: ${Formatters.date(defaultDue)}  (giorno ${u.paymentDayOfMonth} da anagrafica)",
                                style = MaterialTheme.typography.labelSmall, color = com.renttrack.app.ui.theme.TextSecondary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val unit = selectedUnit ?: return@Button
                    val imp = importo.toDoubleOrNull() ?: return@Button
                    onCreate(unit.id, imp, descrizione.trim().ifBlank { "Quota" }, categoria, periodo.trim(), defaultDue)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Amber400, contentColor = com.renttrack.app.ui.theme.DarkBg),
                enabled = selectedUnit != null && (importo.toDoubleOrNull() ?: 0.0) > 0
            ) { Text("Addebita", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla", color = com.renttrack.app.ui.theme.TextSecondary) } }
    )
}

// ─── Dialog: Propaga giorno scadenza agli altri avvisi ───────────────────────
@Composable
private fun PropagateDueDateDialog(
    newDay: Int,
    otherCedolini: List<SCedolino>,
    onDismiss: () -> Unit,
    onApply: (Set<String>) -> Unit
) {
    // Di default tutti pre-selezionati
    var selectedIds by remember { mutableStateOf(otherCedolini.map { it.id }.toSet()) }
    val allSelected = selectedIds.size == otherCedolini.size

    AlertDialog(
        onDismissRequest = { /* usa i pulsanti */ },
        containerColor = DarkSurface,
        icon = { Icon(Icons.Filled.CalendarToday, null, tint = Cyan400) },
        title = {
            Text(
                "Applica giorno $newDay ad altri avvisi?",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "La scadenza è stata aggiornata. Vuoi applicare il giorno $newDay anche agli altri avvisi aperti di questo inquilino?",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                // Toggle seleziona/deseleziona tutti
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Cyan400.copy(alpha = 0.06f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = allSelected,
                        onCheckedChange = {
                            selectedIds = if (allSelected) emptySet()
                            else otherCedolini.map { it.id }.toSet()
                        },
                        colors = CheckboxDefaults.colors(checkedColor = Cyan400)
                    )
                    Text(
                        if (allSelected) "Deseleziona tutti" else "Seleziona tutti (${otherCedolini.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = Cyan400,
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = TextMuted.copy(alpha = 0.12f))

                // Lista avvisi con checkbox
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    otherCedolini.forEach { ced ->
                        val isChecked = ced.id in selectedIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isChecked) Cyan400.copy(alpha = 0.05f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    selectedIds = if (isChecked)
                                        selectedIds - ced.id
                                    else
                                        selectedIds + ced.id
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    selectedIds = if (isChecked)
                                        selectedIds - ced.id else selectedIds + ced.id
                                },
                                colors = CheckboxDefaults.colors(checkedColor = Cyan400)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    ced.period,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isChecked) TextPrimary else TextSecondary,
                                    fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal
                                )
                                Text(
                                    "Scad. ${Formatters.date(ced.dueDate)} · €%.2f".format(ced.total),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(selectedIds) },
                enabled = selectedIds.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = DarkBg)
            ) {
                Icon(Icons.Filled.CalendarToday, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Applica a ${selectedIds.size} avvis${if (selectedIds.size == 1) "o" else "i"}",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Solo questo (già salvato)", color = TextSecondary)
            }
        }
    )
}

// ─── Dialog: Modifica cedolino — Step 1: form | Step 2: propaga scadenza ─────
@Composable
private fun EditCedolinoDialog(
    cedolino: SCedolino,
    sameUnitOthers: List<SCedolino>,          // altri avvisi aperti dello stesso inquilino
    onDismiss: () -> Unit,
    onSave: (SCedolino, Set<String>) -> Unit  // cedolino aggiornato + IDs da propagare
) {
    val cal        = remember { Calendar.getInstance().apply { timeInMillis = cedolino.dueDate } }
    var period     by remember { mutableStateOf(cedolino.period) }
    var dueDayStr  by remember { mutableStateOf(cal.get(Calendar.DAY_OF_MONTH).toString()) }
    var importoStr by remember { mutableStateOf(cedolino.total.toString()) }

    // Step 1 = modifica, Step 2 = propaga scadenza (solo se giorno cambia e ci sono altri)
    var step           by remember { mutableIntStateOf(1) }
    var pendingUpdated by remember { mutableStateOf<SCedolino?>(null) }
    var propagateIds   by remember { mutableStateOf(sameUnitOthers.map { it.id }.toSet()) }

    val tfColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor    = Cyan400,
        unfocusedBorderColor  = TextMuted.copy(alpha = 0.3f),
        focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface,
        cursorColor = Cyan400
    )

    if (step == 1) {
        AlertDialog(
            onDismissRequest = { /* usa Annulla */ },
            containerColor   = DarkSurface,
            icon  = { Icon(Icons.Filled.Edit, null, tint = Cyan400) },
            title = { Text("Modifica avviso", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = period, onValueChange = { period = it },
                        label = { Text("Periodo (es. Maggio 2026)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true, colors = tfColors
                    )
                    OutlinedTextField(
                        value = importoStr, onValueChange = { importoStr = it },
                        label = { Text("Importo (€)") },
                        leadingIcon = { Icon(Icons.Filled.EuroSymbol, null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = tfColors
                    )
                    Text("Scadenza attuale: ${Formatters.date(cedolino.dueDate)}",
                        style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    OutlinedTextField(
                        value = dueDayStr,
                        onValueChange = { if (it.length <= 2 && (it.toIntOrNull() ?: 0) <= 31) dueDayStr = it },
                        label = { Text("Nuovo giorno scadenza (1–31)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText = { Text("Mese e anno restano invariati", color = TextMuted) },
                        colors = tfColors
                    )
                }
            },
            confirmButton = {
                val newTotal = importoStr.replace(",", ".").toDoubleOrNull() ?: cedolino.total
                Button(
                    onClick = {
                        val originalDay = cal.get(Calendar.DAY_OF_MONTH)
                        val newDay      = dueDayStr.toIntOrNull()?.coerceIn(1, 31) ?: originalDay
                        val newDueCal   = Calendar.getInstance().apply {
                            timeInMillis = cedolino.dueDate
                            set(Calendar.DAY_OF_MONTH, newDay)
                        }
                        val updated = cedolino.copy(
                            period  = period.trim(),
                            dueDate = newDueCal.timeInMillis,
                            total   = newTotal
                        )
                        if (newDay != originalDay && sameUnitOthers.isNotEmpty()) {
                            // Passa allo step 2 per scegliere a chi propagare
                            pendingUpdated = updated
                            step = 2
                        } else {
                            onSave(updated, emptySet())
                        }
                    },
                    enabled = period.isNotBlank() && (importoStr.replace(",", ".").toDoubleOrNull() ?: 0.0) > 0,
                    colors  = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = DarkBg)
                ) {
                    Icon(Icons.Filled.Save, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Salva", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Annulla", color = TextSecondary) }
            }
        )
    } else {
        // ── Step 2: Propaga il nuovo giorno agli altri avvisi ──────────────────
        val newDay    = dueDayStr.toIntOrNull()?.coerceIn(1, 31) ?: cal.get(Calendar.DAY_OF_MONTH)
        val allSel    = propagateIds.size == sameUnitOthers.size
        AlertDialog(
            onDismissRequest = { /* usa i pulsanti */ },
            containerColor   = DarkSurface,
            icon  = { Icon(Icons.Filled.CalendarToday, null, tint = Cyan400) },
            title = {
                Text("Applica giorno $newDay agli altri avvisi?",
                    color = TextPrimary, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Vuoi applicare il giorno $newDay anche agli altri avvisi aperti di questo inquilino?",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    // Toggle tutti
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Cyan400.copy(alpha = 0.06f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = allSel,
                            onCheckedChange = {
                                propagateIds = if (allSel) emptySet()
                                              else sameUnitOthers.map { it.id }.toSet()
                            },
                            colors = CheckboxDefaults.colors(checkedColor = Cyan400)
                        )
                        Text(
                            if (allSel) "Deseleziona tutti" else "Seleziona tutti (${sameUnitOthers.size})",
                            style = MaterialTheme.typography.labelMedium, color = Cyan400,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    HorizontalDivider(color = TextMuted.copy(alpha = 0.12f))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        sameUnitOthers.forEach { ced ->
                            val checked = ced.id in propagateIds
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (checked) Cyan400.copy(alpha = 0.05f) else Color.Transparent)
                                    .clickable {
                                        propagateIds = if (checked) propagateIds - ced.id
                                                      else propagateIds + ced.id
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = {
                                        propagateIds = if (checked) propagateIds - ced.id
                                                       else propagateIds + ced.id
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = Cyan400)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ced.period,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (checked) TextPrimary else TextSecondary,
                                        fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal)
                                    Text("Scad. ${Formatters.date(ced.dueDate)} · €%.2f".format(ced.total),
                                        style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick  = { onSave(pendingUpdated!!, propagateIds) },
                    enabled  = propagateIds.isNotEmpty(),
                    colors   = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = DarkBg)
                ) {
                    Icon(Icons.Filled.CalendarToday, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Applica a ${propagateIds.size} avvis${if (propagateIds.size == 1) "o" else "i"}",
                        fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { onSave(pendingUpdated!!, emptySet()) }) {
                    Text("Solo questo avviso", color = TextSecondary)
                }
            }
        )
    }
}


// ─── Dialog: Registra Pagamento con Metodo ───────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistraPagamentoDialog(
    cedolino: SCedolino,
    unitName: String,
    onDismiss: () -> Unit,
    onConfirm: (method: String, reference: String) -> Unit
) {
    var selectedMethod by remember { mutableStateOf(com.renttrack.app.data.model.PaymentMethods.methods.first()) }
    var reference by remember { mutableStateOf("") }
    var methodExpanded by remember { mutableStateOf(false) }

    val methodIcons = mapOf(
        "Contanti" to "💵",
        "Bonifico" to "🏦",
        "Bollettino Postale" to "📮",
        "RID / Addebito diretto" to "🔄",
        "Assegno" to "📝"
    )

    AlertDialog(
        onDismissRequest = { /* blocca chiusura accidentale — usare Annulla */ },
        containerColor = com.renttrack.app.ui.theme.DarkSurface,
        icon = { Icon(Icons.Filled.CheckCircle, null, tint = Green400) },
        title = { Text("Registra pagamento", color = com.renttrack.app.ui.theme.TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Riepilogo cedolino
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Green500.copy(alpha = 0.08f), androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(unitName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = com.renttrack.app.ui.theme.TextPrimary)
                        Text("Periodo: ${cedolino.period}", style = MaterialTheme.typography.bodySmall, color = com.renttrack.app.ui.theme.TextSecondary)
                        Text(
                            Formatters.currency(cedolino.total),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = Green400
                        )
                    }
                }
                // Metodo di pagamento
                ExposedDropdownMenuBox(expanded = methodExpanded, onExpandedChange = { methodExpanded = it }) {
                    OutlinedTextField(
                        value = "${methodIcons[selectedMethod] ?: "💳"} $selectedMethod",
                        onValueChange = {}, readOnly = true, label = { Text("Metodo di pagamento") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(methodExpanded) }
                    )
                    ExposedDropdownMenu(expanded = methodExpanded, onDismissRequest = { methodExpanded = false }) {
                        com.renttrack.app.data.model.PaymentMethods.methods.forEach { method ->
                            DropdownMenuItem(
                                text = { Text("${methodIcons[method] ?: "💳"} $method", color = com.renttrack.app.ui.theme.TextPrimary) },
                                onClick = { selectedMethod = method; methodExpanded = false }
                            )
                        }
                    }
                }
                // Riferimento (numero bollettino, CRO bonifico, ecc.)
                OutlinedTextField(
                    reference, { reference = it },
                    label = { Text("Riferimento (opzionale)") },
                    placeholder = { Text("es. CRO, n° bollettino...", color = com.renttrack.app.ui.theme.TextMuted) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedMethod, reference.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = Green500)
            ) {
                Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Conferma pagamento", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla", color = com.renttrack.app.ui.theme.TextSecondary) } }
    )
}
