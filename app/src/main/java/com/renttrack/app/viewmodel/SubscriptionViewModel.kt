package com.renttrack.app.viewmodel

import android.app.Activity
import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.renttrack.app.billing.BillingManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SubscriptionViewModel(app: Application) : AndroidViewModel(app) {

    val billing = BillingManager(app)

    /** true solo in debug build — permette di simulare il piano Free per i test */
    val isDebugBuild = (app.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    /** Quando true (solo debug), forza isPremium=false per testare il flusso Free */
    private val _simulateFreePlan = MutableStateFlow(false)
    val simulateFreePlan: StateFlow<Boolean> = _simulateFreePlan.asStateFlow()

    /** true se l'utente ha un abbonamento Pro attivo */
    val isPremium: StateFlow<Boolean> = combine(
        billing.isPremium,
        _simulateFreePlan
    ) { billingPremium, simFree ->
        if (simFree) false else billingPremium
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

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

    /** Solo in debug: attiva/disattiva la simulazione del piano Free */
    fun toggleDebugFreePlan(simulate: Boolean) {
        if (isDebugBuild) _simulateFreePlan.value = simulate
    }

    override fun onCleared() {
        super.onCleared()
        billing.endConnection()
    }
}
