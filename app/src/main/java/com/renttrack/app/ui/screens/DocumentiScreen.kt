package com.renttrack.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.renttrack.app.data.model.Documento
import com.renttrack.app.data.model.DocumentCategories
import com.renttrack.app.data.model.FileTypes
import com.renttrack.app.ui.components.CategoryChip
import com.renttrack.app.ui.components.condoTextFieldColors
import com.renttrack.app.ui.theme.*
import com.renttrack.app.viewmodel.RentViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// Contract per aprire file di tipi multipli
class GetMultiTypeContent : ActivityResultContract<Array<String>, Pair<Uri, String>?>() {
    override fun createIntent(context: android.content.Context, input: Array<String>) =
        Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, input)
        }
    override fun parseResult(resultCode: Int, intent: Intent?): Pair<Uri, String>? {
        if (resultCode != Activity.RESULT_OK) return null
        val uri = intent?.data ?: return null
        val type = intent.type ?: ""
        return Pair(uri, type)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentiScreen(viewModel: RentViewModel) {
    val context = LocalContext.current
    val documenti by viewModel.documenti.collectAsState()
    val documentCount by viewModel.documentCount.collectAsState()
    val units by viewModel.units.collectAsState()

    var selectedCategoria by remember { mutableStateOf<String?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }
    var documentoToDelete by remember { mutableStateOf<Documento?>(null) }
    var documentoToEdit by remember { mutableStateOf<Documento?>(null) }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var pickedMimeType by remember { mutableStateOf("") }

    val documentiFiltrati = if (selectedCategoria == null) documenti
    else documenti.filter { it.categoria == selectedCategoria }

    val filePicker = rememberLauncherForActivityResult(GetMultiTypeContent()) { result ->
        result?.let { (uri, mime) ->
            pickedUri = uri
            pickedMimeType = mime.ifBlank {
                context.contentResolver.getType(uri) ?: ""
            }
            showAddSheet = true
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBg)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Archivio Documenti",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                    Text("$documentCount documento${if (documentCount != 1) "i" else ""}",
                        style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
                // Chip tipi file
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FileTypes.supported.forEach { ft ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = try { Color(android.graphics.Color.parseColor(ft.colorHex)) } catch (e: Exception) { Cyan400 }.copy(alpha = 0.15f)
                        ) {
                            Text(ft.icon, modifier = Modifier.padding(6.dp), fontSize = 16.sp)
                        }
                    }
                }
            }

            // ── Filtro Categorie ──────────────────────────────────────
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                item {
                    CategoryChip("Tutti", "📂", selectedCategoria == null, documenti.size, "#636E72") { selectedCategoria = null }
                }
                items(DocumentCategories.categories) { cat ->
                    CategoryChip(cat.name, cat.icon, selectedCategoria == cat.name,
                        documenti.count { it.categoria == cat.name }, cat.colorHex) {
                        selectedCategoria = if (selectedCategoria == cat.name) null else cat.name
                    }
                }
            }

            HorizontalDivider(color = DarkSurface, thickness = 1.dp)

            // ── Lista ──────────────────────────────────────────────────
            if (documentiFiltrati.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("📁", fontSize = 48.sp)
                        Text(
                            if (selectedCategoria == null) "Nessun documento ancora" else "Nessun documento in questa categoria",
                            color = TextMuted, style = MaterialTheme.typography.bodyMedium
                        )
                        if (selectedCategoria == null) {
                            FilledTonalButton(
                                onClick = { filePicker.launch(FileTypes.allMimeTypes) },
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Cyan400.copy(alpha = 0.15f), contentColor = Cyan400)
                            ) {
                                Icon(Icons.Filled.UploadFile, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Carica il primo documento")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(documentiFiltrati, key = { it.id }) { doc ->
                        DocumentCard(
                            documento = doc,
                            units = units,
                            onOpen = {
                                val file = File(doc.filePath)
                                if (file.exists()) {
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        context, "${context.packageName}.provider", file)
                                    val mimeType = when (doc.fileType) {
                                        "Word" -> "application/msword"
                                        "Foto" -> "image/*"
                                        else -> "application/pdf"
                                    }
                                    context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, mimeType)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                                    })
                                }
                            },
                            onEdit = { documentoToEdit = doc },
                            onDelete = { documentoToDelete = doc }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { filePicker.launch(FileTypes.allMimeTypes) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = Cyan400, contentColor = DarkBg
        ) { Icon(Icons.Filled.Add, "Aggiungi documento") }
    }


    if (showAddSheet && pickedUri != null) {
        AddDocumentoSheet(
            uri = pickedUri!!,
            mimeType = pickedMimeType,
            units = units,
            onDismiss = { showAddSheet = false; pickedUri = null },
            onConfirm = { titolo, categoria, note, sommario, visibilita, destinatariIds ->
                viewModel.addDocumento(pickedUri!!, titolo, categoria, note, pickedMimeType, sommario, visibilita, destinatariIds)
                showAddSheet = false; pickedUri = null
            }
        )
    }

    documentoToEdit?.let { doc ->
        EditDocumentoSheet(
            documento = doc,
            units = units,
            onDismiss = { documentoToEdit = null },
            onConfirm = { titolo, categoria, note, sommario, visibilita, destinatariIds ->
                viewModel.updateDocumento(doc.copy(
                    titolo = titolo, categoria = categoria, note = note,
                    sommario = sommario, visibilita = visibilita, destinatariUnitIds = destinatariIds
                ))
                documentoToEdit = null
            }
        )
    }

    documentoToDelete?.let { doc ->
        AlertDialog(
            onDismissRequest = { documentoToDelete = null },
            containerColor = DarkSurface,
            icon = { Icon(Icons.Filled.DeleteForever, null, tint = Color(0xFFFF6B6B)) },
            title = { Text("Elimina documento", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Eliminare \"${doc.titolo}\"?\nIl file verrà rimosso definitivamente.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteDocumento(doc); documentoToDelete = null }) {
                    Text("Elimina", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { documentoToDelete = null }) { Text("Annulla", color = TextSecondary) } }
        )
    }
}

