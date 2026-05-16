package com.renttrack.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.renttrack.app.data.model.Listing
import com.renttrack.app.ui.theme.*
import com.renttrack.app.viewmodel.ListingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MieiAnnunciScreen(
    viewModel: ListingsViewModel,
    onCreaAnnuncio: () -> Unit,
    onRichieste: () -> Unit,
    onBack: () -> Unit
) {
    val myListings  by viewModel.myListings.collectAsState()
    val isLoading   by viewModel.myListingsLoading.collectAsState()
    val toast       by viewModel.toast.collectAsState()
    val myInquiries by viewModel.myInquiries.collectAsState()
    val unreadCount by viewModel.unreadInquiriesCount.collectAsState()
    var toDelete by remember { mutableStateOf<Listing?>(null) }
    var toEdit   by remember { mutableStateOf<Listing?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadMyListings()
        viewModel.loadMyInquiries()
    }

    toast?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(2500)
            viewModel.clearToast()
        }
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("I miei annunci", color = TextPrimary, fontWeight = FontWeight.Bold)
                        if (myListings.isNotEmpty()) {
                            val attivi = myListings.count { it.isActive }
                            Text(
                                "$attivi di ${myListings.size} visibili in vetrina",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (attivi > 0) Cyan400 else TextMuted
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Indietro", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg),
                actions = {
                    // Richieste con badge solo per quelle non ancora viste
                    BadgedBox(
                        badge = {
                            if (unreadCount > 0) {
                                Badge(containerColor = Red400) {
                                    Text(
                                        "$unreadCount",
                                        color = androidx.compose.ui.graphics.Color.White,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        },
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        IconButton(onClick = {
                            viewModel.markInquiriesAsRead()
                            onRichieste()
                        }) {
                            Icon(Icons.Filled.MarkEmailUnread, "Richieste ricevute", tint = if (unreadCount > 0) Red400 else Cyan400)
                        }
                    }
                    // Nuovo annuncio
                    FilledTonalButton(
                        onClick = onCreaAnnuncio,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Cyan400.copy(alpha = 0.15f),
                            contentColor = Cyan400
                        ),
                        modifier = Modifier.padding(end = 8.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Nuovo", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Cyan400)
                            Spacer(Modifier.height(12.dp))
                            Text("Caricamento annunci…", color = TextMuted,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                myListings.isEmpty() -> {
                    // Empty state
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(Cyan400.copy(alpha = 0.15f), Color.Transparent)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🏠", fontSize = 44.sp)
                        }
                        Text(
                            "Nessun annuncio ancora",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )
                        Text(
                            "Pubblica il tuo primo annuncio\nper trovare inquilini rapidamente",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = onCreaAnnuncio,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Cyan400, contentColor = DarkBg
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Pubblica il primo annuncio", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Tip contestuale
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Cyan400.copy(alpha = 0.07f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.Lightbulb, null,
                                        tint = Cyan400, modifier = Modifier.size(16.dp))
                                    Text(
                                        "Usa il toggle per mostrare o nascondere ogni annuncio dalla vetrina.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        items(myListings, key = { it.id }) { listing ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + slideInVertically()
                            ) {
                                MyListingCard(
                                    listing = listing,
                                    onToggleActive = {
                                        viewModel.toggleActive(listing.id, listing.isActive)
                                    },
                                    onToggleAvailable = {
                                        viewModel.toggleAvailable(listing.id, listing.isAvailable)
                                    },
                                    onToggleFeatured = {
                                        viewModel.toggleFeatured(listing.id, listing.isFeatured)
                                    },
                                    onDelete = { toDelete = listing },
                                    onEdit   = { toEdit = listing }
                                )
                            }
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }

            // Toast
            AnimatedVisibility(
                visible = toast != null,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                toast?.let { msg ->
                    val isError = msg.contains("eliminato") || msg.contains("Errore")
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isError) Red400.copy(alpha = 0.15f)
                               else Green400.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp,
                            if (isError) Red400.copy(alpha = 0.3f)
                            else Green400.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                if (isError) Icons.Filled.Info else Icons.Filled.CheckCircle,
                                null,
                                tint = if (isError) Red400 else Green400,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(msg,
                                color = if (isError) Red400 else Green400,
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    // Dialog conferma elimina
    toDelete?.let { listing ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            containerColor = DarkSurface,
            icon = { Icon(Icons.Filled.DeleteForever, null, tint = Red400) },
            title = {
                Text("Elimina annuncio", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Stai per eliminare:",
                        color = TextMuted, style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "\"${listing.title}\"",
                        color = TextPrimary, fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Red400.copy(alpha = 0.08f)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Warning, null,
                                tint = Red400, modifier = Modifier.size(14.dp))
                            Text(
                                "L'annuncio sarà rimosso definitivamente dalla vetrina.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Red400
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteListing(listing.id)
                        toDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red400)
                ) { Text("Elimina", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) {
                    Text("Annulla", color = TextSecondary)
                }
            }
        )
    }

    // Edit bottom sheet
    toEdit?.let { listing ->
        EditListingSheet(
            listing  = listing,
            viewModel = viewModel,
            onDismiss = { toEdit = null }
        )
    }
}

// ─── Card annuncio riprogettata ──────────────────────────────────────────────
@Composable
private fun MyListingCard(
    listing: Listing,
    onToggleActive: () -> Unit,
    onToggleAvailable: () -> Unit,
    onToggleFeatured: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    // Animazione colore bordo in base allo stato
    val borderColor by animateColorAsState(
        targetValue = when {
            listing.isActive && listing.isAvailable -> Cyan400.copy(alpha = 0.35f)
            listing.isActive                        -> Amber400.copy(alpha = 0.35f)
            else                                    -> Color(0xFF2D3748)
        },
        animationSpec = tween(300),
        label = "border"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = DarkSurface,
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Column {
            // ── Riga principale: foto + info + overflow ──────────────────
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Foto copertina con badge stato
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceVariant)
                ) {
                    if (listing.coverUrl.isNotBlank()) {
                        AsyncImage(
                            model = listing.coverUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("🏠", fontSize = 30.sp)
                        }
                    }
                    // Overlay semi-trasparente se inattivo
                    if (!listing.isActive) {
                        Box(
                            Modifier.fillMaxSize()
                                .background(DarkBg.copy(alpha = 0.65f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "NASCOSTO",
                                color = TextMuted,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 8.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                    // Badge featured
                    if (listing.isFeatured) {
                        Box(
                            Modifier
                                .align(Alignment.TopStart)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Cyan400.copy(alpha = 0.9f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("⭐", fontSize = 9.sp)
                        }
                    }
                }

                // Info annuncio
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        listing.title,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "${listing.city}${if (listing.zone.isNotBlank()) " · ${listing.zone}" else ""}",
                        color = TextMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                    if (listing.address.isNotBlank()) {
                        Text(
                            listing.address,
                            color = TextMuted.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Text(
                        "€${listing.priceMonthly.toInt()}/mese",
                        color = Cyan400,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )

                    // Badge occupato/libero
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (listing.isAvailable) Green400.copy(alpha = 0.12f)
                               else Amber400.copy(alpha = 0.12f)
                    ) {
                        Text(
                            if (listing.isAvailable) "🔓 Disponibile" else "🔒 Occupato",
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            color = if (listing.isAvailable) Green400 else Amber400,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Menu overflow (3 puntini)
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Filled.MoreVert, "Altre azioni",
                            tint = TextMuted, modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        // Libero / Occupato
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (listing.isAvailable) "Segna come occupato"
                                    else "Segna come disponibile",
                                    color = TextPrimary
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    if (listing.isAvailable) Icons.Filled.Lock
                                    else Icons.Filled.LockOpen,
                                    null,
                                    tint = if (listing.isAvailable) Amber400 else Green400
                                )
                            },
                            onClick = { showMenu = false; onToggleAvailable() }
                        )
                        // Featured
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (listing.isFeatured) "Rimuovi da \"In evidenza\""
                                    else "Metti in evidenza ⭐",
                                    color = TextPrimary
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Star, null,
                                    tint = if (listing.isFeatured) Cyan400 else TextMuted
                                )
                            },
                            onClick = { showMenu = false; onToggleFeatured() }
                        )
                        // Modifica
                        DropdownMenuItem(
                            text = { Text("Modifica annuncio", color = TextPrimary) },
                            leadingIcon = {
                                Icon(Icons.Filled.Edit, null, tint = Cyan400)
                            },
                            onClick = { showMenu = false; onEdit() }
                        )
                        HorizontalDivider(color = TextMuted.copy(alpha = 0.15f))
                        // Elimina
                        DropdownMenuItem(
                            text = { Text("Elimina annuncio", color = Red400) },
                            leadingIcon = {
                                Icon(Icons.Filled.DeleteForever, null, tint = Red400)
                            },
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                }
            }

            // ── Toggle visibilità in vetrina — AZIONE PRINCIPALE ────────────
            HorizontalDivider(
                color = TextMuted.copy(alpha = 0.08f),
                modifier = Modifier.padding(horizontal = 14.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleActive)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        if (listing.isActive) Icons.Filled.Visibility
                        else Icons.Filled.VisibilityOff,
                        contentDescription = null,
                        tint = if (listing.isActive) Cyan400 else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            if (listing.isActive) "Visibile in vetrina"
                            else "Nascosto dalla vetrina",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (listing.isActive) Cyan400 else TextMuted
                        )
                        Text(
                            if (listing.isActive) "Tocca per nascondere"
                            else "Tocca per pubblicare",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted.copy(alpha = 0.6f)
                        )
                    }
                }

                // Switch visuale
                Switch(
                    checked = listing.isActive,
                    onCheckedChange = { onToggleActive() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DarkBg,
                        checkedTrackColor = Cyan400,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceVariant
                    )
                )
            }
        }
    }
}

// ─── Bottom sheet modifica annuncio ─────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditListingSheet(
    listing: Listing,
    viewModel: ListingsViewModel,
    onDismiss: () -> Unit
) {
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    var title         by remember { mutableStateOf(listing.title) }
    var address        by remember { mutableStateOf(listing.address) }
    var city           by remember { mutableStateOf(listing.city) }
    var zone          by remember { mutableStateOf(listing.zone) }
    var price         by remember { mutableStateOf(listing.priceMonthly.toInt().toString()) }
    var description   by remember { mutableStateOf(listing.description) }
    var availableFrom by remember { mutableStateOf(listing.availableFrom) }
    var newPhotoUris  by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    var existingPhotos by remember { mutableStateOf(listing.photos) }
    val MAX_PHOTOS = 10
    val totalPhotos = existingPhotos.size + newPhotoUris.size

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris -> newPhotoUris = newPhotoUris + uris }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Modifica annuncio",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            HorizontalDivider(color = TextMuted.copy(alpha = 0.15f))

            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor    = Cyan400,
                unfocusedBorderColor  = androidx.compose.ui.graphics.Color(0xFF2D3748),
                focusedLabelColor     = Cyan400,
                unfocusedLabelColor   = TextMuted,
                focusedTextColor      = TextPrimary,
                unfocusedTextColor    = TextPrimary,
                cursorColor           = Cyan400
            )
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Titolo annuncio") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = fieldColors, singleLine = true
            )
            OutlinedTextField(
                value = address, onValueChange = { address = it },
                label = { Text("Via / Indirizzo") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = fieldColors, singleLine = true,
                placeholder = { androidx.compose.material3.Text("es. Via Roma 12", color = TextMuted) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = city, onValueChange = { city = it },
                    label = { Text("Città *") },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                    colors = fieldColors, singleLine = true,
                    placeholder = { androidx.compose.material3.Text("es. Roma", color = TextMuted) }
                )
                OutlinedTextField(
                    value = zone, onValueChange = { zone = it },
                    label = { Text("Zona / Quartiere") },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                    colors = fieldColors, singleLine = true,
                    placeholder = { androidx.compose.material3.Text("es. Prati", color = TextMuted) }
                )
            }
            OutlinedTextField(
                value = price, onValueChange = { price = it.filter { c -> c.isDigit() } },
                label = { Text("Prezzo mensile (€)") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = fieldColors, singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                )
            )
            OutlinedTextField(
                value = availableFrom, onValueChange = { availableFrom = it },
                label = { Text("Disponibile dal (es. 2024-07-01)") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = fieldColors, singleLine = true
            )
            OutlinedTextField(
                value = description, onValueChange = { description = it },
                label = { Text("Descrizione") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                shape = RoundedCornerShape(12.dp), colors = fieldColors, maxLines = 6
            )

            // ── Sezione foto ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "📷 Foto annuncio",
                    color = TextPrimary,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    "$totalPhotos / $MAX_PHOTOS",
                    color = if (totalPhotos >= MAX_PHOTOS) Amber400 else TextMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            // Foto esistenti con pulsante ❌
            if (existingPhotos.isNotEmpty()) {
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(existingPhotos) { photo ->
                        Box(modifier = Modifier.size(72.dp)) {
                            AsyncImage(
                                model = photo.url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))
                            )
                            if (photo.isCover) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
                                        .background(DarkBg.copy(alpha = 0.6f))
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Cover", style = MaterialTheme.typography.labelSmall, color = Cyan400)
                                }
                            }
                            // Pulsante ❌ elimina foto
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(3.dp)
                                    .size(20.dp)
                                    .background(Color(0xCC000000), RoundedCornerShape(50))
                                    .clickable {
                                        // Rimozione ottimistica: aggiorna UI immediatamente
                                        existingPhotos = existingPhotos.filter { it.id != photo.id }
                                        viewModel.deletePhoto(photo.id, photo.url)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Close, null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
            // Nuove foto selezionate con pulsante ❌
            if (newPhotoUris.isNotEmpty()) {
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(newPhotoUris) { uri ->
                        Box(modifier = Modifier.size(72.dp)) {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.5.dp, Cyan400, RoundedCornerShape(10.dp))
                            )
                            // Pulsante ❌ rimuove dalla selezione (non ancora caricata)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(3.dp)
                                    .size(20.dp)
                                    .background(Color(0xCC000000), RoundedCornerShape(50))
                                    .clickable { newPhotoUris = newPhotoUris.filter { it != uri } },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
                Text(
                    "${newPhotoUris.size} nuova/e foto da caricare",
                    style = MaterialTheme.typography.labelSmall,
                    color = Cyan400
                )
            }
            OutlinedButton(
                onClick = { photoPicker.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
                enabled = totalPhotos < MAX_PHOTOS,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan400),
                border = androidx.compose.foundation.BorderStroke(1.dp, Cyan400.copy(alpha = if (totalPhotos < MAX_PHOTOS) 0.4f else 0.15f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.AddPhotoAlternate, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (totalPhotos >= MAX_PHOTOS) "Limite $MAX_PHOTOS foto raggiunto" else "Aggiungi foto",
                    style = MaterialTheme.typography.labelMedium
                )
            }


            Button(
                onClick = {
                    val priceVal = price.toDoubleOrNull() ?: listing.priceMonthly
                    viewModel.updateListing(
                        listingId = listing.id,
                        title = title, address = address, city = city, zone = zone,
                        priceMonthly = priceVal, description = description,
                        availableFrom = availableFrom,
                        onSuccess = {
                            if (newPhotoUris.isNotEmpty()) {
                                // Carica le foto: onDismiss è chiamato nel finally di addPhotosToListing
                                viewModel.addPhotosToListing(listing.id, newPhotoUris) { onDismiss() }
                            } else {
                                onDismiss()
                            }
                        }
                    )
                },
                enabled = !isSubmitting && title.isNotBlank() && city.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = DarkBg),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = DarkBg, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Salva modifiche", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
