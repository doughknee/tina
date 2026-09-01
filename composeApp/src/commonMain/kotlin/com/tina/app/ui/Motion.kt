package com.tina.app.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * When true, motion collapses to a plain 100ms fade everywhere. Set from the
 * Reduce motion setting (which itself defaults to the platform accessibility flag),
 * so the preference actually gates animation rather than just being stored.
 */
val LocalReduceMotion = staticCompositionLocalOf { false }

private val REDUCED: FiniteAnimationSpec<Float> = tween(100)

/** Expand + fade normally; a bare fade when motion is reduced. */
@Composable
fun expandEnter(): EnterTransition =
    if (LocalReduceMotion.current) fadeIn(REDUCED) else expandVertically() + fadeIn()

@Composable
fun expandExit(): ExitTransition =
    if (LocalReduceMotion.current) fadeOut(REDUCED) else shrinkVertically() + fadeOut()
