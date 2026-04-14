package com.pv239.beelocal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.pv239.beelocal.ui.theme.BeelocalTheme
import com.pv239.beelocal.navigation.AppNavGraph
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.ui.unit.dp
import com.pv239.beelocal.ui.components.Header
import com.pv239.beelocal.ui.components.NavigationBar
import com.pv239.beelocal.ui.screens.HomeScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BeelocalTheme {
                AppNavGraph()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun BeelocalApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            // TODO: replace hardcoded values
            Header(
                streakCount = 7,
                honeyCount = 67,
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            when (currentDestination) {
                AppDestinations.HOME -> HomeScreen(innerPadding)
                AppDestinations.ROUTES -> Greeting("Routes Screen")
                AppDestinations.BINGO -> Greeting("Bingo Screen")
                AppDestinations.SOCIAL -> Greeting("Social Screen")
                AppDestinations.PROFILE -> Greeting("Profile Screen")
            }

            NavigationBar(
                currentDestination = currentDestination,
                onDestinationSelected = { currentDestination = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 16.dp)
            )
        }
    }
}

enum class AppDestinations(
    val label: String,
    val iconSelected: Int,
    val iconUnselected: Int
) {
    HOME("Home", R.drawable.baseline_home_24, R.drawable.outline_home_24),
    ROUTES("Routes", R.drawable.baseline_map_24, R.drawable.outline_map_24),
    BINGO("Bingo", R.drawable.baseline_grid_on_24, R.drawable.outline_grid_on_24),
    SOCIAL("Social", R.drawable.baseline_group_24, R.drawable.outline_group_24),
    PROFILE("Profile", R.drawable.baseline_person_24, R.drawable.outline_person_24),
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BeelocalTheme {
        Greeting("Android")
    }
}