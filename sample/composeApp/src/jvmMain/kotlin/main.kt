
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.nxoim.decomposite.App
import com.nxoim.decomposite.core.common.navigation.BackGestureProviderContainer
import com.nxoim.decomposite.core.jvm.navigation.NavigationRootProvider
import com.nxoim.decomposite.core.jvm.navigation.defaultNavigationRootData
import com.nxoim.decomposite.ui.theme.SampleTheme
import java.awt.Dimension

@OptIn(ExperimentalDecomposeApi::class)
fun main() = application {
    // initialize this at the root of your app
    val windowState = rememberWindowState()
    val navigationRootData = defaultNavigationRootData()

    Window(
        title = "Decomposite",
        state = windowState,
        onCloseRequest = ::exitApplication,
    ) {
        window.minimumSize = Dimension(350, 600)

        SampleTheme {
            // first wrap your app in a theme.
            // because of material 3 quirks - surface wraps the root to fix text
            // colors in overlays.
            Surface {
                // also since you need to initialize the component context of the app
                // on your preferred platform anyway - it's ok to add decomposite to
                // your entry-point/app module of the project, or combine it with your
                // navigation module

                // then initialize the back gesture overlay that will handle the back gestures.
                // initialize it first, put NavigationRoot inside it, else overlays will not
                // detect the gestures
                BackGestureProviderContainer(
                    navigationRootData.defaultComponentContext,
                    content = {
                        NavigationRootProvider(navigationRootData, windowState) {
                            App()
                        }
                    }
                )
            }
        }
    }
}