package com.number869.decomposite.core.common.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.stack.animation.*
import com.arkivanov.decompose.router.stack.items
import com.number869.decomposite.core.common.navigation.animations.*
import com.number869.decomposite.core.common.ultils.*
import kotlinx.serialization.serializer

/**
 * Navigation Host.
 * [router] is where you declare the content of each destination.
 * [routedContent] is where the content is displayed, where you put your scaffold, maybe something else.
 * When a host is created - a navigation controller is created for it, and it is accessible in [routedContent],
 * however you can make your own controller by requesting [LocalNavControllerStore]
 * and calling [NavControllerStore.getOrCreate] with the type of your destination.
 *
 *
 * You can provide your own animations for the content and overlaying content.
 * [containedContentAnimation] and [overlayingContentAnimation] are decompose animations.
 */
@Stable
@Composable
inline fun <reified C : Any> NavHost(
    startingDestination: C,
    startingNavControllerInstance: NavController<C> = navController(startingDestination),
    defaultAnimation: ContentAnimator = cleanSlideAndFade(),
    crossinline routedContent: @Composable NavController<C>.(content: @Composable (Modifier) -> Unit) -> Unit = { it(Modifier) },
    crossinline router: @Composable NavigationItem.(child: C) -> Unit
) {
    with(remember { SharedBackEventScope() }) {
        var backHandlerEnabled by remember { mutableStateOf(false) }

        CompositionLocalProvider(LocalContentAnimator provides defaultAnimation) {
            CompositionLocalProvider(LocalContentType provides ContentType.Contained) {
                startingNavControllerInstance.routedContent { modifier ->
                    CustomStackAnimator(
                        startingNavControllerInstance.screenStack,
                        modifier,
                        sharedBackEventScope = this,
                        onBackstackEmpty = { backHandlerEnabled = it }
                    ) {
                        CompositionLocalProvider(
                            LocalComponentContext provides it.instance.componentContext
                        ) {
                            router(it.configuration)
                        }
                    }
                }
            }

            LocalNavigationRoot.current.overlay {
                CompositionLocalProvider(LocalContentType provides ContentType.Overlay) {
                    CustomStackAnimator(
                        startingNavControllerInstance.overlayStack,
                        onBackstackEmpty = {
                            backHandlerEnabled = if (it)
                                it
                            else
                                startingNavControllerInstance.screenStack.items.size > 1
                        },
                        sharedBackEventScope = this,
                        excludeStartingDestination = true
                    ) {
                        CompositionLocalProvider(
                            LocalComponentContext provides it.instance.componentContext
                        ) {
                            runCatching { router(it.configuration) }
                        }
                    }
                }

                // snacks dont need to be aware of gestures
                CustomStackAnimator(
                    startingNavControllerInstance.snackStack,
                    onBackstackEmpty = {},
                    sharedBackEventScope = SharedBackEventScope(),
                    excludeStartingDestination = true
                ) {
                    CompositionLocalProvider(LocalComponentContext provides it.instance.componentContext) {
                        animatedDestination(
                            startingNavControllerInstance.animationsForDestinations[it.configuration] ?: emptyAnimation()
                        ) {
                            startingNavControllerInstance.contentOfSnacks[it.configuration]?.invoke()

                            DisposableEffect(it) {
                                onDispose { startingNavControllerInstance.removeSnackContents(it.configuration) }
                            }
                        }
                    }
                }
            }
        }

        BackGestureHandler(
            enabled = backHandlerEnabled,
            LocalComponentContext.current.backHandler,
            onBackStarted = { onBackStarted(it) },
            onBackProgressed = { onBackProgressed(it) },
            onBackCancelled = { onBackCancelled() },
            onBack = {
                startingNavControllerInstance.navigateBack()
                onBack()
            }
        )
    }
}

@Composable
inline fun <reified C : Any> NavHost(
    startingDestination: C,
    startingNavControllerInstance: NavController<C> = navController(startingDestination),
    crossinline animations: NavigationItem.(child: C) -> ContentAnimator,
    crossinline routedContent: @Composable NavController<C>.(content: @Composable (Modifier) -> Unit) -> Unit = { it(Modifier) },
    crossinline router: @Composable (child: C) -> Unit
) {
    NavHost(
        startingDestination = startingDestination,
        startingNavControllerInstance = startingNavControllerInstance,
        routedContent = routedContent,
    ) {
        animatedDestination(animations(it)) { router(it) }
    }
}

@Composable
inline fun <reified C : Any> NavHost(
    startingDestination: C,
    navControllerStore: NavControllerStore = LocalNavControllerStore.current,
    parentsComponentContext: ComponentContext = LocalComponentContext.current,
    startingNavControllerInstance: NavController<C> = remember {
        navControllerStore.getOrCreate {
            NavController(startingDestination, serializer(), parentsComponentContext)
        }
    },
    crossinline animations: NavigationItem.(child: C) -> ContentAnimator,
    crossinline router: @Composable (child: C) -> Unit
) {
    NavHost(
        startingDestination = startingDestination,
        startingNavControllerInstance = startingNavControllerInstance,
        routedContent = { it(Modifier) },
    ) {
        animatedDestination(animations(it)) { router(it) }
    }
}