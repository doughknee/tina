package com.tina.app.data

/** Lets the "Wi-Fi only" setting hold cloud AI calls off metered connections. */
interface NetworkStatus {
    val isUnmetered: Boolean
}
