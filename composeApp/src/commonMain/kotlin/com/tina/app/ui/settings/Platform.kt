package com.tina.app.ui.settings

/**
 * Drives which settings rows exist at all. Rows that don't apply to a platform are
 * HIDDEN, never shown-but-disabled — a dead toggle is worse than no toggle.
 */
@Suppress("NO_ACTUAL_FOR_EXPECT", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object Platform {
    val isAndroid: Boolean
    val isDesktop: Boolean
}

/** The build's version name; never a constant that drifts from Gradle. */
expect fun appVersionName(): String
