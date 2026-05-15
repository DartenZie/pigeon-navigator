package cz.miroslavpasek.pigeonnavigator.ui.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import cz.miroslavpasek.pigeonnavigator.domain.settings.AppSettingsRepository
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

/**
 * Full-screen settings editor backed by [AppSettingsRepository].
 *
 * Reads the current `AppSettings` via the repository's `StateFlow` and forwards
 * user edits back through the repository's `update*` methods. The screen owns
 * no business logic of its own – it is a thin presentation layer over the
 * existing domain contract that delegates layout to [SettingsContent].
 */
@Composable
fun SettingsScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val repository = remember { GlobalContext.get().get<AppSettingsRepository>() }
    val settings by repository.settings.collectAsState()
    val scope = rememberCoroutineScope()

    SettingsContent(
        settings = settings,
        onClose = onClose,
        onUnitsChange = { newUnits ->
            scope.launch { repository.updateUnits(newUnits) }
        },
        onWarningChange = { newWarning ->
            scope.launch { repository.updateWarningPreferences(newWarning) }
        },
        onLocationChange = { newLocation ->
            scope.launch { repository.updateLocationPreferences(newLocation) }
        },
        modifier = modifier,
    )
}
