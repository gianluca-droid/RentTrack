package com.renttrack.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renttrack.app.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// ─── Formatters ─────────────────────────────────────────────────────
object Formatters {
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.ITALY)
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
    private val shortDateFormat = SimpleDateFormat("dd MMM", Locale.ITALY)

    fun currency(amount: Double): String = currencyFormat.format(amount)
    fun date(timestamp: Long): String = dateFormat.format(Date(timestamp))
    fun shortDate(timestamp: Long): String = shortDateFormat.format(Date(timestamp))
}

// ─── Summary Card ───────────────────────────────────────────────────
@Composable
fun SummaryCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Icona + titolo su una riga compatta
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                Text(title, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
            }
            // Valore grande — ora ha tutta la larghezza della card
            Text(
                value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted, maxLines = 1)
            }
        }
    }
}

// ─── Clickable Summary Card (con freccia) ─────────────────────────────
@Composable
fun ClickableSummaryCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Icona + titolo + freccia su una riga
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                Text(title, style = MaterialTheme.typography.labelSmall, color = TextSecondary,
                    maxLines = 1, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ArrowForward, null, tint = accentColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(12.dp))
            }
            // Valore — larghezza piena
            Text(
                value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted, maxLines = 1)
            }
        }
    }
}


// ─── Section Header ─────────────────────────────────────────────────
@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(action, color = Cyan400, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// ─── Status Badge ───────────────────────────────────────────────────
@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor) = when (status) {
        "Pagato" -> Green500.copy(alpha = 0.15f) to Green400
        "Emesso" -> Cyan500.copy(alpha = 0.15f) to Cyan400
        "Scaduto" -> Red500.copy(alpha = 0.15f) to Red400
        "Parziale" -> Amber500.copy(alpha = 0.15f) to Amber400
        else -> TextMuted.copy(alpha = 0.15f) to TextSecondary
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Text(
            status,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = textColor
        )
    }
}

// ─── Category Chip ──────────────────────────────────────────────────
@Composable
fun CategoryChip(category: String, icon: String) {
    val color = CategoryColors[category] ?: TextSecondary
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.12f)) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 12.sp)
            Spacer(Modifier.width(4.dp))
            Text(category, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ─── Item Card (for lists) ──────────────────────────────────────────
@Composable
fun ItemCard(
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) {
            content()
            if (onEdit != null || onDelete != null) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.06f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (onEdit != null) {
                        TextButton(
                            onClick = onEdit,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.Edit, null, tint = Cyan400, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Modifica", color = Cyan400, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    if (onDelete != null) {
                        TextButton(
                            onClick = onDelete,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.Delete, null, tint = Red400, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Elimina", color = Red400, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

// ─── Empty State ────────────────────────────────────────────────────
@Composable
fun EmptyState(message: String, icon: ImageVector) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = TextMuted, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
    }
}

// ─── Confirm Delete Dialog ──────────────────────────────────────────
@Composable
fun ConfirmDeleteDialog(
    itemName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Conferma eliminazione") },
        text = { Text("Sei sicuro di voler eliminare \"$itemName\"? Questa azione non può essere annullata.") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Elimina", color = Red400) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        },
        containerColor = DarkSurface
    )
}

// ─── Gradient FAB ───────────────────────────────────────────────────
@Composable
fun GradientFab(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        containerColor = Cyan500,
        contentColor = DarkBg
    ) {
        Icon(icon, contentDescription, modifier = Modifier.size(28.dp))
    }
}

// ─── TextField Colors (stile dark) ──────────────────────────────────
@Composable
fun condoTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = Cyan400,
    unfocusedBorderColor = TextMuted.copy(alpha = 0.4f),
    focusedLabelColor = Cyan400,
    unfocusedLabelColor = TextMuted,
    cursorColor = Cyan400,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent
)

// ─── Category Chip per filtri documenti ─────────────────────────────
@Composable
fun CategoryChip(
    label: String,
    icon: String,
    isSelected: Boolean,
    count: Int,
    colorHex: String,
    onClick: () -> Unit
) {
    val color = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (e: Exception) { Cyan400 }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) color.copy(alpha = 0.20f) else DarkSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) color else TextMuted.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(icon, fontSize = 13.sp)
            Text(
                label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isSelected) color else TextSecondary
            )
            if (count > 0) {
                Surface(shape = CircleShape, color = color.copy(alpha = 0.15f)) {
                    Text(
                        "$count",
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = color
                    )
                }
            }
        }
    }
}
