package com.renttrack.app.ui.screens

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.billingclient.api.ProductDetails
import com.renttrack.app.billing.BillingManager
import com.renttrack.app.ui.theme.*
import com.renttrack.app.viewmodel.SubscriptionViewModel

// ─── Colori esclusivi Paywall ────────────────────────────────────────────────
private val ProGold    = Color(0xFFFFD700)
private val ProGoldSoft = Color(0xFFFFC947)
private val IndigoDark = Color(0xFF0D0F1A)
private val IndigoMid  = Color(0xFF131628)
private val PurpleAccent = Color(0xFF7C3AED)

@Composable
fun PaywallScreen(
    subscriptionViewModel: SubscriptionViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val products by subscriptionViewModel.products.collectAsState()

    val monthlyProduct = products.find { it.productId == BillingManager.PRODUCT_MONTHLY }
    val yearlyProduct  = products.find { it.productId == BillingManager.PRODUCT_YEARLY }

    // Prezzi fallback (se prodotti non ancora caricati da Play)
    val monthlyPrice = monthlyProduct
        ?.subscriptionOfferDetails?.firstOrNull()
        ?.pricingPhases?.pricingPhaseList?.lastOrNull()
        ?.formattedPrice ?: "€4,99"
    val yearlyPrice = yearlyProduct
        ?.subscriptionOfferDetails?.firstOrNull()
        ?.pricingPhases?.pricingPhaseList?.lastOrNull()
        ?.formattedPrice ?: "€39,99"

    var selectedPlan by remember { mutableStateOf("yearly") } // default annuale

    // Animazione pulse sul badge "Pro"
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(IndigoDark, IndigoMid, Color(0xFF0A1628))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            // ── Badge PRO ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size((60 * pulseScale).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(ProGoldSoft, PurpleAccent))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Stars, null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "RentTrack",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = Color.White
            )
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = ProGoldSoft, fontWeight = FontWeight.Bold)) {
                        append("PRO")
                    }
                    append(" — Gestione immobili senza limiti")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            // ── Feature list ─────────────────────────────────────────────
            PaywallFeatureList()

            Spacer(Modifier.height(28.dp))

            // ── Piano mensile ─────────────────────────────────────────────
            PlanCard(
                title = "Mensile",
                price = monthlyPrice,
                subtitle = "Rinnovo automatico mensile",
                badge = null,
                selected = selectedPlan == "monthly",
                onClick = { selectedPlan = "monthly" }
            )

            Spacer(Modifier.height(10.dp))

            // ── Piano annuale (evidenziato) ───────────────────────────────
            PlanCard(
                title = "Annuale",
                price = yearlyPrice,
                subtitle = "~€3,33/mese · Risparmia il 33%",
                badge = "🏆 Più conveniente",
                selected = selectedPlan == "yearly",
                onClick = { selectedPlan = "yearly" }
            )

            Spacer(Modifier.height(8.dp))

            // Trial badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Green400.copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, null, tint = Green400, modifier = Modifier.size(14.dp))
                    Text(
                        "7 giorni di prova gratuita · Disdici quando vuoi",
                        style = MaterialTheme.typography.labelSmall,
                        color = Green400
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── CTA Button ───────────────────────────────────────────────
            Button(
                onClick = {
                    if (activity != null) {
                        val product = if (selectedPlan == "yearly") yearlyProduct else monthlyProduct
                        if (product != null) {
                            subscriptionViewModel.purchase(activity, product)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ProGoldSoft,
                    contentColor = Color(0xFF1A0A00)
                ),
                elevation = ButtonDefaults.buttonElevation(8.dp)
            ) {
                Icon(Icons.Filled.Stars, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Inizia 7 giorni gratis",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Ripristina acquisti
            TextButton(onClick = { subscriptionViewModel.restorePurchases() }) {
                Text(
                    "Ripristina acquisti",
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.labelMedium
                )
            }

            // Torna indietro
            TextButton(onClick = onDismiss) {
                Text(
                    "Continua con la versione gratuita",
                    color = Color.White.copy(alpha = 0.35f),
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Pagamento gestito da Google Play · Nessun addebito oggi",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.25f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Feature list composable ──────────────────────────────────────────────────
@Composable
private fun PaywallFeatureList() {
    val features = listOf(
        Triple(Icons.Filled.Business,        "Immobili illimitati",                   true),
        Triple(Icons.Filled.People,          "Inquilini illimitati",                  true),
        Triple(Icons.Filled.Description,     "Avvisi affitto illimitati",             true),
        Triple(Icons.Filled.BarChart,        "Reports finanziari completi",           true),
        Triple(Icons.Filled.FolderOpen,      "Archivio documenti",                    true),
        Triple(Icons.Filled.PictureAsPdf,    "Generazione PDF cedolini",              true),
        Triple(Icons.Filled.FileDownload,    "Export CSV",                            true),
        Triple(Icons.Filled.TrendingUp,      "⭐ Annunci in cima alla vetrina",       true),
        Triple(Icons.Filled.Widgets,         "Widget schermata home",                 true),
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.05f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            features.forEach { (icon, label, included) ->
                FeatureRow(icon = icon, label = label, included = included)
                if (label != features.last().second) {
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.06f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, label: String, included: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon, null,
            tint = if (included) ProGoldSoft else Color.White.copy(alpha = 0.2f),
            modifier = Modifier.size(18.dp)
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = if (included) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.3f),
            modifier = Modifier.weight(1f)
        )
        Icon(
            if (included) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
            null,
            tint = if (included) Green400 else Color.White.copy(alpha = 0.2f),
            modifier = Modifier.size(16.dp)
        )
    }
}

// ── Plan selection card ──────────────────────────────────────────────────────
@Composable
private fun PlanCard(
    title: String,
    price: String,
    subtitle: String,
    badge: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) ProGoldSoft else Color.White.copy(alpha = 0.12f)
    val bgColor     = if (selected) ProGoldSoft.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.04f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = ProGoldSoft,
                    unselectedColor = Color.White.copy(alpha = 0.3f)
                )
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    badge?.let {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ProGoldSoft.copy(alpha = 0.18f)
                        ) {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = ProGoldSoft,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.55f)
                )
            }
            Text(
                price,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = if (selected) ProGoldSoft else Color.White.copy(alpha = 0.7f)
            )
        }
    }
}
