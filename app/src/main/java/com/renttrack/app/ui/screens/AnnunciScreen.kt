package com.renttrack.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.renttrack.app.data.model.Listing
import com.renttrack.app.ui.theme.*
import com.renttrack.app.viewmodel.ListingsUiState
import com.renttrack.app.viewmodel.ListingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnunciScreen(
    viewModel: ListingsViewModel,
    isLoggedIn: Boolean,
    onListingClick: (Listing) -> Unit,
    onLoginClick: () -> Unit,
    onCreaAnnuncio: () -> Unit
) {
    val state        by viewModel.publicState.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val hasMore      by viewModel.hasMore.collectAsState()
    val listState    = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    var query         by remember { mutableStateOf("") }
    var maxPrice      by remember { mutableStateOf("") }
    var onlyFurnished by remember { mutableStateOf(false) }
    var showFilters   by remember { mutableStateOf(false) }

    val allListings = (state as? ListingsUiState.Success)?.listings ?: emptyList()
    val listings = allListings.filter { l ->
        (query.isBlank() ||
            l.city.contains(query, ignoreCase = true) ||
            l.zone.contains(query, ignoreCase = true) ||
            l.title.contains(query, ignoreCase = true)) &&
        (maxPrice.isBlank() || l.priceMonthly <= (maxPrice.toDoubleOrNull() ?: Double.MAX_VALUE)) &&
        (!onlyFurnished || l.furnished)
    }
    val activeFilters = (if (maxPrice.isNotBlank()) 1 else 0) + (if (onlyFurnished) 1 else 0)

    // Carica annunci al primo avvio della schermata
    LaunchedEffect(Unit) { viewModel.loadPublicListings() }

    // Infinite scroll: carica più annunci quando mancano 3 item alla fine
    // (solo quando non ci sono filtri attivi — i filtri lavorano sui dati già caricati)
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            lastVisible >= total - 3 && total > 0
        }
    }
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && query.isBlank() && activeFilters == 0) {
            viewModel.loadMorePublicListings()
        }
    }

    Scaffold(containerColor = DarkBg) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {

            // ── Hero ──────────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF0A1628), Color(0xFF112240), DarkBg)
                            )
                        )
                        .padding(horizontal = 20.dp)
                        .padding(top = 24.dp, bottom = 20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Header row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "🏠 RentTrack",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold, color = Cyan400
                                    )
                                )
                                Text(
                                    "Trova il tuo prossimo affitto",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                            if (isLoggedIn) {
                                FilledTonalButton(
                                    onClick = onCreaAnnuncio,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = Cyan400.copy(alpha = 0.15f),
                                        contentColor = Cyan400
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Pubblica", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = onLoginClick,
                                    border = BorderStroke(1.dp, Cyan400.copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Filled.Lock, null, tint = Cyan400, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Sei un proprietario?", color = Cyan400, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        // Barra di ricerca
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Cerca per città, zona, titolo…", color = TextMuted) },
                            leadingIcon = { Icon(Icons.Filled.Search, null, tint = Cyan400) },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (query.isNotBlank()) {
                                        IconButton(onClick = { query = "" }) {
                                            Icon(Icons.Filled.Close, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    // Badge filtri attivi
                                    BadgedBox(
                                        badge = {
                                            if (activeFilters > 0)
                                                Badge(containerColor = Cyan400, contentColor = DarkBg) {
                                                    Text("$activeFilters")
                                                }
                                        }
                                    ) {
                                        IconButton(onClick = { showFilters = !showFilters }) {
                                            Icon(Icons.Filled.Tune, null,
                                                tint = if (activeFilters > 0) Cyan400 else TextMuted)
                                        }
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Cyan400,
                                unfocusedBorderColor = Color(0xFF1E3A5F),
                                cursorColor = Cyan400,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = Color(0xFF0D1F38),
                                unfocusedContainerColor = Color(0xFF0D1F38)
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                        )

                        // Filtri espandibili
                        AnimatedVisibility(visible = showFilters) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = maxPrice,
                                    onValueChange = { maxPrice = it.filter { c -> c.isDigit() } },
                                    label = { Text("Prezzo massimo (€/mese)", color = TextMuted) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Cyan400, unfocusedBorderColor = Color(0xFF1E3A5F),
                                        cursorColor = Cyan400, focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedContainerColor = Color(0xFF0D1F38),
                                        unfocusedContainerColor = Color(0xFF0D1F38),
                                        focusedLabelColor = Cyan400
                                    ),
                                    leadingIcon = { Text("€", color = TextMuted, modifier = Modifier.padding(start = 12.dp)) }
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilterChip(
                                        selected = onlyFurnished,
                                        onClick = { onlyFurnished = !onlyFurnished },
                                        label = { Text("🛋 Solo arredati") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Cyan400.copy(alpha = 0.2f),
                                            selectedLabelColor = Cyan400
                                        )
                                    )
                                    if (activeFilters > 0) {
                                        TextButton(onClick = { maxPrice = ""; onlyFurnished = false }) {
                                            Text("Azzera", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Risultati header ──────────────────────────────────────────
            if (state is ListingsUiState.Success) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (listings.isEmpty()) "Nessun annuncio trovato"
                            else "${listings.size} annunci trovati",
                            color = TextMuted, style = MaterialTheme.typography.bodySmall
                        )
                        if (query.isNotBlank() || activeFilters > 0) {
                            TextButton(
                                onClick = { query = ""; maxPrice = ""; onlyFurnished = false },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Rimuovi filtri", color = Cyan400, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // ── Contenuto ─────────────────────────────────────────────────
            when (val s = state) {
                is ListingsUiState.Loading -> {
                    item {
                        Box(
                            Modifier.fillMaxWidth().height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                CircularProgressIndicator(color = Cyan400, strokeWidth = 3.dp)
                                Text("Caricamento annunci…", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                is ListingsUiState.Error -> {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("😕", fontSize = 56.sp)
                            Text("Impossibile caricare gli annunci",
                                color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text(s.message, color = TextMuted,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Button(
                                onClick = { viewModel.loadPublicListings() },
                                colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = DarkBg),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Riprova", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                is ListingsUiState.Success -> {
                    if (listings.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("🏗️", fontSize = 56.sp)
                                Text("Nessun annuncio trovato",
                                    color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text("Prova a modificare i filtri di ricerca",
                                    color = TextMuted, style = MaterialTheme.typography.bodySmall,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    } else {
                        // Prima card in grande (featured)
                        item {
                            FeaturedListingCard(
                                listing = listings.first(),
                                onClick = { onListingClick(listings.first()) }
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                        // Resto delle card
                        if (listings.size > 1) {
                            items(listings.drop(1)) { listing ->
                                ListingCard(
                                    listing = listing,
                                    onClick = { onListingClick(listing) }
                                )
                            }
                        }
                        // ── Footer paginazione ─────────────────────────────
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    isLoadingMore -> {
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(color = Cyan400,
                                                strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                            Text("Caricamento…", color = TextMuted,
                                                style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    !hasMore && listings.size > 1 -> {
                                        Text("✓ Hai visto tutti gli annunci",
                                            color = TextMuted,
                                            style = MaterialTheme.typography.labelSmall,
                                            textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Card grande (primo risultato) ────────────────────────────────────────────
@Composable
fun FeaturedListingCard(listing: Listing, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        // Immagine di sfondo
        Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
            if (listing.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = listing.coverUrl, contentDescription = listing.title,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(Brush.linearGradient(listOf(Color(0xFF1A2744), Color(0xFF0D1B2A)))),
                    contentAlignment = Alignment.Center
                ) { Text("🏠", fontSize = 64.sp) }
            }
            // Gradiente overlay basso
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, DarkBg.copy(alpha = 0.95f)),
                            startY = 80f
                        )
                    )
            )
            // Badge in alto
            Row(
                modifier = Modifier.align(Alignment.TopStart).padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(shape = RoundedCornerShape(8.dp), color = Cyan400) {
                    Text("✨ In evidenza",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = DarkBg, fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.labelSmall)
                }
                if (!listing.isAvailable) {
                    Surface(shape = RoundedCornerShape(8.dp), color = Amber400.copy(alpha = 0.9f)) {
                        Text("🔒 Occupato",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            color = DarkBg, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (listing.furnished) {
                    Surface(shape = RoundedCornerShape(8.dp), color = Green400.copy(alpha = 0.9f)) {
                        Text("Arredato",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            color = DarkBg, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            // Info in basso
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "€${listing.priceMonthly.toInt()}/mese",
                    fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Cyan400
                )
                Text(listing.title, fontWeight = FontWeight.Bold, color = TextPrimary,
                    fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(
                        buildString {
                            append(listing.city)
                            if (listing.zone.isNotBlank()) append(" · ${listing.zone}")
                        },
                        color = TextMuted, style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listing.rooms?.let { SmallChip("🛏 $it camere") }
                    listing.sqm?.let { SmallChip("📐 $it m²") }
                    listing.bathrooms?.let { SmallChip("🚿 $it bagni") }
                }
            }
        }
    }
}

// ─── Card normale ─────────────────────────────────────────────────────────────
@Composable
fun ListingCard(listing: Listing, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, Color(0xFF1E3A5F))
    ) {
        Row(modifier = Modifier.height(120.dp)) {
            // Foto
            Box(
                modifier = Modifier.width(120.dp).fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            ) {
                if (listing.coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = listing.coverUrl, contentDescription = null,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(Brush.linearGradient(listOf(Color(0xFF1A2744), Color(0xFF0D1B2A)))),
                        contentAlignment = Alignment.Center
                    ) { Text("🏠", fontSize = 32.sp) }
                }
                if (listing.furnished) {
                    Surface(
                        modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = Green400.copy(alpha = 0.85f)
                    ) {
                        Text("Arredato", modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            color = DarkBg, style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
            // Info
            Column(
                modifier = Modifier.weight(1f).padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(listing.title, fontWeight = FontWeight.Bold, color = TextPrimary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Filled.LocationOn, null, tint = TextMuted, modifier = Modifier.size(12.dp))
                        Text(
                            buildString {
                                append(listing.city)
                                if (listing.zone.isNotBlank()) append(" · ${listing.zone}")
                            },
                            color = TextMuted, style = MaterialTheme.typography.labelSmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                // Dettagli rapidi
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listing.rooms?.let { SmallChip("🛏 $it") }
                    listing.sqm?.let { SmallChip("$it m²") }
                }
                Text("€${listing.priceMonthly.toInt()}/mese",
                    fontWeight = FontWeight.ExtraBold, color = Cyan400, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun SmallChip(text: String) {
    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF0D1F38)) {
        Text(text, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}
