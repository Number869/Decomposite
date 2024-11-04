package com.nxoim.decomposite.core.common.navigation.animations.stack

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterExitState.PostExit
import androidx.compose.animation.EnterExitState.PreEnter
import androidx.compose.animation.EnterExitState.Visible
import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.animation.core.InternalAnimationApi
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.createChildTransition
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy
import androidx.compose.ui.util.fastMaxOfOrNull
import androidx.compose.ui.zIndex
import com.arkivanov.decompose.InternalDecomposeApi
import com.arkivanov.decompose.hashString
import com.nxoim.decomposite.core.common.navigation.animations.AnimationType.Companion.passiveCancelling
import com.nxoim.decomposite.core.common.navigation.animations.ItemLocation.Companion.top

/**
 * Animates the stack. Caches the children for the time of animation.
 */
@Composable
fun <Key : Any, Instance : Any> StackAnimator(
    stackAnimatorScope: StackAnimatorScope<Key, Instance>,
    modifier: Modifier = Modifier,
    blockInputForNonTopItems: Boolean = true,
    content: @Composable AnimatedVisibilityScope.(child: Instance) -> Unit,
    // a key is needed because of something related
    // to generation of composition hash keys (idk). animation data registry
    // is expected to be a retained instance, so if theres an accidental
    // recomposition affecting the instance of the stack animator scope
    // - the stack animator instance won't be affected since the animation
    // data registry instance will always stay the same
) = key(stackAnimatorScope.animationDataRegistry) {
    val holder = rememberSaveableStateHolder()

    LaunchedEffect(Unit) { stackAnimatorScope.observeAndUpdateAnimatorData() }

    if (stackAnimatorScope.visibleCachedChildren.isNotEmpty()) Box(modifier) {
        val seekableTransitionState = remember {
            SeekableTransitionState(
                stackAnimatorScope.itemKey(stackAnimatorScope.sourceStack.last())
            )
        }
        val transition = rememberTransition(seekableTransitionState)

        stackAnimatorScope.visibleCachedChildren.forEach { (childKey, cachedInstance) ->
            // keys are needed for animations to work. something something
            // correctness something something control flow
            key(childKey) {
                val state by stackAnimatorScope.createStackItemState(childKey)

                val animationScopes = state.animationData.scopes()
                // for seekable transitions
                val firstAnimData = animationScopes.values.first()
                val animationStatus = firstAnimData.animationStatus

                DisposableEffect(Unit) {
                    onDispose {
                        if (!state.inStack) holder.removeState(childHolderKey(childKey))
                    }
                }

                if (state.allowingAnimation) {
                    LaunchedEffect(animationStatus.animating) {
                        if (animationStatus.location.top && !animationStatus.animating) {
                            seekableTransitionState.snapTo(childKey)
                        }
                    }

                    LaunchedEffect(animationStatus) {
                        snapshotFlow { firstAnimData.animationProgressForScope }
                            .collect() { progress ->
                                if (animationStatus.fromBackIntoTop) {
                                    seekableTransitionState.seekTo(
                                        (1f - progress).coerceIn(0f, 1f),
                                        childKey
                                    )
                                } else if (animationStatus.fromOutsideIntoTop) {
                                    seekableTransitionState.seekTo(
                                        (1f + progress).coerceIn(0f, 1f),
                                        childKey
                                    )
                                } else if (animationStatus.run { animationType.passiveCancelling && location.top }) {
                                    seekableTransitionState.seekTo((-progress).coerceIn(0f, 1f))
                                }
                            }
                    }

                    AnimatedVisibilityScopeProvider(
                        transition,
                        visible = { it == childKey },
                        Modifier
                            .accumulate {
                                remember(animationScopes) {
                                    state.animationData.modifiers()
                                }
                            }
                            .zIndex((-state.indexFromTop).toFloat()),
                        displaying = { state.displaying },
                    ) {
                        holder.SaveableStateProvider(childHolderKey(childKey)) {
                            content(cachedInstance)
                        }

                        if (state.indexFromTop != 0 && blockInputForNonTopItems) Box(
                            Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            event.changes.forEach { it.consume() }
                                        }
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
}

@Stable
@Composable
private inline fun Modifier.accumulate(modifiers: () -> List<Modifier>) =
    modifiers().fold(initial = this) { acc, modifier -> acc.then(modifier) }

@OptIn(InternalDecomposeApi::class)
private fun <C : Any> childHolderKey(child: C) =
    child.hashString() + " StackAnimator SaveableStateHolder"

// This converts Boolean visible to EnterExitState
@Composable
private inline fun <T> Transition<T>.targetEnterExit(
    visible: (T) -> Boolean,
    targetState: T
): EnterExitState = key(this) {
    if (this.isSeeking) {
        if (visible(targetState)) {
            Visible
        } else {
            if (visible(this.currentState)) PostExit else PreEnter
        }
    } else {
        val hasBeenVisible = remember { mutableStateOf(false) }

        if (visible(currentState)) hasBeenVisible.value = true

        if (visible(targetState)) {
            Visible
        } else {
            // If never been visible, visible = false means PreEnter, otherwise PostExit
            if (hasBeenVisible.value) PostExit else PreEnter
        }
    }
}


private val Transition<EnterExitState>.exitFinished
    get() = currentState == PostExit && targetState == PostExit


private class AnimatedVisibilityScopeImpl(
    _transition: Transition<EnterExitState>
) : AnimatedVisibilityScope {
    override var transition = _transition
    val targetSize = mutableStateOf(IntSize.Zero)
}

private class AnimatedEnterExitMeasurePolicy(
    val scope: AnimatedVisibilityScopeImpl
) : MeasurePolicy {
    var hasLookaheadOccurred = false
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        val placeables = measurables.fastMap { it.measure(constraints) }
        val maxWidth: Int = placeables.fastMaxBy { it.width }?.width ?: 0
        val maxHeight = placeables.fastMaxBy { it.height }?.height ?: 0
        // Position the children.
        if (isLookingAhead) {
            hasLookaheadOccurred = true
            scope.targetSize.value = IntSize(maxWidth, maxHeight)
        } else if (!hasLookaheadOccurred) {
            // Not in lookahead scope.
            scope.targetSize.value = IntSize(maxWidth, maxHeight)
        }
        return layout(maxWidth, maxHeight) {
            placeables.fastForEach {
                it.place(0, 0)
            }
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int
    ) = measurables.fastMaxOfOrNull { it.minIntrinsicWidth(height) } ?: 0

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int
    ) = measurables.fastMaxOfOrNull { it.minIntrinsicHeight(width) } ?: 0

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int
    ) = measurables.fastMaxOfOrNull { it.maxIntrinsicWidth(height) } ?: 0

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int
    ) = measurables.fastMaxOfOrNull { it.maxIntrinsicHeight(width) } ?: 0
}

@OptIn(InternalAnimationApi::class, ExperimentalTransitionApi::class)
@Composable
private fun <T> AnimatedVisibilityScopeProvider(
    transition: Transition<T>,
    visible: (T) -> Boolean,
    modifier: Modifier,
    displaying: () -> Boolean = {
        visible(transition.targetState) || visible(transition.currentState) ||
                transition.isSeeking || transition.hasInitialValueAnimations
    },
    content: @Composable() AnimatedVisibilityScope.() -> Unit
) {
    if (displaying()) {
        val childTransition = transition.createChildTransition(label = "EnterExitTransition") {
            transition.targetEnterExit(visible, it)
        }

        if (!childTransition.exitFinished) {
            val scope = remember(transition) { AnimatedVisibilityScopeImpl(childTransition) }

            Layout(
                content = { scope.content() },
                modifier = modifier,
                measurePolicy = remember { AnimatedEnterExitMeasurePolicy(scope) }
            )
        }
    }
}