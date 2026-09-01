package com.tina.app.ui

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * When true, motion collapses to a plain 100ms fade everywhere. Set from the
 * Reduce motion setting (which itself defaults to the platform accessibility flag),
 * so the preference actually gates animation rather than just being stored.
 */
val LocalReduceMotion = staticCompositionLocalOf { false }

private val REDUCED: FiniteAnimationSpec<Float> = tween(100)

// Material 3 Expressive's default motion-scheme springs. MotionScheme itself is internal
// in material3 1.9.0 (public in the JVM jar, but `internal` in the KMP metadata), so the
// two token pairs are copied here — spatial moves things, effects fade them.
private const val SPATIAL_DAMPING = 0.8f
private const val SPATIAL_STIFFNESS = 380f
private const val EFFECTS_DAMPING = 1f
private const val EFFECTS_STIFFNESS = 1600f

private val SlideSpring: FiniteAnimationSpec<IntOffset> =
    spring(SPATIAL_DAMPING, SPATIAL_STIFFNESS)
private val ExpandSpring: FiniteAnimationSpec<IntSize> =
    spring(SPATIAL_DAMPING, SPATIAL_STIFFNESS)
private val FadeSpring: FiniteAnimationSpec<Float> =
    spring(EFFECTS_DAMPING, EFFECTS_STIFFNESS)

private fun fadeSwap(): ContentTransform = fadeIn(FadeSpring) togetherWith fadeOut(FadeSpring)

/**
 * The app's page transitions, all on one horizontal axis so depth reads as direction:
 * going deeper slides the full width, moving sideways between tabs slides a tenth of it,
 * and coming back is the push played in reverse.
 */
@Composable
fun rememberAppMotion(): AppMotion =
    if (LocalReduceMotion.current) AppMotion.Reduced else AppMotion.Full

enum class AppMotion {
    Full,
    Reduced,
    ;

    /** Deeper into the hierarchy: the new page arrives from the trailing edge, over the old one. */
    fun push(): ContentTransform {
        if (this == Reduced) return fadeSwap()
        return (
            slideInHorizontally(SlideSpring) { it } togetherWith
                (slideOutHorizontally(SlideSpring) { -it / 4 } + fadeOut(FadeSpring))
            ).apply { targetContentZIndex = 1f }
    }

    /**
     * Back out: the page you're leaving slides off and the one beneath catches up from its
     * parallax offset, so it has to stay *under* the outgoing page. [fromRightEdge] mirrors
     * the whole thing for a predictive-back swipe started on the right.
     */
    fun pop(fromRightEdge: Boolean = false): ContentTransform {
        if (this == Reduced) return fadeSwap()
        val sign = if (fromRightEdge) -1 else 1
        return (
            (slideInHorizontally(SlideSpring) { -sign * it / 4 } + fadeIn(FadeSpring)) togetherWith
                slideOutHorizontally(SlideSpring) { sign * it }
            ).apply { targetContentZIndex = -1f }
    }

    /** Sideways between peers (the nav bar tabs): same axis, a tenth of the distance, no depth. */
    fun lateral(forward: Boolean): ContentTransform {
        if (this == Reduced) return fadeSwap()
        val sign = if (forward) 1 else -1
        return (slideInHorizontally(SlideSpring) { sign * it / 10 } + fadeIn(FadeSpring)) togetherWith
            (slideOutHorizontally(SlideSpring) { -sign * it / 10 } + fadeOut(FadeSpring))
    }
}

/** Expand + fade normally; a bare fade when motion is reduced. */
@Composable
fun expandEnter(): EnterTransition =
    if (LocalReduceMotion.current) fadeIn(REDUCED)
    else expandVertically(ExpandSpring) + fadeIn(FadeSpring)

@Composable
fun expandExit(): ExitTransition =
    if (LocalReduceMotion.current) fadeOut(REDUCED)
    else shrinkVertically(ExpandSpring) + fadeOut(FadeSpring)
