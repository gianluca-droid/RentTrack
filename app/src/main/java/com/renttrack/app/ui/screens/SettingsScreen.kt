package com.renttrack.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.renttrack.app.notifications.RentCheckWorker
import com.renttrack.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs   = remember { context.getSharedPreferences("renttrack_prefs", android.content.Context.MODE_PRIVATE) }

    // ── Stati configurazione ──────────────────────────────────────────────────
    var reminderDays    by remember { mutableIntStateOf(prefs.getInt(RentCheckWorker.PREF_REMINDER_DAYS, 3)) }
    var reminderSameDay by remember { mutableStateOf(prefs.getBoolean(RentCheckWorker.PREF_REMINDER_SAMEDAY, true)) }
    var showDaysDialog  by remember { mutableStateOf(false) }

    // Salva automaticamente ogni cambio
    fun save() {
        prefs.edit()
            .putInt(RentCheckWorker.PREF_REMINDER_DAYS, reminderDays)
            .putBoolean(RentCheckWorker.PREF_REMINDER_SAMEDAY, reminderSameDay)
            .apply()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Impostazioni", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Indietro", tint = TextMuted)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBg
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // ── Sezione Notifiche ─────────────────────────────────────────
            item { SettingsSectionHeader(Icons.Filled.Notifications, "Notifiche e Reminder") }

            item {
                SettingsCard {
                    // Toggle: notifica giorno stesso
                    SettingsToggleRow(
                        icon    = Icons.Filled.Today,
                        label   = "Avviso giorno scadenza",
                        desc    = "Ricevi una notifica il giorno in cui scade il cedolino",
                        checked = reminderSameDay,
                        onToggle = { reminderSameDay = it; save() }
                    )

                    HorizontalDivider(color = Color(0xFF2D3748))

                    // Giorni prima
                    SettingsActionRow(
                        icon     = Icons.Filled.Schedule,
                        label    = "Giorni di preavviso",
                        desc     = "Ricevi un reminder $reminderDays giorni prima della scadenza",
                        trailing = {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Cyan400.copy(alpha = 0.15f),
                                onClick = { showDaysDialog = true }
                            ) {
                                Text(
                                    "$reminderDays gg",
                                    color = Cyan400,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    )
                }
            }

            // ── Sezione Informazioni App ──────────────────────────────────
            item { SettingsSectionHeader(Icons.Filled.Info, "Informazioni") }

            item {
                SettingsCard {
                    SettingsInfoRow(Icons.Filled.PhoneAndroid, "Versione app", "1.0.0 (build 1)")
                    HorizontalDivider(color = Color(0xFF2D3748))
                    SettingsInfoRow(Icons.Filled.Cloud, "Backend", "Supabase Cloud")
                    HorizontalDivider(color = Color(0xFF2D3748))
                    SettingsInfoRow(Icons.Filled.Android, "Piattaforma", "Android 8.0+")
                    HorizontalDivider(color = Color(0xFF2D3748))
                    SettingsInfoRow(Icons.Filled.Code, "Sviluppato da", "Gianluca D.")
                }
            }

            // ── Sezione Privacy ───────────────────────────────────────────
            item { SettingsSectionHeader(Icons.Filled.Lock, "Privacy & Sicurezza") }

            item {
                SettingsCard {
                    SettingsInfoRow(
                        Icons.Filled.Storage,
                        "Archiviazione dati",
                        "I tuoi dati sono salvati in modo sicuro su Supabase (EU)"
                    )
                    HorizontalDivider(color = Color(0xFF2D3748))
                    SettingsInfoRow(
                        Icons.Filled.VpnLock,
                        "Autenticazione",
                        "JWT + refresh token (Supabase Auth)"
                    )
                    HorizontalDivider(color = Color(0xFF2D3748))
                    SettingsInfoRow(
                        Icons.Filled.Email,
                        "Password dimenticata",
                        "Usa il link nella schermata di accesso"
                    )
                }
            }

            // ── Sezione Supporto ──────────────────────────────────────────
            item { SettingsSectionHeader(Icons.Filled.HelpOutline, "Supporto") }

            item {
                SettingsCard {
                    SettingsInfoRow(
                        Icons.Filled.BugReport,
                        "Segnala un problema",
                        "Contatta: support@renttrack.it"
                    )
                    HorizontalDivider(color = Color(0xFF2D3748))
                    SettingsInfoRow(
                        Icons.Filled.PrivacyTip,
                        "Privacy Policy",
                        "renttrack.it/privacy"
                    )
                }
            }
        }
    }

    // ── Dialog selezione giorni reminder ─────────────────────────────────────
    if (showDaysDialog) {
        AlertDialog(
            onDismissRequest = { showDaysDialog = false },
            containerColor   = DarkSurface,
            shape            = RoundedCornerShape(20.dp),
            icon             = { Icon(Icons.Filled.Schedule, null, tint = Cyan400) },
            title            = { Text("Giorni di preavviso", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text             = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Ricevi un reminder N giorni prima della scadenza di ogni cedolino:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(Modifier.height(8.dp))
                    listOf(1, 2, 3, 5, 7, 14).forEach { days ->
                        Surface(
                            shape   = RoundedCornerShape(10.dp),
                            color   = if (reminderDays == days) Cyan400.copy(alpha = 0.15f) else Color.Transparent,
                            onClick = { reminderDays = days; save(); showDaysDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = reminderDays == days,
                                    onClick  = { reminderDays = days; save(); showDaysDialog = false },
                                    colors   = RadioButtonDefaults.colors(selectedColor = Cyan400)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "$days ${if (days == 1) "giorno" else "giorni"} prima",
                                    color = if (reminderDays == days) Cyan400 else TextPrimary,
                                    fontWeight = if (reminderDays == days) FontWeight.Bold else FontWeight.Normal,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDaysDialog = false }) {
                    Text("Chiudi", color = TextMuted)
                }
            }
        )
    }
}

// ─── Componenti interni ───────────────────────────────────────────────────────

@Composable
private fun SettingsSectionHeader(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = Cyan400, modifier = Modifier.size(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = Cyan400
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkCard,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    label: String,
    desc: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor       = Color.Black,
                checkedTrackColor       = Cyan400,
                uncheckedThumbColor     = TextMuted,
                uncheckedTrackColor     = DarkSurface
            )
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    label: String,
    desc: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        trailing()
    }
}

@Composable
private fun SettingsInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
            Text(value, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
    }
}
