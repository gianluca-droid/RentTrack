package com.renttrack.app.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.renttrack.app.billing.BillingManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SubscriptionViewModel(app: Application) : AndroidViewModel(app) {

    val billing = BillingManager(app)

    /** true se l'utente ha un abbonamento Pro attivo */
    val isPremium: StateFlow<Boolean> = billing.isPremium.stateIn(
        viewModelScope, SharingStarted.Eagerly, false
    )

    /** Lista prodotti restituita da Google Play (può essere vuota in sviluppo) */
    val products: StateFlow<List<ProductDetails>> = billing.products.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )

    init {
        billing.startConnection()
    }

    /** Avvia il flusso di acquisto Google Play */
    fun purchase(activity: Activity, productDetails: ProductDetails) {
        billing.launchBillingFlow(activity, productDetails)
    }

    /** Ripristina gli acquisti esistenti (utile su nuovo dispositivo) */
    fun restorePurchases() {
        viewModelScope.launch { billing.checkPremiumStatus() }
    }

    override fun onCleared() {
        super.onCleared()
        billing.endConnection()
    }
}
