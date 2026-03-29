package cz.miroslavpasek.pigeonnavigator.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cz.miroslavpasek.pigeonnavigator.ui.screens.NavigateScreen
import cz.miroslavpasek.pigeonnavigator.ui.viewmodel.HomeViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppRoot(vm: HomeViewModel = koinViewModel()) {
    val state by vm.uiState.collectAsState()



    var selectedTab by remember { mutableIntStateOf(0) }

    MaterialTheme {
        NavigateScreen(
            location = state.location,
            followUser = false,
            modifier = Modifier.fillMaxSize()
        )
    }
}
