package com.tina.app.ui

import android.app.Application
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.tina.app.ForegroundActivity
import kotlinx.coroutines.flow.first

private val KEY_CAPTURES = intPreferencesKey("captureCount")
private val KEY_REVIEW_ASKED = booleanPreferencesKey("reviewAsked")

/** The twentieth capture is the first moment the app has proven itself; ask then, and only then. */
const val REVIEW_AFTER_CAPTURES = 20

class PlayReviewPrompter(
    private val app: Application,
    private val store: DataStore<Preferences>,
) : ReviewPrompter {
    override suspend fun onCapture() {
        val prefs = store.edit { it[KEY_CAPTURES] = (it[KEY_CAPTURES] ?: 0) + 1 }
        if (prefs[KEY_REVIEW_ASKED] == true || (prefs[KEY_CAPTURES] ?: 0) != REVIEW_AFTER_CAPTURES) return
        val activity = ForegroundActivity.current ?: return
        // asked once, whatever Play decides to show; Play applies its own quota on top
        store.edit { it[KEY_REVIEW_ASKED] = true }
        runCatching {
            val manager = ReviewManagerFactory.create(app)
            manager.launchReview(activity, manager.requestReview())
        }.onFailure { Log.d("peggy.review", "review flow unavailable: ${it.message}") }
        store.data.first()
    }
}
