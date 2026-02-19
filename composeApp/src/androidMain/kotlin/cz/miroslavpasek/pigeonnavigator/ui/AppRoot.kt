package cz.miroslavpasek.pigeonnavigator.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cz.miroslavpasek.pigeonnavigator.ui.screens.NavigateScreen
import cz.miroslavpasek.pigeonnavigator.ui.viewmodel.HomeViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppRoot(vm: HomeViewModel = koinViewModel()) {
    val state by vm.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    MaterialTheme {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Filled.LocationOn, contentDescription = "Navigate") },
                        label = { Text("Navigate") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Filled.Place, contentDescription = "Nearby") },
                        label = { Text("Nearby") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") }
                    )
                }
            }
        ) { innerPadding ->
            when (selectedTab) {
                0 -> NavigateScreen(
                    location = state.location,
                    followUser = false,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
                1 -> Box(
                    Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) { Text("Nearby - coming soon") }
                2 -> Box(
                    Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) { Text("Settings - coming soon") }
            }
        }
    }
}