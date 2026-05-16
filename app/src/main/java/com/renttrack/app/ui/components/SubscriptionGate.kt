package com.renttrack.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.renttrack.app.ui.screens.PaywallScreen
import com.renttrack.app.viewmodel.SubscriptionViewModel

/**
 * Composable gate: mostra il contenuto premium solo se l'utente è abbonato.
 * Altrimenti mostra la schermata Paywall.
 *
 * Uso:
 * ```
 * SubscriptionGate(subscriptionViewModel) {
 *     ReportsScreen(viewModel)
 * }
 * ```
 */
@Composable
fun SubscriptionGate(
    subscriptionViewModel: SubscriptionViewModel,
    onDismiss: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val isPremium by subscriptionViewModel.isPremium.collectAsState()

    if (isPremium) {
        content()
    } else {
        PaywallScreen(
            subscriptionViewModel = subscriptionViewModel,
            onDismiss = onDismiss
        )
    }
}
