package com.renttrack.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.renttrack.app.ui.theme.*
import com.renttrack.app.viewmodel.ListingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreaAnnuncioScreen(
    viewModel: ListingsViewModel,
    onBack: () -> Unit
) {
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val toast by viewModel.toast.collectAsState()

    // Campi form
    var title          by remember { mutableStateOf("") }
    var city           by remember { mutableStateOf("") }
    var zone           by remember { mutableStateOf("") }
    var price          by remember { mutableStateOf("") }
    var sqm            by remember { mutableStateOf("") }
    var rooms          by remember { mutableStateOf("") }
    var bathrooms      by remember { mutableStateOf("") }
    var floor          by remember { mutableStateOf("") }
    var furnished      by remember { mutableStateOf(false) }
    var availableFrom  by remember { mutableStateOf("") }
    var description    by remember { mutableStateOf("") }
    var contactType    by remember { mutableStateOf("direct") } // "direct" | "form"
    var contactPhone   by remember { mutableStateOf("") }
    var contactEmail   by remember { mutableStateOf("") }
    var contactWhatsapp by remember { mutableStateOf("") }
    var photoUris      by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris -> photoUris = (photoUris + uris).take(10) }

    toast?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearToast()
            if (msg.contains("✅")) onBack()
        }
    }

    val isValid = title.isNotBlank() && city.isNotBlank() && price.isNotBlank() &&
        (contactType == "form" || contactPhone.isNotBlank() || contactEmail.isNotBlank())

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Cyan400, unfocusedBorderColor = Color(0xFF2D3748),
        focusedLabelColor = Cyan400, unfocusedLabelColor = TextMuted,
        cursorColor = Cyan400, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
        focusedContainerColor = DarkSurfaceVariant.copy(alpha = 0.4f),
        unfocusedContainerColor = DarkSurfaceVariant.copy(alpha = 0.2f)
    )

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = { Text("Pubblica annuncio", color = TextPrimary, fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Foto ─────────────────────────────────────────────────────
            SectionTitle("📸 Foto (max 10)")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Box(
                        modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp))
                            .border(2.dp, Cyan400.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .background(DarkSurface).clickable { photoPicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.AddPhotoAlternate, null, tint = Cyan400, modifier = Modifier.size(28.dp))
                            Text("Aggiungi", color = Cyan400, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                items(photoUris) { uri ->
                    Box(modifier = Modifier.size(100.dp)) {
                        AsyncImage(model = uri, contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)))
                        IconButton(
                            onClick = { photoUris = photoUris - uri },
                            modifier = Modifier.align(Alignment.TopEnd).size(28.dp)
                        ) {
                            Surface(shape = CircleShape, color = DarkBg.copy(alpha = 0.8f)) {
                                Icon(Icons.Filled.Close, null, tint = Red400, modifier = Modifier.padding(4.dp).size(16.dp))
                            }
                        }
                    }
                }
            }
            Text("${photoUris.size}/10 · La prima foto sarà la copertina",
                color = TextMuted, style = MaterialTheme.typography.labelSmall)

            // ── Informazioni base ─────────────────────────────────────────
            SectionTitle("📋 Informazioni")
            OutlinedTextField(title, { title = it }, label = { Text("Titolo annuncio *") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = RoundedCornerShape(12.dp), colors = fieldColors,
                placeholder = { Text("es. Bilocale luminoso in centro", color = TextMuted) })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(city, { city = it }, label = { Text("Città *") },
                    modifier = Modifier.weight(1f), singleLine = true,
                    shape = RoundedCornerShape(12.dp), colors = fieldColors)
                OutlinedTextField(zone, { zone = it }, label = { Text("Zona / Quartiere") },
                    modifier = Modifier.weight(1f), singleLine = true,
                    shape = RoundedCornerShape(12.dp), colors = fieldColors)
            }
            OutlinedTextField(price, { price = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Canone mensile (€) *") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = RoundedCornerShape(12.dp), colors = fieldColors,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = { Text("€", color = TextMuted, modifier = Modifier.padding(start = 12.dp)) })

            // ── Dettagli immobile ─────────────────────────────────────────
            SectionTitle("🏠 Dettagli immobile")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(rooms, { rooms = it.filter { c -> c.isDigit() } }, label = { Text("Stanze") },
                    modifier = Modifier.weight(1f), singleLine = true,
                    shape = RoundedCornerShape(12.dp), colors = fieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(bathrooms, { bathrooms = it.filter { c -> c.isDigit() } }, label = { Text("Bagni") },
                    modifier = Modifier.weight(1f), singleLine = true,
                    shape = RoundedCornerShape(12.dp), colors = fieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(sqm, { sqm = it.filter { c -> c.isDigit() } }, label = { Text("m²") },
                    modifier = Modifier.weight(1f), singleLine = true,
                    shape = RoundedCornerShape(12.dp), colors = fieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(floor, { floor = it }, label = { Text("Piano") },
                    modifier = Modifier.weight(1f), singleLine = true,
                    shape = RoundedCornerShape(12.dp), colors = fieldColors)
                OutlinedTextField(availableFrom, { availableFrom = it }, label = { Text("Disponibile da") },
                    modifier = Modifier.weight(1.5f), singleLine = true,
                    shape = RoundedCornerShape(12.dp), colors = fieldColors,
                    placeholder = { Text("es. Luglio 2026", color = TextMuted) })
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Arredato", color = TextPrimary)
                Switch(checked = furnished, onCheckedChange = { furnished = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = DarkBg, checkedTrackColor = Cyan400))
            }
            OutlinedTextField(description, { description = it }, label = { Text("Descrizione") },
                modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 6,
                shape = RoundedCornerShape(12.dp), colors = fieldColors)

            // ── Modalità contatto ─────────────────────────────────────────
            SectionTitle("📞 Modalità di contatto")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = contactType == "direct",
                    onClick = { contactType = "direct" },
                    label = { Text("Contatto diretto") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = contactType == "form",
                    onClick = { contactType = "form" },
                    label = { Text("Ricevi richieste") },
                    modifier = Modifier.weight(1f)
                )
            }
            if (contactType == "direct") {
                OutlinedTextField(contactPhone, { contactPhone = it }, label = { Text("Telefono") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(12.dp), colors = fieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = { Icon(Icons.Filled.Phone, null, tint = TextMuted, modifier = Modifier.size(18.dp)) })
                OutlinedTextField(contactEmail, { contactEmail = it }, label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(12.dp), colors = fieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    leadingIcon = { Icon(Icons.Filled.Email, null, tint = TextMuted, modifier = Modifier.size(18.dp)) })
                OutlinedTextField(contactWhatsapp, { contactWhatsapp = it }, label = { Text("WhatsApp (numero)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(12.dp), colors = fieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = { Icon(Icons.Filled.Message, null, tint = TextMuted, modifier = Modifier.size(18.dp)) })
            } else {
                Surface(shape = RoundedCornerShape(12.dp), color = Cyan400.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "ℹ️ I cercatori potranno lasciarti nome, telefono, email e un messaggio. Tu li riceverai nella sezione \"Richieste\" dei tuoi annunci.",
                        modifier = Modifier.padding(14.dp),
                        color = TextSecondary, style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // ── Pulsante pubblica ─────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.createListing(
                        title = title.trim(), city = city.trim(), zone = zone.trim(),
                        priceMonthly = price.toDoubleOrNull() ?: 0.0,
                        sqm = sqm.toIntOrNull(), rooms = rooms.toIntOrNull(), bathrooms = bathrooms.toIntOrNull(),
                        floor = floor.trim(), furnished = furnished, availableFrom = availableFrom.trim(),
                        description = description.trim(), contactType = contactType,
                        contactPhone = contactPhone.trim(), contactEmail = contactEmail.trim(),
                        contactWhatsapp = contactWhatsapp.trim(), photoUris = photoUris,
                        onSuccess = {}
                    )
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                enabled = isValid && !isSubmitting,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = DarkBg,
                    disabledContainerColor = Cyan400.copy(alpha = 0.3f))
            ) {
                if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(22.dp), color = DarkBg, strokeWidth = 2.dp)
                else {
                    Icon(Icons.Filled.Publish, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Pubblica annuncio", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
            }

            toast?.let {
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    color = if (it.contains("✅")) Green400.copy(alpha = 0.15f) else Red400.copy(alpha = 0.15f)) {
                    Text(it, modifier = Modifier.padding(12.dp),
                        color = if (it.contains("✅")) Green400 else Red400, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp))
}
