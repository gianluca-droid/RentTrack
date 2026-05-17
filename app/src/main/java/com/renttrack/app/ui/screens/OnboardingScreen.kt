package com.renttrack.app.ui.screens

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renttrack.app.ui.theme.*
import kotlinx.coroutines.launch

// ─── Data model ──────────────────────────────────────────────────────────────
private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val description: String,
    val accentColor: Color
)

// ─── Pages definition ─────────────────────────────────────────────────────────
private val onboardingPages = listOf(
    OnboardingPage(
        icon        = Icons.Filled.Home,
        title       = "Benvenuto in RentTrack",
        subtitle    = "Il gestionale per proprietari di casa",
        description = "Accedi in un tap con Google o con email e password. I tuoi dati sono al sicuro nel cloud — sempre disponibili, su qualsiasi dispositivo.",
        accentColor = Cyan400
    ),
    OnboardingPage(
        icon        = Icons.Filled.Storefront,
        title       = "Pubblica la tua vetrina",
        subtitle    = "Annunci online per i tuoi immobili",
        description = "Crea annunci pubblici per le tue proprietà libere con foto, prezzo e dettagli. Gli interessati possono contattarti direttamente dall'app.",
        accentColor = Color(0xFF7C83FD)
    ),
    OnboardingPage(
        icon        = Icons.Filled.Business,
        title       = "Gestisci le proprietà",
        subtitle    = "Immobili, unità e inquilini",
        description = "Aggiungi i tuoi immobili, crea le unità abitative e registra ogni inquilino con i dettagli del contratto. Tutto organizzato in un unico posto.",
        accentColor = Green400
    ),
    OnboardingPage(
        icon        = Icons.Filled.EuroSymbol,
        title       = "Monitora i pagamenti",
        subtitle    = "Affitti, spese e report finanziari",
        description = "Genera avvisi di pagamento mensili, registra le spese di manutenzione e ottieni report finanziari con export CSV. Morosità sempre sotto controllo.",
        accentColor = Amber400
    )
)

// ─── Main composable ─────────────────────────────────────────────────────────
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context     = LocalContext.current
    val scope       = rememberCoroutineScope()
    val pagerState  = rememberPagerState(pageCount = { onboardingPages.size })
    val currentPage = pagerState.currentPage
    val isLastPage  = currentPage == onboardingPages.size - 1

    val currentAccent by animateColorAsState(
        targetValue = onboardingPages[currentPage].accentColor,
        animationSpec = tween(400),
        label = "accent"
    )

    fun finish() {
        com.renttrack.app.SecurePrefs.get(context)
            .edit().putBoolean("onboarding_shown", true).apply()
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkBg,
                        currentAccent.copy(alpha = 0.06f),
                        DarkBg
                    )
                )
            )
    ) {

        // ── Pulsante Salta (solo se non è l'ultima slide) ──────────────────
        if (!isLastPage) {
            TextButton(
                onClick = { finish() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 52.dp, end = 20.dp)
            ) {
                Text(
                    "Salta",
                    color = TextMuted,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Logo / App name ────────────────────────────────────────────
            Spacer(Modifier.height(56.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Filled.HomeWork,
                    contentDescription = null,
                    tint = currentAccent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "RentTrack",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = currentAccent
                )
            }

            // ── Swipeable pages ────────────────────────────────────────────
            HorizontalPager(
                state    = pagerState,
                modifier = Modifier.weight(1f)
            ) { pageIndex ->
                OnboardingPageContent(
                    page        = onboardingPages[pageIndex],
                    accentColor = currentAccent
                )
            }

            // ── Dot indicators ─────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically,
                modifier              = Modifier.padding(bottom = 28.dp)
            ) {
                onboardingPages.forEachIndexed { index, _ ->
                    val selected = index == currentPage
                    val width by animateDpAsState(
                        targetValue    = if (selected) 28.dp else 8.dp,
                        animationSpec  = tween(300),
                        label          = "dot_$index"
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (selected) currentAccent
                                else TextMuted.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            // ── CTA button ─────────────────────────────────────────────────
            Button(
                onClick = {
                    if (isLastPage) {
                        finish()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .height(56.dp),
                shape  = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = currentAccent,
                    contentColor   = Color.Black
                )
            ) {
                Text(
                    text  = if (isLastPage) "Inizia ora 🚀" else "Avanti",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
                if (!isLastPage) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}

// ─── Single page content ──────────────────────────────────────────────────────
@Composable
private fun OnboardingPageContent(page: OnboardingPage, accentColor: Color) {
    Column(
        modifier              = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.Center
    ) {

        // Icon with glow rings
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.08f))
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.14f))
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.22f))
                ) {
                    Icon(
                        imageVector     = page.icon,
                        contentDescription = null,
                        tint            = accentColor,
                        modifier        = Modifier.size(40.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(40.dp))

        Text(
            text      = page.title,
            style     = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            color     = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = accentColor.copy(alpha = 0.12f)
        ) {
            Text(
                text      = page.subtitle,
                style     = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color     = accentColor,
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text       = page.description,
            style      = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
            color      = TextSecondary,
            textAlign  = TextAlign.Center
        )
    }
}
