package com.renttrack.app.ui.screens

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
    val state by viewModel.publicState.collectAsState()
    var searchCity by remember { mutableStateOf("") }
    var searchZone by remember { mutableStateOf("") }
    var maxPrice   by remember { mutableStateOf("") }

    val listings = when (val s = state) {
        is ListingsUiState.Success -> s.listings.filter { l ->
            (searchCity.isBlank() || l.city.contains(searchCity, ignoreCase = true)) &&
            (searchZone.isBlank() || l.zone.contains(searchZone, ignoreCase = true)) &&
            (maxPrice.isBlank()   || l.priceMonthly <= (maxPrice.toDoubleOrNull() ?: Double.MAX_VALUE))
        }
        else -> emptyList()
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🏠 Annunci", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = TextPrimary)
                        Text("Trova il tuo prossimo affitto", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg),
                actions = {
                    if (isLoggedIn) {
                        TextButton(onClick = onCreaAnnuncio) {
                            Icon(Icons.Filled.Add, null, tint = Cyan400, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Pubblica", color = Cyan400, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        TextButton(onClick = onLoginClick) {
                            Icon(Icons.Filled.Lock, null, tint = Cyan400, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Proprietario", color = Cyan400, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Filtri ────────────────────────────────────────────────────
            Surface(color = DarkSurface, tonalElevation = 0.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchCity, onValueChange = { searchCity = it },
                        label = { Text("Città", style = MaterialTheme.typography.labelSmall) },
                        singleLine = true, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = filterFieldColors(),
                        leadingIcon = { Icon(Icons.Filled.LocationCity, null, tint = TextMuted, modifier = Modifier.size(16.dp)) }
                    )
                    OutlinedTextField(
                        value = searchZone, onValueChange = { searchZone = it },
                        label = { Text("Zona", style = MaterialTheme.typography.labelSmall) },
                        singleLine = true, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = filterFieldColors()
                    )
                    OutlinedTextField(
                        value = maxPrice, onValueChange = { maxPrice = it.filter { c -> c.isDigit() } },
                        label = { Text("Max €", style = MaterialTheme.typography.labelSmall) },
                        singleLine = true, modifier = Modifier.weight(0.8f),
                        shape = RoundedCornerShape(10.dp),
                        colors = filterFieldColors()
                    )
                }
            }

            // ── Lista annunci ─────────────────────────────────────────────
            when (val s = state) {
                is ListingsUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Cyan400)
                    }
                }
                is ListingsUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("😕", fontSize = 40.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(s.message, color = TextMuted)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadPublicListings() },
                                colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = DarkBg)) {
                                Text("Riprova")
                            }
                        }
                    }
                }
                is ListingsUiState.Success -> {
                    if (listings.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🏗️", fontSize = 48.sp)
                                Text("Nessun annuncio trovato", color = TextMuted)
                                if (searchCity.isNotBlank() || searchZone.isNotBlank() || maxPrice.isNotBlank()) {
                                    TextButton(onClick = { searchCity = ""; searchZone = ""; maxPrice = "" }) {
                                        Text("Rimuovi filtri", color = Cyan400)
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(listings) { listing ->
                                ListingCard(listing = listing, onClick = { onListingClick(listing) })
                            }
                            item { Spacer(Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
    }
}

// ─── Card annuncio ────────────────────────────────────────────────────────────
@Composable
fun ListingCard(listing: Listing, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, Cyan400.copy(alpha = 0.15f))
    ) {
        Column {
            // Foto di copertina
            Box(
                modifier = Modifier.fillMaxWidth().height(180.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            ) {
                if (listing.coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = listing.coverUrl,
                        contentDescription = listing.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(Brush.linearGradient(listOf(Color(0xFF1A2744), Color(0xFF0D1B2A)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🏠", fontSize = 48.sp)
                    }
                }
                // Badge prezzo
                Surface(
                    modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = DarkBg.copy(alpha = 0.85f)
                ) {
                    Text(
                        "€${listing.priceMonthly.toInt()}/mese",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = Cyan400, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp
                    )
                }
                // Badge arredato
                if (listing.furnished) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = Green400.copy(alpha = 0.85f)
                    ) {
                        Text("Arredato", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = DarkBg, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Info
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(listing.title, fontWeight = FontWeight.Bold, color = TextPrimary,
                    fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    Text(
                        buildString {
                            append(listing.city)
                            if (listing.zone.isNotBlank()) append(" · ${listing.zone}")
                        },
                        color = TextMuted, style = MaterialTheme.typography.bodySmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                // Dettagli rapidi
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listing.rooms?.let { DetailChip("🛏 $it") }
                    listing.bathrooms?.let { DetailChip("🚿 $it") }
                    listing.sqm?.let { DetailChip("📐 $it m²") }
                    if (listing.floor.isNotBlank()) DetailChip("🏢 ${listing.floor}")
                }
            }
        }
    }
}

@Composable
private fun DetailChip(text: String) {
    Surface(shape = RoundedCornerShape(6.dp), color = DarkSurfaceVariant) {
        Text(text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
private fun filterFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Cyan400, unfocusedBorderColor = Color(0xFF2D3748),
    focusedLabelColor = Cyan400, unfocusedLabelColor = TextMuted,
    cursorColor = Cyan400, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
    focusedContainerColor = DarkSurfaceVariant.copy(alpha = 0.4f),
    unfocusedContainerColor = DarkSurfaceVariant.copy(alpha = 0.2f)
)
