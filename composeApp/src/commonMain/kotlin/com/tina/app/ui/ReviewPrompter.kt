package com.tina.app.ui

/**
 * Asks for a store rating at the one moment it is earned: after enough captures, once.
 * Never on first launch, never on an empty state, never after a failure.
 */
interface ReviewPrompter {
    suspend fun onCapture()
}

object NoReviewPrompter : ReviewPrompter {
    override suspend fun onCapture() {}
}
