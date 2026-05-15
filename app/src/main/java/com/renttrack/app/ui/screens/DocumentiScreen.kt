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
import androidx.compose.foundation.lazy.grid.*
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
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.renttrack.app.data.model.SDocumento
import com.renttrack.app.data.model.SCondoUnit
import com.renttrack.app.data.model.DocumentCategories
import com.renttrack.app.data.model.FileTypes
import com.renttrack.app.ui.components.CategoryChip
import com.renttrack.app.ui.components.condoTextFieldColors
import com.renttrack.app.ui.theme.*
import com.renttrack.app.viewmodel.SupabaseRentViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// ─── Helper: URL pubblico Supabase Storage ────────────────────────────────────
// I documenti sono salvati su Supabase Storage (non in locale).
// filePath contiene il path relativo es. "condoId/uuid_nome.pdf"
private const val SUPABASE_DOCS_URL =
    "https://zjqrtuposdrimzjoydgh.supabase.co/storage/v1/object/public/documenti/"

private fun documentoPublicUrl(filePath: String): String =
    if (filePath.startsWith("http")) filePath else "$SUPABASE_DOCS_URL$filePath"

enum class DocSortOrder(val label: String, val icon: String) {
    DATE_DESC("Più recenti", "🕐"),
    DATE_ASC("Più vecchi", "🕰"),
    NAME_ASC("Nome A→Z", "🔤"),
    NAME_DESC("Nome Z→A", "🔡"),
    TYPE("Tipo file", "📄")
}

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
fun DocumentiScreen(viewModel: SupabaseRentViewModel) {
    val context = LocalContext.current
    val documenti by viewModel.documenti.collectAsState()
    val documentCount by viewModel.documentCount.collectAsState()
    val units by viewModel.units.collectAsState()

    var selectedCategoria by remember { mutableStateOf<String?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }
    var documentoToDelete by remember { mutableStateOf<SDocumento?>(null) }
    var documentoToEdit by remember { mutableStateOf<SDocumento?>(null) }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var pickedMimeType by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(DocSortOrder.DATE_DESC) }
    var showSortMenu by remember { mutableStateOf(false) }
    var searchActive by remember { mutableStateOf(false) }
    var gridMode by remember { mutableStateOf(false) }
    var photoViewer by remember { mutableStateOf<SDocumento?>(null) }
    var selectedUnitFilter by remember { mutableStateOf<String?>(null) }

    val documentiFiltrati = documenti
        .filter { doc ->
            (selectedCategoria == null || doc.categoria == selectedCategoria) &&
            (searchQuery.isBlank() || doc.titolo.contains(searchQuery, ignoreCase = true) ||
             doc.note.contains(searchQuery, ignoreCase = true) ||
             doc.categoria.contains(searchQuery, ignoreCase = true) ||
             doc.sommario.contains(searchQuery, ignoreCase = true)) &&
            (selectedUnitFilter == null || doc.visibilita == "Tutti" ||
             doc.destinatariUnitIds.split(",").map { it.trim() }.contains(selectedUnitFilter))
        }
        .let { list ->
            when (sortOrder) {
                DocSortOrder.DATE_DESC -> list.sortedByDescending { it.createdAt }
                DocSortOrder.DATE_ASC  -> list.sortedBy { it.createdAt }
                DocSortOrder.NAME_ASC  -> list.sortedBy { it.titolo.lowercase() }
                DocSortOrder.NAME_DESC -> list.sortedByDescending { it.titolo.lowercase() }
                DocSortOrder.TYPE      -> list.sortedBy { it.fileType }
            }
        }

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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Archivio Documenti",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                    Text(
                        if (searchQuery.isBlank()) "$documentCount documento${if (documentCount != 1) "i" else ""}"
                        else "${documentiFiltrati.size} risultati su $documentCount",
                        style = MaterialTheme.typography.bodySmall, color = TextMuted
                    )
                }
                // Bottone vista griglia/lista
                IconButton(onClick = { gridMode = !gridMode }) {
                    Icon(
                        if (gridMode) Icons.Filled.ViewList else Icons.Filled.GridView,
                        "Cambia vista",
                        tint = if (gridMode) Cyan400 else TextMuted
                    )
                }
                // Bottone cerca
                IconButton(onClick = { searchActive = !searchActive; if (!searchActive) searchQuery = "" }) {
                    Icon(
                        if (searchActive) Icons.Filled.SearchOff else Icons.Filled.Search,
                        "Cerca",
                        tint = if (searchActive) Cyan400 else TextMuted
                    )
                }
                // Bottone ordinamento
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Filled.SortByAlpha, "Ordina", tint = if (sortOrder != DocSortOrder.DATE_DESC) Cyan400 else TextMuted)
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        Text(
                            "Ordina per",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        DocSortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(order.icon)
                                        Text(order.label, color = if (sortOrder == order) Cyan400 else TextPrimary)
                                    }
                                },
                                trailingIcon = {
                                    if (sortOrder == order) Icon(Icons.Filled.Check, null, tint = Cyan400, modifier = Modifier.size(16.dp))
                                },
                                onClick = { sortOrder = order; showSortMenu = false }
                            )
                        }
                    }
                }
            }

            // ── Barra di ricerca (espandibile) ───────────────────────
            AnimatedVisibility(visible = searchActive) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cerca per titolo, categoria, note...", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = Cyan400) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, null, tint = TextMuted)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    colors = condoTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
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

            // ── Filtro Inquilini ──────────────────────────────────────
            if (units.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    item {
                        // Chip "Tutti gli inquilini"
                        FilterChip(
                            selected = selectedUnitFilter == null,
                            onClick = { selectedUnitFilter = null },
                            label = {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("👥", fontSize = 12.sp)
                                    Text("Tutti", style = MaterialTheme.typography.labelSmall)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Cyan400.copy(alpha = 0.2f),
                                selectedLabelColor = Cyan400,
                                containerColor = DarkSurface,
                                labelColor = TextMuted
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedUnitFilter == null,
                                selectedBorderColor = Cyan400.copy(alpha = 0.4f),
                                borderColor = TextMuted.copy(alpha = 0.2f)
                            )
                        )
                    }
                    items(units.sortedBy { it.number }) { unit ->
                        val docCount = documenti.count { doc ->
                            doc.visibilita == "Tutti" ||
                            doc.destinatariUnitIds.split(",").map { it.trim() }.contains(unit.id)
                        }
                        val isSelected = selectedUnitFilter == unit.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedUnitFilter = if (isSelected) null else unit.id },
                            label = {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("👤", fontSize = 12.sp)
                                    Text(
                                        "${unit.ownerName.split(" ").first()} · ${unit.number}",
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                    if (docCount > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) Cyan400.copy(alpha = 0.3f) else TextMuted.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                "$docCount",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSelected) Cyan400 else TextMuted,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Cyan400.copy(alpha = 0.15f),
                                selectedLabelColor = Cyan400,
                                containerColor = DarkSurface,
                                labelColor = TextMuted
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                selectedBorderColor = Cyan400.copy(alpha = 0.4f),
                                borderColor = TextMuted.copy(alpha = 0.2f)
                            )
                        )
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
                            when {
                                selectedUnitFilter != null -> "Nessun documento per questo inquilino"
                                selectedCategoria != null -> "Nessun documento in questa categoria"
                                else -> "Nessun documento ancora"
                            },
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
                if (gridMode) {
                    // ── Vista griglia 2 colonne ─────────────────────
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(documentiFiltrati, key = { it.id }) { doc ->
                            DocumentGridCard(
                                documento = doc,
                                onOpen = {
                                    if (doc.fileType == "Foto" || doc.fileType.startsWith("image/")) {
                                        photoViewer = doc
                                    } else {
                                        val url = documentoPublicUrl(doc.filePath)
                                        val mimeType = when (doc.fileType) {
                                            "Word" -> "application/msword"
                                            "PDF"  -> "application/pdf"
                                            else   -> doc.fileType.ifBlank { "application/octet-stream" }
                                        }
                                        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(Uri.parse(url), mimeType)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        })
                                    }
                                },
                                onEdit = { documentoToEdit = doc },
                                onDelete = { documentoToDelete = doc }
                            )
                        }
                    }
                } else {
                    // ── Vista lista ─────────────────────────────
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(documentiFiltrati, key = { it.id }) { doc ->
                            DocumentCard(
                                documento = doc,
                                units = units,
                                onOpen = {
                                    if (doc.fileType == "Foto" || doc.fileType.startsWith("image/")) {
                                        photoViewer = doc
                                    } else {
                                        val url = documentoPublicUrl(doc.filePath)
                                        val mimeType = when (doc.fileType) {
                                            "Word" -> "application/msword"
                                            "PDF"  -> "application/pdf"
                                            else   -> doc.fileType.ifBlank { "application/octet-stream" }
                                        }
                                        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(Uri.parse(url), mimeType)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        })
                                    }
                                },
                                onEdit = { documentoToEdit = doc },
                                onDelete = { documentoToDelete = doc },
                                onShare = {
                                    val url = documentoPublicUrl(doc.filePath)
                                    context.startActivity(Intent.createChooser(
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, url)
                                            putExtra(Intent.EXTRA_SUBJECT, doc.titolo)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }, "Condividi ${doc.titolo}"
                                    ))
                                }
                            )
                        }
                    }
                }
            }
        }

        // FAB aggiungi documento
        FloatingActionButton(
            onClick = { filePicker.launch(FileTypes.allMimeTypes) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .zIndex(5f),
            containerColor = Cyan400, contentColor = DarkBg
        ) { Icon(Icons.Filled.Add, "Aggiungi documento") }

        // ── Photo Viewer fullscreen overlay (dentro il Box principale — nessun Dialog) ──
        AnimatedVisibility(
            visible = photoViewer != null,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val doc = photoViewer
            if (doc != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    // Immagine
                    AsyncImage(
                        model = documentoPublicUrl(doc.filePath),
                        contentDescription = doc.titolo,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Gradiente + header in alto
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .fillMaxWidth()
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)
                                )
                            )
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { photoViewer = null }) {
                            Icon(Icons.Filled.ArrowBack, "Chiudi", tint = Color.White)
                        }
                        Text(
                            doc.titolo,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Info in basso
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                                )
                            )
                            .navigationBarsPadding()
                            .padding(16.dp)
                    ) {
                        Text(doc.categoria, style = MaterialTheme.typography.labelMedium, color = Cyan400)
                        if (doc.sommario.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(doc.sommario, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                        }
                        if (doc.note.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(doc.note, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }

    // FAB aggiunto all'overlay Box qui sopra

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
    documento: SDocumento,
    units: List<SCondoUnit> = emptyList(),
    onOpen: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit,
    onShare: (() -> Unit)? = null
) {
    val catColor = try { Color(android.graphics.Color.parseColor(DocumentCategories.getColorHex(documento.categoria))) }
    catch (e: Exception) { Cyan400 }
    val fileColor = try { Color(android.graphics.Color.parseColor(FileTypes.getColorHex(documento.fileType))) }
    catch (e: Exception) { Cyan400 }
    val dateStr = remember { documento.createdAt.ifBlank { "—" } }

    // Calcola etichetta destinatari
    val destinatariLabel = remember(documento, units) {
        if (documento.visibilita == "Tutti") "🌐 Tutti gli inquilini"
        else {
            val ids = documento.destinatariUnitIds.split(",").map { it.trim() }.filter { it.isNotBlank() }
            if (ids.isEmpty()) "🌐 Tutti gli inquilini"
            else {
                val nomi = ids.mapNotNull { id -> units.find { it.id == id }?.let { "Int.${it.number}" } }
                "👥 ${nomi.joinToString(", ").ifBlank { "${ids.size} inquilini" }}"
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
                        if ((documento.fileType == "Foto" || documento.fileType.startsWith("image/")) && documento.filePath.isNotBlank()) {
                            AsyncImage(
                                model = documentoPublicUrl(documento.filePath),
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
                    if (onShare != null) {
                        IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.Share, "Condividi", tint = Green400, modifier = Modifier.size(20.dp))
                        }
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

// ─── Card compatta per vista GRIGLIA ─────────────────────────────────
@Composable
fun DocumentGridCard(
    documento: SDocumento,
    onOpen: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit
) {
    val catColor = try { Color(android.graphics.Color.parseColor(DocumentCategories.getColorHex(documento.categoria))) }
    catch (e: Exception) { Cyan400 }
    val fileColor = try { Color(android.graphics.Color.parseColor(FileTypes.getColorHex(documento.fileType))) }
    catch (e: Exception) { Cyan400 }
    val isPhoto = (documento.fileType == "Foto" || documento.fileType.startsWith("image/")) && documento.filePath.isNotBlank()

    Surface(
        onClick = onOpen,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f),
        shape = RoundedCornerShape(14.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, catColor.copy(alpha = 0.25f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background: foto reale o icona
            if (isPhoto) {
                AsyncImage(
                    model = documentoPublicUrl(documento.filePath),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Gradiente in basso per leggibilità testo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.55f)
                        .align(Alignment.BottomCenter)
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                )
            } else {
                // Icona centrata
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(fileColor.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(FileTypes.getIcon(documento.fileType), fontSize = 36.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(documento.fileType, fontSize = 10.sp, color = fileColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Categoria badge in alto
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                shape = RoundedCornerShape(6.dp),
                color = catColor.copy(alpha = 0.85f)
            ) {
                Text(
                    documento.categoria,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Azioni rapide in alto a destra
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Surface(shape = CircleShape, color = DarkSurface.copy(alpha = 0.8f)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Edit, null, tint = Purple400, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.width(4.dp))
                Surface(shape = CircleShape, color = DarkSurface.copy(alpha = 0.8f)) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Delete, null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Titolo in basso
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    documento.titolo,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isPhoto) Color.White else TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    documento.createdAt.ifBlank { "—" },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isPhoto) Color.White.copy(alpha = 0.7f) else TextMuted
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
    units: List<SCondoUnit> = emptyList(),
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
    var selectedUnitIds by remember { mutableStateOf(setOf<String>()) }
    var showCategoriaMenu by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = { /* blocca chiusura accidentale — usare Annulla */ }, containerColor = DarkSurface) {
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
                        Text("Note aggiuntive",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Amber400)
                    }
                    Text("Informazioni extra sul documento (opzionale)",
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
                OutlinedTextField(note, { note = it }, label = { Text("Note interne (solo proprietario)") },
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
                        listOf("Tutti" to "🌐 Tutti gli inquilini", "Singoli" to "👥 Inquilini specifici").forEach { (value, label) ->
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
                            Text("Seleziona gli inquilini destinatari:",
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
                                                "${unit.ownerName} — ${unit.number}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isChecked) TextPrimary else TextSecondary
                                            )
                                            Spacer(Modifier.weight(1f))
                                            
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
                        enabled = titolo.isNotBlank()
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
    documento: SDocumento,
    units: List<SCondoUnit> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (titolo: String, categoria: String, note: String, sommario: String, visibilita: String, destinatariIds: String) -> Unit
) {
    var titolo by remember { mutableStateOf(documento.titolo) }
    var selectedCategoria by remember { mutableStateOf(documento.categoria) }
    var note by remember { mutableStateOf(documento.note) }
    var sommario by remember { mutableStateOf(documento.sommario) }
    var visibilita by remember { mutableStateOf(documento.visibilita) }
    var selectedUnitIds by remember {
        mutableStateOf(documento.destinatariUnitIds.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet())
    }
    var showCategoriaMenu by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = { /* blocca chiusura accidentale — usare Annulla */ }, containerColor = DarkSurface) {
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
                        Text("Note aggiuntive", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = Amber400)
                    }
                    OutlinedTextField(sommario, { sommario = it }, label = { Text("Sintesi (opzionale)") },
                        modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4, colors = condoTextFieldColors())
                }
            }

            item {
                OutlinedTextField(note, { note = it }, label = { Text("Note interne (solo proprietario)") },
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
                        listOf("Tutti" to "🌐 Tutti gli inquilini", "Singoli" to "👥 Inquilini specifici").forEach { (value, label) ->
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
                                    Text("${unit.ownerName} — ${unit.number}", style = MaterialTheme.typography.bodySmall, color = if (isChecked) TextPrimary else TextSecondary)
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
