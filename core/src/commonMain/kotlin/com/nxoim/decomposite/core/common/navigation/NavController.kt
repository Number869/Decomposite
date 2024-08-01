package com.nxoim.decomposite.core.common.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.backStack
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.popTo
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.router.stack.replaceCurrent
import com.nxoim.decomposite.core.common.ultils.LocalComponentContext
import com.nxoim.decomposite.core.common.ultils.OnDestinationDisposeEffect
import com.nxoim.decomposite.core.common.ultils.rememberRetained
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import kotlin.jvm.JvmInline

/**
 * Gets an existing navigation controller instance
 */
@ReadOnlyComposable
@Composable
inline fun <reified C : Any> getExistingNavController(
	key: String = navControllerKey<C>(),
	navStore: NavControllerStore = LocalNavControllerStore.current
) = navStore.get(key, C::class)

/**
 * Creates a navigation controller instance in the [NavControllerStore], which allows
 * for sharing the same instance between multiple calls of [navController] or [getExistingNavController].
 *
 * Is basically a decompose component that replicates the functionality of a generic
 * navigation controller. The instance is not retained, therefore on configuration changes
 * components will die and get recreated. By default inherits parent's [ComponentContext].
 *
 * [childFactory] allows for creating custom children instances that implement [DecomposeChildInstance].
 *
 * [key] is used for identifying [childStack]'s during serialization and instances in
 * [NavControllerStore], which means keys MUST be unique.
 *
 * On death removes itself from the [NavControllerStore] right after the composition's death.
 */
@Composable
inline fun <reified C : Any> navController(
	startingDestination: C,
	serializer: KSerializer<C>? = serializer(),
	navStore: NavControllerStore = LocalNavControllerStore.current,
	componentContext: ComponentContext = LocalComponentContext.current,
	key: String = navControllerKey<C>(),
	noinline childFactory: (
		config: C,
		childComponentContext: ComponentContext
	) -> DecomposeChildInstance = { _, childComponentContext ->
		DefaultChildInstance(childComponentContext)
	}
): NavController<C> {
	OnDestinationDisposeEffect(
		"${C::class} key $key navController OnDestinationDisposeEffect",
		componentContext = componentContext,
		waitForCompositionRemoval = true
	) {
		navStore.remove(key, C::class)
	}

	return navStore.getOrCreate(key, C::class) {
		NavController(
			startingDestination,
			serializer,
			componentContext,
			key,
			childFactory
		)
	}
}

inline fun <reified C : Any> navControllerKey(additionalKey: String = "") =
	"${C::class}$additionalKey"

/**
 * Generic navigation controller. Contains a stack for overlays and a stack for screens.
 */
@Immutable
class NavController<C : Any>(
	private val startingDestination: C,
	serializer: KSerializer<C>? = null,
	componentContext: ComponentContext,
	val key: String = startingDestination::class.toString(),
	childFactory: (
		config: C,
		childComponentContext: ComponentContext
	) -> DecomposeChildInstance = { _, childComponentContext ->
		DefaultChildInstance(childComponentContext)
	}
) : ComponentContext by componentContext {
	val controller = StackNavigation<C>()

	val screenStack = childStack(
		source = controller,
		serializer = serializer,
		initialConfiguration = startingDestination,
		key = "screenStack $key",
		handleBackButton = true,
		childFactory = childFactory
	)

	val currentScreen by screenStack.let {
		val state = mutableStateOf(it.value.active.configuration)

		it.subscribe { newState -> state.value = newState.active.configuration }

		return@let state
	}

	/**
	 * Navigates to a destination. If a destination exists already - moves it to the top instead
	 * of adding a new entry. If the [removeIfIsPreceding] is enabled (is by default) and
	 * the requested [destination] precedes the current one in the stack -
	 * navigate back instead.
	 */
	fun navigate(
		destination: C,
		// removes the current entry if requested navigation to the preceding one
		removeIfIsPreceding: Boolean = true,
		onComplete: () -> Unit = { }
	) {
		controller.navigate(
			transformer = { stack ->
				if (removeIfIsPreceding && stack.size > 1 && stack[stack.lastIndex - 1] == destination)
					stack.dropLast(1)
				else
					stack.filterNot { it == destination } + destination
			},
			onComplete = { _, _ -> onComplete() }
		)
	}

	/**
	 * Navigates back in this(!) nav controller.
	 */
	fun navigateBack(onComplete: (Boolean) -> Unit = { }) {
		controller.pop(onComplete)
	}

	/**
	 * Removes destinations that, in the stack, are after the provided one.
	 */
	fun navigateBackTo(
		destination: C,
		onComplete: (Boolean) -> Unit = { }
	) {
		val indexOfDestination = screenStack.backStack
			.indexOfFirst { it.configuration == destination }

		controller.popTo(indexOfDestination, onComplete)
	}

	/**
	 * Removes a destination.
	 */
	fun close(destination: C, onComplete: () -> Unit = { }) = controller
		.navigate(
			transformer = { stack -> stack.filterNot { it == destination } },
			onComplete = { _, _ -> onComplete() }
		)

	/**
	 * Replaces the current destination with the provided one.
	 */
	fun replaceCurrent(
		withDestination: C,
		onComplete: () -> Unit = { }
	) = controller
		.replaceCurrent(withDestination) { onComplete() }

	/**
	 * Replaces all destinations with the provided one.
	 */
	fun replaceAll(
		vararg destination: C,
		onComplete: () -> Unit = { }
	) = controller
		.replaceAll(*destination) { onComplete() }
}

@JvmInline
@Immutable
value class DefaultChildInstance(
	override val componentContext: ComponentContext
) : DecomposeChildInstance

/**
 * Base for child instances. Contains [componentContext] for features like [rememberRetained],
 * [OnDestinationDisposeEffect], [LocalComponentContext].
 */
interface DecomposeChildInstance {
	val componentContext: ComponentContext
}
