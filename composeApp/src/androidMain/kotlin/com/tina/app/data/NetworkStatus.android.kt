package com.tina.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class AndroidNetworkStatus(private val context: Context) : NetworkStatus {
    override val isUnmetered: Boolean
        get() = runCatching {
            val manager = context.getSystemService(ConnectivityManager::class.java)
            val caps = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        }.getOrDefault(true)
}
