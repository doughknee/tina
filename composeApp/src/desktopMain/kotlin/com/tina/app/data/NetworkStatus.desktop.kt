package com.tina.app.data

/** Desktop is assumed wired/Wi-Fi; there is no metered-connection concept to honour. */
class DesktopNetworkStatus : NetworkStatus {
    override val isUnmetered: Boolean = true
}
