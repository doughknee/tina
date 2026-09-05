package com.tina.app.pro

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.tina.app.resources.Res
import com.tina.app.resources.pro_active_title
import com.tina.app.resources.pro_buy
import com.tina.app.resources.pro_lifetime
import com.tina.app.resources.pro_lifetime_note
import com.tina.app.resources.pro_loading
import com.tina.app.resources.pro_manage
import com.tina.app.resources.pro_monthly
import com.tina.app.resources.pro_none
import com.tina.app.resources.pro_pending
import com.tina.app.resources.pro_pitch_ai
import com.tina.app.resources.pro_pitch_coming
import com.tina.app.resources.pro_pitch_quota
import com.tina.app.resources.pro_quota
import com.tina.app.resources.pro_plan_active
import com.tina.app.resources.pro_restore
import com.tina.app.resources.pro_restore_none
import com.tina.app.resources.pro_restored
import com.tina.app.resources.pro_start_trial
import com.tina.app.resources.pro_terms
import com.tina.app.resources.pro_title
import com.tina.app.resources.pro_trial_note
import com.tina.app.resources.pro_unavailable
import com.tina.app.resources.pro_yearly
import com.tina.app.resources.pro_yearly_note
import com.tina.app.ui.settings.subpages.SettingsSubpageScaffold
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun ProPlan.label(): String = when (this) {
    ProPlan.MONTHLY -> stringResource(Res.string.pro_monthly)
    ProPlan.YEARLY -> stringResource(Res.string.pro_yearly)
    ProPlan.LIFETIME -> stringResource(Res.string.pro_lifetime)
}

/**
 * The one paywall (MONETIZATION.md §6): three prices, the trial called out, restore, and
 * back as "not now". A settings subpage, never a pop-up.
 */
@Composable
fun PaywallScreen(onBack: () -> Unit, store: ProStore = koinInject(), http: io.ktor.client.HttpClient = koinInject()) {
    val entitlement by store.entitlement.collectAsState()
    val prices by store.prices.collectAsState()
    val pending by store.pending.collectAsState()
    val available by store.available.collectAsState()
    var selected by remember { mutableStateOf(ProPlan.YEARLY) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current
    val restoredText = stringResource(Res.string.pro_restored)
    val restoreNoneText = stringResource(Res.string.pro_restore_none)

    SettingsSubpageScaffold(
        title = stringResource(Res.string.pro_title),
        onBack = onBack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Only what ships is promised here. Themes, icons and export return to this
                // list as each one lands, not before (MONETIZATION.md §7).
                Pitch(Icons.Outlined.AutoAwesome, stringResource(Res.string.pro_pitch_ai))
                Pitch(Icons.Outlined.History, stringResource(Res.string.pro_pitch_quota))
                Pitch(Icons.Outlined.Palette, stringResource(Res.string.pro_pitch_coming))
            }
        }
        when (val current = entitlement) {
            is Entitlement.Pro -> item {
                // the relay's own count, so the number here is the number that gates requests
                var quota by remember(current) { mutableStateOf<com.tina.app.ai.HostedQuota?>(null) }
                LaunchedEffect(current) { quota = com.tina.app.ai.fetchHostedQuota(http, current) }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                            Text(stringResource(Res.string.pro_active_title), style = MaterialTheme.typography.titleMediumEmphasized)
                        }
                        Text(stringResource(Res.string.pro_plan_active, current.plan.label()), style = MaterialTheme.typography.bodyMedium)
                        quota?.let { q ->
                            Text(
                                stringResource(Res.string.pro_quota, q.askUsed, q.askLimit, q.lightUsed, q.lightLimit),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (current.plan.subscription) {
                            TextButton(onClick = {
                                uriHandler.openUri(
                                    "https://play.google.com/store/account/subscriptions?sku=${current.plan.productId}&package=com.peggy.app",
                                )
                            }) { Text(stringResource(Res.string.pro_manage)) }
                        }
                    }
                }
            }
            Entitlement.Free -> {
                item {
                    when {
                        !available -> Text(
                            stringResource(Res.string.pro_unavailable),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        prices == null -> Text(
                            stringResource(Res.string.pro_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        prices.orEmpty().isEmpty() -> Text(
                            stringResource(Res.string.pro_none),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            prices.orEmpty().forEach { price ->
                                PlanCard(price, selected = price.plan == selected, onClick = { selected = price.plan })
                            }
                        }
                    }
                }
                item {
                    val chosen = prices?.firstOrNull { it.plan == selected }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(
                            enabled = chosen != null && !pending,
                            onClick = { store.buy(selected) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (chosen != null && chosen.trialDays > 0) stringResource(Res.string.pro_start_trial, chosen.trialDays)
                                else stringResource(Res.string.pro_buy),
                            )
                        }
                        if (pending) {
                            Text(
                                stringResource(Res.string.pro_pending),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(
                            enabled = available,
                            onClick = {
                                scope.launch {
                                    snackbarHostState.showSnackbar(if (store.restore()) restoredText else restoreNoneText)
                                }
                            },
                        ) { Text(stringResource(Res.string.pro_restore)) }
                        Text(
                            stringResource(Res.string.pro_terms),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Pitch(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PlanCard(price: ProPrice, selected: Boolean, onClick: () -> Unit) {
    val note = when (price.plan) {
        ProPlan.MONTHLY, ProPlan.YEARLY ->
            if (price.trialDays > 0) stringResource(Res.string.pro_trial_note, price.trialDays, price.price)
            else if (price.plan == ProPlan.YEARLY) stringResource(Res.string.pro_yearly_note) else null
        ProPlan.LIFETIME -> stringResource(Res.string.pro_lifetime_note)
    }
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(price.plan.label(), style = MaterialTheme.typography.titleMediumEmphasized)
                note?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.size(12.dp))
            Text(price.price, style = MaterialTheme.typography.titleMedium)
        }
    }
}
