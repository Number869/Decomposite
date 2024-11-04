package com.nxoim.decomposite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.nxoim.decomposite.core.android.navigation.NavigationRootProvider
import com.nxoim.decomposite.core.android.navigation.defaultNavigationRootData
import com.nxoim.decomposite.core.common.navigation.BackGestureProviderContainer
import com.nxoim.decomposite.ui.theme.SampleTheme

class AppActivity : ComponentActivity() {
    @OptIn(ExperimentalDecomposeApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val navigationRootData = defaultNavigationRootData()

        setContent {
            enableEdgeToEdge()

            SampleTheme {
                Surface {
                    BackGestureProviderContainer(
                        navigationRootData.defaultComponentContext,
                    ) {
                        NavigationRootProvider(navigationRootData) { App() }
                    }
                }
            }
        }
    }
}