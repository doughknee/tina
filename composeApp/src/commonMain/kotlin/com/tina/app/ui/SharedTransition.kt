package com.tina.app.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation3.ui.LocalNavAnimatedContentScope

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

/** Item titles morph between list rows and their detail screens. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedItemTitle(itemId: Long): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return this
    val animatedScope = LocalNavAnimatedContentScope.current
    return with(shared) {
        this@sharedItemTitle.sharedBounds(
            rememberSharedContentState("item-title-$itemId"),
            animatedVisibilityScope = animatedScope,
        )
    }
}
