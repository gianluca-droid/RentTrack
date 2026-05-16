package com.renttrack.app.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.renttrack.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Gestisce la connessione a Google Play Billing e lo stato dell'abbonamento Pro.
 *
 * Product IDs configurati in Google Play Console:
 *   - renttrack_pro_monthly  → €4,99/mese (trial 7 giorni)
 *   - renttrack_pro_yearly   → €39,99/anno (trial 7 giorni)
 *
 * ⚠️ In DEBUG build, isPremium è sempre TRUE per facilitare i test.
 *    In RELEASE build usa il billing reale di Google Play.
 */
class BillingManager(context: Context) : PurchasesUpdatedListener {

    companion object {
        const val PRODUCT_MONTHLY = "renttrack_pro_monthly"
        const val PRODUCT_YEARLY  = "renttrack_pro_yearly"
        private const val TAG = "BillingManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    // ── State flows ──────────────────────────────────────────────────────────
    // In DEBUG: sempre Pro per facilitare i test dello sviluppatore
    private val _isPremium = MutableStateFlow(BuildConfig.DEBUG)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products.asStateFlow()

    private val _billingReady = MutableStateFlow(false)
    val billingReady: StateFlow<Boolean> = _billingReady.asStateFlow()

    // ── Connection ───────────────────────────────────────────────────────────
    fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing connected")
                    _billingReady.value = true
                    scope.launch {
                        queryProducts()
                        checkPremiumStatus()
                    }
                } else {
                    Log.e(TAG, "Billing setup failed: ${result.debugMessage}")
                }
            }
            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing disconnected — retry")
                _billingReady.value = false
                startConnection() // auto-reconnect
            }
        })
    }

    fun endConnection() = billingClient.endConnection()

    // ── Queries ──────────────────────────────────────────────────────────────
    private suspend fun queryProducts() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_YEARLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        val result = billingClient.queryProductDetails(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _products.value = result.productDetailsList ?: emptyList()
            Log.d(TAG, "Products loaded: ${_products.value.map { it.productId }}")
        }
    }

    fun checkPremiumStatus() {
        scope.launch {
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
            val result = billingClient.queryPurchasesAsync(params)
            if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val active = result.purchasesList.any { purchase ->
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                _isPremium.value = active
                Log.d(TAG, "Premium status: $active")
            }
        }
    }

    // ── Launch billing flow ──────────────────────────────────────────────────
    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails): BillingResult {
        val offerToken = productDetails
            .subscriptionOfferDetails
            ?.firstOrNull()
            ?.offerToken
            ?: return BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.DEVELOPER_ERROR)
                .setDebugMessage("No offer token found")
                .build()

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()
        return billingClient.launchBillingFlow(activity, flowParams)
    }

    // ── PurchasesUpdatedListener ─────────────────────────────────────────────
    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        acknowledgePurchase(purchase)
                        _isPremium.value = true
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED ->
                Log.d(TAG, "User cancelled purchase")
            else ->
                Log.e(TAG, "Purchase error: ${result.debugMessage}")
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        scope.launch {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            val result = billingClient.acknowledgePurchase(params)
            Log.d(TAG, "Acknowledge result: ${result.responseCode}")
        }
    }
}
