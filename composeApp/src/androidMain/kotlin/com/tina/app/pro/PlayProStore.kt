package com.tina.app.pro

import android.app.Application
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.tina.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

private val KEY_PRO_PLAN = stringPreferencesKey("proPlan")
private val TRIAL_PERIOD = Regex("P(\\d+)([DW])")
private const val CONNECT_TIMEOUT_MS = 8_000L
private const val TAG = "tina.pro"

/**
 * Play Billing behind [ProStore]. Purchases are acknowledged client-side and cached in the
 * settings store; the entitlement refreshes from Play on every launch, so a refund or an
 * expired subscription revokes on the next open.
 *
 * ponytail: no server-side verification yet. Add the relay's /entitlement check
 * (MONETIZATION.md §4) before hosted AI exists, since that is what a forged purchase would steal.
 */
class PlayProStore(
    private val app: Application,
    private val store: DataStore<Preferences>,
) : ProStore, PurchasesUpdatedListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var details: Map<String, ProductDetails> = emptyMap()

    override val available = MutableStateFlow(true)
    override val entitlement = MutableStateFlow<Entitlement>(Entitlement.Free)
    override val prices = MutableStateFlow<List<ProPrice>?>(null)
    override val pending = MutableStateFlow(false)

    private val client = BillingClient.newBuilder(app)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    init {
        scope.launch {
            if (BuildConfig.PRO_OVERRIDE) {
                entitlement.value = Entitlement.Pro(ProPlan.LIFETIME)
                return@launch
            }
            // the cache first, so a Pro user with no signal is still Pro
            store.data.first()[KEY_PRO_PLAN]?.let(ProPlan::fromProductId)?.let { entitlement.value = Entitlement.Pro(it) }
            val connected = connect()
            Log.d(TAG, "connected=$connected")
            if (connected) {
                refresh()
                loadPrices()
            } else {
                available.value = false
            }
        }
    }

    override fun buy(plan: ProPlan) {
        val host = com.tina.app.ForegroundActivity.current ?: return
        val product = details[plan.productId] ?: return
        val params = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product)
        if (plan.subscription) bestOffer(product)?.offerToken?.let { params.setOfferToken(it) }
        client.launchBillingFlow(
            host,
            BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(params.build())).build(),
        )
    }

    override suspend fun restore(): Boolean {
        if (!connect()) {
            available.value = false
            return false
        }
        available.value = true
        refresh()
        if (prices.value.isNullOrEmpty()) loadPrices()
        return entitlement.value is Entitlement.Pro
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK) scope.launch { refresh() }
    }

    /** True once the service is up; false on a refusal or when nothing answers in time. */
    private suspend fun connect(): Boolean = withTimeoutOrNull(CONNECT_TIMEOUT_MS) { awaitConnection() } ?: false

    private suspend fun awaitConnection(): Boolean = suspendCancellableCoroutine { cont ->
        if (client.isReady) {
            cont.resume(true)
            return@suspendCancellableCoroutine
        }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (cont.isActive) cont.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
            }

            override fun onBillingServiceDisconnected() {
                if (cont.isActive) cont.resume(false)
            }
        })
    }

    /** Reads every purchase, acknowledges the new ones, and writes what that means to the cache. */
    private suspend fun refresh() {
        val subs = client.queryPurchasesAsync(purchasesOf(BillingClient.ProductType.SUBS))
        val inapp = client.queryPurchasesAsync(purchasesOf(BillingClient.ProductType.INAPP))
        val ok = BillingClient.BillingResponseCode.OK
        // a failed read keeps the cache; only a successful "nothing" revokes
        if (subs.billingResult.responseCode != ok || inapp.billingResult.responseCode != ok) return
        var plan: ProPlan? = null
        var waiting = false
        for (purchase in subs.purchasesList + inapp.purchasesList) {
            when (purchase.purchaseState) {
                Purchase.PurchaseState.PURCHASED -> {
                    if (!purchase.isAcknowledged) {
                        client.acknowledgePurchase(
                            AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build(),
                        )
                    }
                    purchase.products.mapNotNull(ProPlan::fromProductId).forEach { plan = better(plan, it) }
                }
                Purchase.PurchaseState.PENDING -> waiting = true
            }
        }
        pending.value = waiting
        Log.d(TAG, "purchases plan=$plan pending=$waiting")
        val found = plan
        entitlement.value = found?.let { Entitlement.Pro(it) } ?: Entitlement.Free
        store.edit { if (found == null) it.remove(KEY_PRO_PLAN) else it[KEY_PRO_PLAN] = found.productId }
    }

    /** Lifetime beats a subscription; between subscriptions the longer one is what the user sees. */
    private fun better(current: ProPlan?, candidate: ProPlan): ProPlan =
        if (current == null || candidate.ordinal > current.ordinal) candidate else current

    private suspend fun loadPrices() {
        val subs = client.queryProductDetails(
            productsOf(listOf(ProPlan.MONTHLY, ProPlan.YEARLY), BillingClient.ProductType.SUBS),
        ).productDetailsList.orEmpty()
        val inapp = client.queryProductDetails(
            productsOf(listOf(ProPlan.LIFETIME), BillingClient.ProductType.INAPP),
        ).productDetailsList.orEmpty()
        details = (subs + inapp).associateBy { it.productId }
        Log.d(TAG, "products=${details.keys}")
        prices.value = ProPlan.entries.mapNotNull { plan -> details[plan.productId]?.let { priceOf(plan, it) } }
    }

    private fun priceOf(plan: ProPlan, product: ProductDetails): ProPrice {
        if (!plan.subscription) return ProPrice(plan, product.oneTimePurchaseOfferDetails?.formattedPrice ?: "", 0)
        val phases = bestOffer(product)?.pricingPhases?.pricingPhaseList.orEmpty()
        val paid = phases.lastOrNull { it.priceAmountMicros > 0 }
        val free = phases.firstOrNull { it.priceAmountMicros == 0L }
        return ProPrice(plan, paid?.formattedPrice ?: "", free?.billingPeriod?.let(::periodDays) ?: 0)
    }

    /** The offer carrying a free phase (the trial) wins; otherwise the base plan. */
    private fun bestOffer(product: ProductDetails): ProductDetails.SubscriptionOfferDetails? {
        val offers = product.subscriptionOfferDetails.orEmpty()
        return offers.firstOrNull { offer -> offer.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L } }
            ?: offers.firstOrNull()
    }

    private fun periodDays(iso: String): Int {
        val match = TRIAL_PERIOD.matchEntire(iso) ?: return 0
        val n = match.groupValues[1].toInt()
        return if (match.groupValues[2] == "W") n * 7 else n
    }

    private fun purchasesOf(type: String) = QueryPurchasesParams.newBuilder().setProductType(type).build()

    private fun productsOf(plans: List<ProPlan>, type: String) = QueryProductDetailsParams.newBuilder()
        .setProductList(
            plans.map {
                QueryProductDetailsParams.Product.newBuilder().setProductId(it.productId).setProductType(type).build()
            },
        )
        .build()
}
