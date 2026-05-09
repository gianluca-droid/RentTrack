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
    val myListings by viewModel.myListings.collectAsState()
    val isLoading  by viewModel.myListingsLoading.collectAsState()
    val toast      by viewModel.toast.collectAsState()
    var toDelete by remember { mutableStateOf<Listing?>(null) }

    LaunchedEffect(Unit) { viewModel.loadMyListings() }

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
                title = { Text("I miei annunci", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Indietro", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg),
                actions = {
                    // Pulsante richieste ricevute
                    TextButton(onClick = onRichieste) {
                        Icon(Icons.Filled.Inbox, null, tint = Cyan400, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Richieste", color = Cyan400, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onCreaAnnuncio) {
                        Icon(Icons.Filled.Add, null, tint = Cyan400, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Nuovo", color = Cyan400, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Cyan400)
                    }
                }
                myListings.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("📭", fontSize = 48.sp)
                        Text("Nessun annuncio ancora", color = TextMuted, fontSize = 16.sp)
                        Text("Pubblica il tuo primo annuncio\nper trovare inquilini",
                            color = TextMuted.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = onCreaAnnuncio,
                            colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = DarkBg),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Pubblica annuncio", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(myListings) { listing ->
                            MyListingCard(
                                listing = listing,
                                onToggleActive = { viewModel.toggleActive(listing.id, listing.isActive) },
                                onToggleAvailable = { viewModel.toggleAvailable(listing.id, listing.isAvailable) },
                                onDelete = { toDelete = listing }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }

            // Toast
            toast?.let { msg ->
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = if (msg.contains("eliminato")) Red400.copy(alpha = 0.15f)
                            else Green400.copy(alpha = 0.15f)
                ) {
                    Text(msg, modifier = Modifier.padding(12.dp),
                        color = if (msg.contains("eliminato")) Red400 else Green400,
                        fontWeight = FontWeight.Medium)
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
            title = { Text("Elimina annuncio", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Eliminare \"${listing.title}\"?\nL'annuncio non sarà più visibile.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteListing(listing.id)
                    toDelete = null
                }) { Text("Elimina", color = Red400, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text("Annulla", color = TextSecondary) }
            }
        )
    }
}

@Composable
private fun MyListingCard(
    listing: Listing,
    onToggleActive: () -> Unit,
    onToggleAvailable: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp,
            if (listing.isActive) Cyan400.copy(alpha = 0.2f) else Color(0xFF2D3748))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Foto copertina
            Box(modifier = Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)).background(DarkSurfaceVariant)) {
                if (listing.coverUrl.isNotBlank()) {
                    AsyncImage(model = listing.coverUrl, contentDescription = null,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("🏠", fontSize = 28.sp)
                    }
                }
                if (!listing.isActive) {
                    Box(Modifier.fillMaxSize().background(DarkBg.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center) {
                        Text("OFF", color = TextMuted, fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Info
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(listing.title, color = TextPrimary, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${listing.city}${if (listing.zone.isNotBlank()) " · ${listing.zone}" else ""}",
                    color = TextMuted, style = MaterialTheme.typography.bodySmall)
                Text("€${listing.priceMonthly.toInt()}/mese",
                    color = Cyan400, fontWeight = FontWeight.Bold)
                // Badge stato visibilità
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(shape = RoundedCornerShape(6.dp),
                        color = if (listing.isActive) Green400.copy(alpha = 0.15f) else TextMuted.copy(alpha = 0.1f)) {
                        Text(
                            if (listing.isActive) "● Attivo" else "● Inattivo",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            color = if (listing.isActive) Green400 else TextMuted,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    // Badge disponibilità
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (listing.isAvailable) Cyan400.copy(alpha = 0.15f)
                                else Amber400.copy(alpha = 0.15f)
                    ) {
                        Text(
                            if (listing.isAvailable) "🔓 Libero" else "🔒 Occupato",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            color = if (listing.isAvailable) Cyan400 else Amber400,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Azioni
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                // Toggle libero/occupato
                IconButton(onClick = onToggleAvailable, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (listing.isAvailable) Icons.Filled.Lock else Icons.Filled.LockOpen,
                        contentDescription = if (listing.isAvailable) "Segna occupato" else "Segna libero",
                        tint = if (listing.isAvailable) Cyan400 else Amber400,
                        modifier = Modifier.size(20.dp)
                    )
                }
                // Toggle attivo/inattivo
                IconButton(onClick = onToggleActive, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (listing.isActive) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (listing.isActive) "Disattiva" else "Attiva",
                        tint = if (listing.isActive) TextMuted else Green400,
                        modifier = Modifier.size(20.dp)
                    )
                }
                // Elimina
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Delete, "Elimina", tint = Red400.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
