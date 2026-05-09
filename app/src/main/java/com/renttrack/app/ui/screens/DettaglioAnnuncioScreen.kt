package com.renttrack.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.renttrack.app.data.model.Listing
import com.renttrack.app.ui.theme.*
import com.renttrack.app.viewmodel.ListingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DettaglioAnnuncioScreen(
    listing: Listing,
    viewModel: ListingsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val toast by viewModel.toast.collectAsState()

    var showInquiryForm by remember { mutableStateOf(false) }

    // Mostra toast
    toast?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearToast()
        }
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = { Text(listing.title, color = TextPrimary, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Indietro", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState())
        ) {

            // ── Galleria foto ─────────────────────────────────────────────
            if (listing.photos.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(listing.photos.sortedBy { it.displayOrder }) { photo ->
                        AsyncImage(
                            model = photo.url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.width(300.dp).fillMaxHeight()
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${listing.photos.size} foto",
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = TextMuted, style = MaterialTheme.typography.labelSmall
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                        .background(DarkSurface),
                    contentAlignment = Alignment.Center
                ) { Text("🏠", fontSize = 64.sp) }
            }

            Spacer(Modifier.height(16.dp))

            // ── Prezzo + titolo ───────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    "€${listing.priceMonthly.toInt()} / mese",
                    fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Cyan400
                )
                Spacer(Modifier.height(4.dp))
                Text(listing.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        buildString {
                            append(listing.city)
                            if (listing.zone.isNotBlank()) append(" · ${listing.zone}")
                        },
                        color = TextMuted
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Dettagli ──────────────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp), color = DarkSurface
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Dettagli", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                    HorizontalDivider(color = DarkSurfaceVariant)
                    DetailRow("🛏 Stanze",    listing.rooms?.toString() ?: "—")
                    DetailRow("🚿 Bagni",     listing.bathrooms?.toString() ?: "—")
                    DetailRow("📐 Superficie", listing.sqm?.let { "$it m²" } ?: "—")
                    DetailRow("🏢 Piano",     listing.floor.ifBlank { "—" })
                    DetailRow("🛋 Arredato",  if (listing.furnished) "Sì" else "No")
                    DetailRow("📅 Disponibile da", listing.availableFrom.ifBlank { "Subito" })
                }
            }

            // ── Descrizione ───────────────────────────────────────────────
            if (listing.description.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp), color = DarkSurface
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Descrizione", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                        HorizontalDivider(color = DarkSurfaceVariant)
                        Text(listing.description, color = TextSecondary, lineHeight = 22.sp)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Contatti ──────────────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp), color = DarkSurface
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Contatta il proprietario", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                    HorizontalDivider(color = DarkSurfaceVariant)

                    if (listing.contactType == "direct") {
                        // Contatti diretti
                        if (listing.contactPhone.isNotBlank()) {
                            ContactButton(
                                icon = Icons.Filled.Phone, label = "Chiama",
                                value = listing.contactPhone, color = Green400
                            ) {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${listing.contactPhone}")))
                            }
                        }
                        if (listing.contactEmail.isNotBlank()) {
                            ContactButton(
                                icon = Icons.Filled.Email, label = "Email",
                                value = listing.contactEmail, color = Cyan400
                            ) {
                                context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${listing.contactEmail}")))
                            }
                        }
                        if (listing.contactWhatsapp.isNotBlank()) {
                            ContactButton(
                                icon = Icons.Filled.Message, label = "WhatsApp",
                                value = listing.contactWhatsapp, color = Color(0xFF25D366)
                            ) {
                                val num = listing.contactWhatsapp.filter { it.isDigit() }
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$num")))
                            }
                        }
                    } else {
                        // Modulo di contatto
                        Text(
                            "Il proprietario preferisce essere contattato tramite richiesta.\nLascia i tuoi riferimenti e ti risponderà.",
                            color = TextMuted, style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = { showInquiryForm = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = DarkBg)
                        ) {
                            Icon(Icons.Filled.Send, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Invia richiesta", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Toast
            toast?.let {
                Spacer(Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Green400.copy(alpha = 0.15f)
                ) {
                    Text(it, modifier = Modifier.padding(12.dp), color = Green400, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // ── Modulo richiesta ──────────────────────────────────────────────────────
    if (showInquiryForm) {
        InquiryFormSheet(
            isSubmitting = isSubmitting,
            onDismiss = { showInquiryForm = false },
            onSubmit = { name, phone, email, message ->
                viewModel.submitInquiry(listing.id, name, phone, email, message) {
                    showInquiryForm = false
                }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextMuted, style = MaterialTheme.typography.bodySmall)
        Text(value, color = TextPrimary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ContactButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, color: Color, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.Start) {
            Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(value, color = TextMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

// ─── Modulo richiesta contatto ────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InquiryFormSheet(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (name: String, phone: String, email: String, message: String) -> Unit
) {
    var name    by remember { mutableStateOf("") }
    var phone   by remember { mutableStateOf("") }
    var email   by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = DarkSurface) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("📬 Lascia i tuoi riferimenti",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
            Text("Il proprietario ti contatterà al più presto.",
                style = MaterialTheme.typography.bodySmall, color = TextMuted)

            val colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Cyan400, unfocusedBorderColor = Color(0xFF2D3748),
                focusedLabelColor = Cyan400, unfocusedLabelColor = TextMuted,
                cursorColor = Cyan400, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                focusedContainerColor = DarkSurfaceVariant.copy(alpha = 0.4f),
                unfocusedContainerColor = DarkSurfaceVariant.copy(alpha = 0.2f)
            )
            OutlinedTextField(name, { name = it }, label = { Text("Nome *") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = RoundedCornerShape(12.dp), colors = colors)
            OutlinedTextField(phone, { phone = it }, label = { Text("Telefono") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = RoundedCornerShape(12.dp), colors = colors,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone))
            OutlinedTextField(email, { email = it }, label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = RoundedCornerShape(12.dp), colors = colors,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email))
            OutlinedTextField(message, { message = it }, label = { Text("Messaggio (opzionale)") },
                modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4,
                shape = RoundedCornerShape(12.dp), colors = colors)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, TextMuted.copy(alpha = 0.4f))) {
                    Text("Annulla", color = TextSecondary)
                }
                Button(
                    onClick = { onSubmit(name.trim(), phone.trim(), email.trim(), message.trim()) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    enabled = name.isNotBlank() && !isSubmitting,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = DarkBg)
                ) {
                    if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = DarkBg, strokeWidth = 2.dp)
                    else Text("Invia", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