@Composable
fun DocumentCard(
    documento: Documento,
    units: List<com.renttrack.app.data.model.CondoUnit> = emptyList(),
    onOpen: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit
) {
    val catColor = try { Color(android.graphics.Color.parseColor(DocumentCategories.getColorHex(documento.categoria))) }
    catch (e: Exception) { Cyan400 }
    val fileColor = try { Color(android.graphics.Color.parseColor(FileTypes.getColorHex(documento.fileType))) }
    catch (e: Exception) { Cyan400 }
    val dateStr = remember { SimpleDateFormat("dd MMM yyyy", Locale.ITALIAN).format(Date(documento.dataInserimento)) }

    // Calcola etichetta destinatari
    val destinatariLabel = remember(documento, units) {
        if (documento.visibilita == "Tutti") "🌐 Tutto il condominio"
        else {
            val ids = documento.destinatariUnitIds.split(",").mapNotNull { it.trim().toLongOrNull() }
            if (ids.isEmpty()) "🌐 Tutto il condominio"
            else {
                val nomi = ids.mapNotNull { id -> units.find { it.id == id }?.let { "Int.${it.number}" } }
                "👥 ${nomi.joinToString(", ").ifBlank { "${ids.size} unità" }}"
            }
        }
    }

    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, catColor.copy(alpha = 0.2f))
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Anteprima immagine o icona
                Surface(shape = RoundedCornerShape(12.dp), color = fileColor.copy(alpha = 0.15f), modifier = Modifier.size(56.dp)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        if (documento.fileType == "Foto" && File(documento.filePath).exists()) {
                            AsyncImage(
                                model = File(documento.filePath),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(FileTypes.getIcon(documento.fileType), fontSize = 22.sp)
                                Text(documento.fileType, fontSize = 8.sp, color = fileColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(documento.titolo,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(documento.categoria, style = MaterialTheme.typography.labelSmall, color = catColor)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(dateStr, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("•", color = TextMuted, fontSize = 8.sp)
                        Text(formatFileSize(documento.fileSize), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                    // Badge destinatari
                    Surface(shape = RoundedCornerShape(4.dp), color = if (documento.visibilita == "Tutti") Cyan400.copy(alpha = 0.1f) else Purple400.copy(alpha = 0.1f)) {
                        Text(
                            destinatariLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (documento.visibilita == "Tutti") Cyan400 else Purple400,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Espandi/collassa sommario se presente
                    if (documento.sommario.isNotBlank()) {
                        IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                "Sintesi",
                                tint = Amber400,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Edit, "Modifica", tint = Purple400, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onOpen, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.OpenInNew, "Apri", tint = Cyan400, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.Delete, "Elimina", tint = Color(0xFFFF6B6B), modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Sommario espandibile
            AnimatedVisibility(visible = expanded && documento.sommario.isNotBlank()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = Amber400.copy(alpha = 0.2f))
                    Row(modifier = Modifier.padding(12.dp)) {
                        Icon(Icons.Filled.Notes, null, tint = Amber400, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text("Sintesi", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Amber400)
                            Spacer(Modifier.height(2.dp))
                            Text(documento.sommario, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
            }

            if (documento.note.isNotBlank() && !expanded) {
                Text(
                    documento.note,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 84.dp, end = 14.dp, bottom = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDocumentoSheet(
    uri: Uri,
    mimeType: String,
    units: List<com.renttrack.app.data.model.CondoUnit> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (titolo: String, categoria: String, note: String, sommario: String, visibilita: String, destinatariIds: String) -> Unit
) {
    val detectedFileType = FileTypes.fromMimeType(mimeType)
    val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "documento"
    var titolo by remember { mutableStateOf(fileName.substringBeforeLast('.')) }
    var selectedCategoria by remember { mutableStateOf(DocumentCategories.names.first()) }
    var note by remember { mutableStateOf("") }
    var sommario by remember { mutableStateOf("") }
    var visibilita by remember { mutableStateOf("Tutti") }     // "Tutti" o "Singoli"
    var selectedUnitIds by remember { mutableStateOf(setOf<Long>()) }
    var showCategoriaMenu by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = DarkSurface) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Nuovo Documento",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
            }

            // Preview file
            item {
                Surface(shape = RoundedCornerShape(10.dp), color = Cyan400.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Cyan400.copy(alpha = 0.3f))) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(FileTypes.getIcon(detectedFileType), fontSize = 20.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(fileName, style = MaterialTheme.typography.bodySmall, color = TextSecondary,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(detectedFileType, style = MaterialTheme.typography.labelSmall, color = Cyan400)
                        }
                    }
                }
            }

            // Titolo
            item {
                OutlinedTextField(titolo, { titolo = it }, label = { Text("Titolo documento") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, colors = condoTextFieldColors())
            }

            // Categoria
            item {
                ExposedDropdownMenuBox(expanded = showCategoriaMenu, onExpandedChange = { showCategoriaMenu = it }) {
                    OutlinedTextField(
                        "${DocumentCategories.getIcon(selectedCategoria)} $selectedCategoria", {},
                        readOnly = true, label = { Text("Categoria") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showCategoriaMenu) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(), colors = condoTextFieldColors()
                    )
                    ExposedDropdownMenu(expanded = showCategoriaMenu, onDismissRequest = { showCategoriaMenu = false }) {
                        DocumentCategories.categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text(cat.icon); Text(cat.name, color = TextPrimary) } },
                                onClick = { selectedCategoria = cat.name; showCategoriaMenu = false },
                                colors = MenuDefaults.itemColors(textColor = TextPrimary, leadingIconColor = TextPrimary,
                                    trailingIconColor = TextPrimary, disabledTextColor = TextMuted,
                                    disabledLeadingIconColor = TextMuted, disabledTrailingIconColor = TextMuted)
                            )
                        }
                    }
                }
            }

            // Sommario (sintesi per il condomino)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Notes, null, tint = Amber400, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sintesi per il condomino",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Amber400)
                    }
                    Text("Il condomino vedrà questa sintesi prima di aprire il documento",
                        style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    OutlinedTextField(
                        sommario, { sommario = it },
                        label = { Text("Sintesi (opzionale)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2, maxLines = 4,
                        colors = condoTextFieldColors(),
                        placeholder = { Text("es. Verbale dell'assemblea del 10/05 - si delibera la sostituzione del cancello...", color = TextMuted) }
                    )
                }
            }

            // Note interne (per l'admin)
            item {
                OutlinedTextField(note, { note = it }, label = { Text("Note interne (solo admin)") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 3, colors = condoTextFieldColors())
            }

            // ── DESTINATARI ───────────────────────────────────────────
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Group, null, tint = Purple400, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Destinatari",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Purple400)
                    }

                    // Radio: Tutti / Singoli
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("Tutti" to "🌐 Tutto il condominio", "Singoli" to "👥 Unità specifiche").forEach { (value, label) ->
                            Surface(
                                onClick = {
                                    visibilita = value
                                    if (value == "Tutti") selectedUnitIds = emptySet()
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (visibilita == value) Purple400.copy(alpha = 0.15f) else DarkBg,
                                border = BorderStroke(1.dp, if (visibilita == value) Purple400 else TextMuted.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    RadioButton(
                                        selected = visibilita == value,
                                        onClick = null,
                                        colors = RadioButtonDefaults.colors(selectedColor = Purple400)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(label, style = MaterialTheme.typography.labelSmall,
                                        color = if (visibilita == value) Purple400 else TextMuted)
                                }
                            }
                        }
                    }

                    // Lista unità selezionabili (solo se "Singoli")
                    AnimatedVisibility(visible = visibilita == "Singoli" && units.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Seleziona le unità destinatarie:",
                                style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            val unitsByScala = units.groupBy { it.scala.ifBlank { "—" } }.toSortedMap()
                            unitsByScala.forEach { (scala, scalaUnits) ->
                                if (unitsByScala.size > 1) {
                                    Text(
                                        if (scala == "—") "Senza scala" else scala,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Cyan400,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                scalaUnits.sortedBy { it.floor }.forEach { unit ->
                                    val isChecked = unit.id in selectedUnitIds
                                    Surface(
                                        onClick = {
                                            selectedUnitIds = if (isChecked)
                                                selectedUnitIds - unit.id
                                            else
                                                selectedUnitIds + unit.id
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isChecked) Purple400.copy(alpha = 0.1f) else DarkBg,
                                        border = BorderStroke(1.dp, if (isChecked) Purple400.copy(alpha = 0.5f) else TextMuted.copy(alpha = 0.2f))
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = null,
                                                colors = CheckboxDefaults.colors(checkedColor = Purple400)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                "Int. ${unit.number} — ${unit.ownerName}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isChecked) TextPrimary else TextSecondary
                                            )
                                            Spacer(Modifier.weight(1f))
                                            Text("P${unit.floor}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottoni conferma
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, TextMuted.copy(alpha = 0.4f))) {
                        Text("Annulla", color = TextSecondary)
                    }
                    Button(
                        onClick = {
                            val destIds = if (visibilita == "Singoli")
                                selectedUnitIds.joinToString(",")
                            else ""
                            onConfirm(
                                titolo.trim().ifBlank { fileName },
                                selectedCategoria,
                                note.trim(),
                                sommario.trim(),
                                visibilita,
                                destIds
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = DarkBg),
                        enabled = titolo.isNotBlank() && (visibilita == "Tutti" || selectedUnitIds.isNotEmpty())
                    ) {
                        Icon(Icons.Filled.Save, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Salva", fontWeight = FontWeight.Bold)
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}

// ─── Sheet Modifica Documento ──────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDocumentoSheet(
    documento: Documento,
    units: List<com.renttrack.app.data.model.CondoUnit> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (titolo: String, categoria: String, note: String, sommario: String, visibilita: String, destinatariIds: String) -> Unit
) {
    var titolo by remember { mutableStateOf(documento.titolo) }
    var selectedCategoria by remember { mutableStateOf(documento.categoria) }
    var note by remember { mutableStateOf(documento.note) }
    var sommario by remember { mutableStateOf(documento.sommario) }
    var visibilita by remember { mutableStateOf(documento.visibilita) }
    var selectedUnitIds by remember {
        mutableStateOf(documento.destinatariUnitIds.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet())
    }
    var showCategoriaMenu by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = DarkSurface) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Edit, null, tint = Purple400, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Modifica Documento",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                }
            }

            // File info (non modificabile)
            item {
                Surface(shape = RoundedCornerShape(10.dp), color = DarkBg, border = BorderStroke(1.dp, TextMuted.copy(alpha = 0.2f))) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(FileTypes.getIcon(documento.fileType), fontSize = 20.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(documento.fileName, style = MaterialTheme.typography.bodySmall, color = TextSecondary,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("File non modificabile", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                    }
                }
            }

            item {
                OutlinedTextField(titolo, { titolo = it }, label = { Text("Titolo documento") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, colors = condoTextFieldColors())
            }

            item {
                ExposedDropdownMenuBox(expanded = showCategoriaMenu, onExpandedChange = { showCategoriaMenu = it }) {
                    OutlinedTextField(
                        "${DocumentCategories.getIcon(selectedCategoria)} $selectedCategoria", {},
                        readOnly = true, label = { Text("Categoria") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showCategoriaMenu) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(), colors = condoTextFieldColors()
                    )
                    ExposedDropdownMenu(expanded = showCategoriaMenu, onDismissRequest = { showCategoriaMenu = false }) {
                        DocumentCategories.categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text(cat.icon); Text(cat.name, color = TextPrimary) } },
                                onClick = { selectedCategoria = cat.name; showCategoriaMenu = false },
                                colors = MenuDefaults.itemColors(textColor = TextPrimary, leadingIconColor = TextPrimary,
                                    trailingIconColor = TextPrimary, disabledTextColor = TextMuted,
                                    disabledLeadingIconColor = TextMuted, disabledTrailingIconColor = TextMuted)
                            )
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Notes, null, tint = Amber400, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sintesi per il condomino", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = Amber400)
                    }
                    OutlinedTextField(sommario, { sommario = it }, label = { Text("Sintesi (opzionale)") },
                        modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4, colors = condoTextFieldColors())
                }
            }

            item {
                OutlinedTextField(note, { note = it }, label = { Text("Note interne (solo admin)") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 3, colors = condoTextFieldColors())
            }

            // Destinatari
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Group, null, tint = Purple400, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Destinatari", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = Purple400)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("Tutti" to "🌐 Tutto il condominio", "Singoli" to "👥 Unità specifiche").forEach { (value, label) ->
                            Surface(
                                onClick = { visibilita = value; if (value == "Tutti") selectedUnitIds = emptySet() },
                                shape = RoundedCornerShape(10.dp),
                                color = if (visibilita == value) Purple400.copy(alpha = 0.15f) else DarkBg,
                                border = BorderStroke(1.dp, if (visibilita == value) Purple400 else TextMuted.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                    RadioButton(selected = visibilita == value, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = Purple400))
                                    Spacer(Modifier.width(4.dp))
                                    Text(label, style = MaterialTheme.typography.labelSmall, color = if (visibilita == value) Purple400 else TextMuted)
                                }
                            }
                        }
                    }
                    if (visibilita == "Singoli" && units.isNotEmpty()) {
                        units.sortedBy { it.number }.forEach { unit ->
                            val isChecked = unit.id in selectedUnitIds
                            Surface(
                                onClick = { selectedUnitIds = if (isChecked) selectedUnitIds - unit.id else selectedUnitIds + unit.id },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isChecked) Purple400.copy(alpha = 0.1f) else DarkBg,
                                border = BorderStroke(1.dp, if (isChecked) Purple400.copy(alpha = 0.5f) else TextMuted.copy(alpha = 0.2f))
                            ) {
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = isChecked, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = Purple400))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Int. ${unit.number} — ${unit.ownerName}", style = MaterialTheme.typography.bodySmall, color = if (isChecked) TextPrimary else TextSecondary)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, TextMuted.copy(alpha = 0.4f))) {
                        Text("Annulla", color = TextSecondary)
                    }
                    Button(
                        onClick = {
                            val destIds = if (visibilita == "Singoli") selectedUnitIds.joinToString(",") else ""
                            onConfirm(titolo.trim().ifBlank { documento.titolo }, selectedCategoria, note.trim(), sommario.trim(), visibilita, destIds)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Purple400, contentColor = DarkBg),
                        enabled = titolo.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Save, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Salva modifiche", fontWeight = FontWeight.Bold)
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
