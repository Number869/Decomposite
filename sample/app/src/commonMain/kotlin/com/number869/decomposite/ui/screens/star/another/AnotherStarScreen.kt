package com.number869.decomposite.ui.screens.star.another

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.number869.decomposite.core.common.navigation.navController
import com.number869.decomposite.ui.screens.star.StarDestinations

@Composable
fun AnotherStarScreen() {
    val navController = navController<StarDestinations>()

    Surface {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Text(
                "I lied, there is no star here muehehehehehehe >:3",
                style = MaterialTheme.typography.displaySmall,
            )

            Button(
                onClick = { navController.navigateBack() },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Go back")
            }
        }
    }

//    val openSnack = @Composable {
//        navController.openInSnack("noStarScreenSnack", displayDurationMillis = 2L.seconds) {
//            Box(
//                Modifier
//                    .align(Alignment.Center)
//                    .clip(CircleShape)
//                    .background(MaterialTheme.colorScheme.surfaceVariant)
//            ) {
//                Text(
//                    "Btw this is a snack and this screen is open as an overlay",
//                    style = MaterialTheme.typography.bodyLarge,
//                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
//                )
//            }
//        }
//    }
//    LaunchedEffect(Unit) {
//        openSnack.invoke()
//    }
}