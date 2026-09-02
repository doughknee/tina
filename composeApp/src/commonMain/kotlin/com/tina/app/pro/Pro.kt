package com.tina.app.pro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.compose.koinInject

/** The three Play products from docs/MONETIZATION.md §2. */
enum class ProPlan(val productId: String) {
    MONTHLY("tina_pro_monthly"),
    YEARLY("tina_pro_yearly"),
    LIFETIME("tina_pro_lifetime");

    val subscription: Boolean get() = this != LIFETIME

    companion object {
        fun fromProductId(id: String): ProPlan? = entries.firstOrNull { it.productId == id }
    }
}

sealed interface Entitlement {
    data object Free : Entitlement

    /** A free trial is an active subscription as far as the store is concerned; Play ends it. */
    data class Pro(val plan: ProPlan) : Entitlement
}

/** One plan as the store prices it for this account, e.g. "$3.99", with the trial its offer carries. */
data class ProPrice(val plan: ProPlan, val price: String, val trialDays: Int)

/**
 * The store behind tina Pro. Android talks to Play Billing; desktop has no store yet and
 * stays Free. The entitlement is cached on the device so a Pro user offline stays Pro.
 */
interface ProStore {
    /** False where nothing can be bought (desktop, a device without Play): the paywall says so. */
    val available: StateFlow<Boolean>
    val entitlement: StateFlow<Entitlement>
    /** Null until the store answers; empty when it has no plans on sale (products not live yet). */
    val prices: StateFlow<List<ProPrice>?>

    /** A purchase Play has not finished yet (slow card, parental approval). */
    val pending: StateFlow<Boolean>

    /** Opens the store's purchase flow; the outcome lands in [entitlement]. */
    fun buy(plan: ProPlan)

    /** Re-reads purchases from the store (reinstall, new device); true when Pro was found. */
    suspend fun restore(): Boolean
}

object NoProStore : ProStore {
    override val available = MutableStateFlow(false)
    override val entitlement = MutableStateFlow<Entitlement>(Entitlement.Free)
    override val prices = MutableStateFlow<List<ProPrice>?>(emptyList())
    override val pending = MutableStateFlow(false)
    override fun buy(plan: ProPlan) {}
    override suspend fun restore() = false
}

@Composable
fun rememberEntitlement(): State<Entitlement> = koinInject<ProStore>().entitlement.collectAsState()

/** For the features MONETIZATION.md §7 lists: `if (!isPro) onOpenPaywall() else …`. */
@Composable
fun rememberIsPro(): Boolean = rememberEntitlement().value is Entitlement.Pro
