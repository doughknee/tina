package com.tina.app.ui

/**
 * Asks for a store rating at the one moment it is earned: after enough captures, once.
 * Never on first launch, never on an empty state, never after a failure.
 */
/** The capture that earns the one store ask; PlayReviewPrompter counts to it. */
const val REVIEW_AFTER_CAPTURES_UI = 20

interface ReviewPrompter {
    suspend fun onCapture()

    /** Developer options: launch the store flow regardless of the count. True when a flow was started. */
    suspend fun promptNow(): Boolean = false

    /** Developer options: forget the once-only ask so the real trigger fires again. */
    suspend fun resetAsk() {}
}

object NoReviewPrompter : ReviewPrompter {
    override suspend fun onCapture() {}
}
