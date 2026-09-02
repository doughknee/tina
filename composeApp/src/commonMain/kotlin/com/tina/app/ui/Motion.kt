package com.tina.app.ui

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.IntOffset

/**
 * When true, motion collapses to a plain 100ms fade everywhere. Set from the
 * Reduce motion setting (which itself defaults to the platform accessibility flag),
 * so the preference actually gates animation rather than just being stored.
 */
val LocalReduceMotion = staticCompositionLocalOf { false }

private val REDUCED: FiniteAnimationSpec<Float> = tween(100)

/**
 * The app's transitions, all on the theme's expressive [androidx.compose.material3.MotionScheme]:
 * spatial springs move things, effects springs fade them. Pages share one horizontal axis so
 * depth reads as direction; sheets rise from the bottom edge.
 */
@Immutable
class AppMotion internal constructor(
    private val slide: FiniteAnimationSpec<IntOffset>,
    private val scale: FiniteAnimationSpec<Float>,
    private val fade: FiniteAnimationSpec<Float>,
    val reduced: Boolean,
) {
    /** A plain cross-fade on the effects spring; what everything collapses to under reduce motion. */
    fun fadeSwap(): ContentTransform = fadeIn(fade) togetherWith fadeOut(fade)

    /** Deeper into the hierarchy: the new page arrives from the trailing edge, over the old one. */
    fun push(): ContentTransform {
        if (reduced) return fadeSwap()
        return (
            slideInHorizontally(slide) { it } togetherWith
                (slideOutHorizontally(slide) { -it / 4 } + fadeOut(fade))
            ).apply { targetContentZIndex = 1f }
    }

    /**
     * Back out: the page you're leaving slides off and the one beneath catches up from its
     * parallax offset, so it has to stay *under* the outgoing page. [fromRightEdge] mirrors
     * the whole thing for a predictive-back swipe started on the right.
     */
    fun pop(fromRightEdge: Boolean = false): ContentTransform {
        if (reduced) return fadeSwap()
        val sign = if (fromRightEdge) -1 else 1
        return (
            (slideInHorizontally(slide) { -sign * it / 4 } + fadeIn(fade)) togetherWith
                slideOutHorizontally(slide) { sign * it }
            ).apply { targetContentZIndex = -1f }
    }

    /** Sideways between peers (the nav bar tabs): same axis, a tenth of the distance, no depth. */
    fun lateral(forward: Boolean): ContentTransform {
        if (reduced) return fadeSwap()
        val sign = if (forward) 1 else -1
        return (slideInHorizontally(slide) { sign * it / 10 } + fadeIn(fade)) togetherWith
            (slideOutHorizontally(slide) { -sign * it / 10 } + fadeOut(fade))
    }

    /** A sheet rising from the bottom edge (Ask, the capture sheet). */
    fun sheetEnter(): EnterTransition =
        if (reduced) fadeIn(REDUCED) else slideInVertically(slide) { it } + fadeIn(fade)

    fun sheetExit(): ExitTransition =
        if (reduced) fadeOut(REDUCED) else slideOutVertically(slide) { it } + fadeOut(fade)

    /** Something small popping into place (the send button, a chip). */
    fun popIn(): EnterTransition = if (reduced) fadeIn(REDUCED) else scaleIn(scale) + fadeIn(fade)

    fun popOut(): ExitTransition = if (reduced) fadeOut(REDUCED) else scaleOut(scale) + fadeOut(fade)

    fun fadeEnter(): EnterTransition = fadeIn(if (reduced) REDUCED else fade)

    fun fadeExit(): ExitTransition = fadeOut(if (reduced) REDUCED else fade)
}

@Composable
fun rememberAppMotion(): AppMotion {
    val scheme = MaterialTheme.motionScheme
    val reduced = LocalReduceMotion.current
    return remember(scheme, reduced) {
        AppMotion(
            slide = scheme.defaultSpatialSpec(),
            scale = scheme.fastSpatialSpec(),
            fade = scheme.defaultEffectsSpec(),
            reduced = reduced,
        )
    }
}

/** Expand + fade normally; a bare fade when motion is reduced. */
@Composable
fun expandEnter(): EnterTransition {
    if (LocalReduceMotion.current) return fadeIn(REDUCED)
    val scheme = MaterialTheme.motionScheme
    return expandVertically(scheme.defaultSpatialSpec()) + fadeIn(scheme.defaultEffectsSpec())
}

@Composable
fun expandExit(): ExitTransition {
    if (LocalReduceMotion.current) return fadeOut(REDUCED)
    val scheme = MaterialTheme.motionScheme
    return shrinkVertically(scheme.defaultSpatialSpec()) + fadeOut(scheme.defaultEffectsSpec())
}
