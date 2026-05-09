package com.renttrack.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renttrack.app.data.model.Inquiry
import com.renttrack.app.data.model.Listing
import com.renttrack.app.ui.theme.*
import com.renttrack.app.viewmodel.ListingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RichiesteScreen(
    viewModel: ListingsViewModel,
    onBack: () -> Unit
) {
    val inquiries    by viewModel.myInquiries.collectAsState()
    val myListings   by viewModel.myListings.collectAsState()
    val isLoading    by viewModel.inquiriesLoading.collectAsState()
    val context      = LocalContext.current

    LaunchedEffect(Unit) { viewModel.loadMyInquiries() }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Richieste ricevute", color = TextPrimary, fontWeight = FontWeight.Bold)
                        if (inquiries.isNotEmpty()) {
                            Text("${inquiries.size} messaggi", color = TextMuted,
                                style = MaterialTheme.typography.labelSmall)
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
                    IconButton(onClick = { viewModel.loadMyInquiries() }) {
                        Icon(Icons.Filled.Refresh, "Aggiorna", tint = TextMuted)
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CircularProgressIndicator(color = Cyan400, strokeWidth = 3.dp)
                        Text("Caricamento richieste…", color = TextMuted,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            inquiries.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(40.dp)
                    ) {
                        Text("📭", fontSize = 64.sp)
                        Text("Nessuna richiesta ancora",
                            color = TextPrimary, fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp)
                        Text("Quando i cercatori ti contatteranno\ntramite i tuoi annunci,\ni messaggi appariranno qui.",
                            color = TextMuted, style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center)
                    }
                }
            }
            else -> {
                // Raggruppa per annuncio
                val byListing = remember(inquiries, myListings) {
                    inquiries.groupBy { it.listingId }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Riepilogo top
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF0D1F38),
                            border = BorderStroke(1.dp, Cyan400.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatBox("📨", "${inquiries.size}", "Totali")
                                VerticalDivider(color = Color(0xFF1E3A5F), modifier = Modifier.height(40.dp))
                                StatBox("🏠", "${byListing.size}", "Annunci con richieste")
                            }
                        }
                    }

                    // Per ogni annuncio, mostra le sue richieste
                    byListing.forEach { (listingId, reqs) ->
                        val listing = myListings.find { it.id == listingId }

                        item(key = "header_$listingId") {
                            // Intestazione annuncio
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Cyan400.copy(alpha = 0.15f)
                                ) {
                                    Icon(Icons.Filled.Apartment, null, tint = Cyan400,
                                        modifier = Modifier.padding(6.dp).size(18.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(listing?.title ?: "Annuncio rimosso",
                                        color = TextPrimary, fontWeight = FontWeight.Bold,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${listing?.city ?: ""} · ${reqs.size} richieste",
                                        color = TextMuted, style = MaterialTheme.typography.labelSmall)
                                }
                                Surface(shape = RoundedCornerShape(8.dp),
                                    color = Cyan400.copy(alpha = 0.15f)) {
                                    Text("${reqs.size}",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        color = Cyan400, fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        items(reqs, key = { "req_${it.id}" }) { inquiry ->
                            InquiryCard(
                                inquiry = inquiry,
                                onCall = {
                                    if (inquiry.seekerPhone.isNotBlank()) {
                                        context.startActivity(
                                            Intent(Intent.ACTION_DIAL,
                                                Uri.parse("tel:${inquiry.seekerPhone}"))
                                        )
                                    }
                                },
                                onEmail = {
                                    if (inquiry.seekerEmail.isNotBlank()) {
                                        context.startActivity(
                                            Intent(Intent.ACTION_SENDTO).apply {
                                                data = Uri.parse("mailto:")
                                                putExtra(Intent.EXTRA_EMAIL, arrayOf(inquiry.seekerEmail))
                                                putExtra(Intent.EXTRA_SUBJECT, "Re: ${listing?.title ?: "Annuncio"}")
                                            }
                                        )
                                    }
                                },
                                onWhatsapp = {
                                    if (inquiry.seekerPhone.isNotBlank()) {
                                        val number = inquiry.seekerPhone
                                            .replace("+", "").replace(" ", "")
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW,
                                                Uri.parse("https://wa.me/$number"))
                                        )
                                    }
                                }
                            )
                        }

                        item(key = "divider_$listingId") {
                            HorizontalDivider(color = Color(0xFF1E3A5F))
                        }
                    }

                    item { Spacer(Modifier.height(60.dp)) }
                }
            }
        }
    }
}

// ─── Card singola richiesta ───────────────────────────────────────────────────
@Composable
private fun InquiryCard(
    inquiry: Inquiry,
    onCall: () -> Unit,
    onEmail: () -> Unit,
    onWhatsapp: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, Color(0xFF1E3A5F)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // Header: nome + data
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    // Avatar
                    Surface(
                        shape = CircleShape,
                        color = Cyan400.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                inquiry.seekerName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                color = Cyan400, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp
                            )
                        }
                    }
                    Column {
                        Text(inquiry.seekerName.ifBlank { "Nome non fornito" },
                            color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text(formatDate(inquiry.createdAt),
                            color = TextMuted, style = MaterialTheme.typography.labelSmall)
                    }
                }
                // Expand toggle
                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        null, tint = TextMuted, modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Messaggio (sempre visibile troncato, espandibile)
            if (inquiry.message.isNotBlank()) {
                Text(
                    inquiry.message,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis
                )
            }

            // Contatti espansi
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(color = Color(0xFF1E3A5F))
                    Text("Contatti", color = TextMuted,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold)
                    if (inquiry.seekerPhone.isNotBlank()) {
                        ContactRow(Icons.Filled.Phone, inquiry.seekerPhone, Cyan400, onCall)
                    }
                    if (inquiry.seekerEmail.isNotBlank()) {
                        ContactRow(Icons.Filled.Email, inquiry.seekerEmail, Green400, onEmail)
                    }
                }
            }

            // Pulsanti azione rapida
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (inquiry.seekerPhone.isNotBlank()) {
                    ActionChip("📞 Chiama", Cyan400) { onCall() }
                    ActionChip("💬 WhatsApp", Color(0xFF25D366)) { onWhatsapp() }
                }
                if (inquiry.seekerEmail.isNotBlank()) {
                    ActionChip("✉️ Email", Green400) { onEmail() }
                }
            }
        }
    }
}

@Composable
private fun ContactRow(icon: ImageVector, text: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Text(text, color = color, style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ActionChip(label: String, color: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = color, style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatBox(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(emoji, fontSize = 22.sp)
        Text(value, color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        Text(label, color = TextMuted, style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center)
    }
}

private fun formatDate(iso: String): String {
    return try {
        val parts = iso.substring(0, 10).split("-")
        "${parts[2]}/${parts[1]}/${parts[0]}"
    } catch (e: Exception) { iso.take(10) }
}
