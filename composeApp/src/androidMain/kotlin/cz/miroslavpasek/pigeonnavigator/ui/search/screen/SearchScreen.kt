package cz.miroslavpasek.pigeonnavigator.ui.search.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import cz.miroslavpasek.pigeonnavigator.feature.search.api.SearchStore
import cz.miroslavpasek.pigeonnavigator.feature.search.presentation.SearchIntent
import org.koin.core.context.GlobalContext

/**
 * Hosts the [SearchStore] for the standalone search screen.
 *
 * Reads state from the store, forwards user intents, and disposes the store
 * when the composable leaves composition. Layout is delegated to
 * [SearchContent].
 */
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
) {
    val store = remember { GlobalContext.get().get<SearchStore>() }
    val state by store.state.collectAsState()

    DisposableEffect(store) {
        onDispose { store.close() }
    }

    SearchContent(
        query = state.query,
        isLoading = state.isLoading,
        errorMessage = state.errorMessage,
        results = state.results,
        onQueryChange = { query ->
            store.send(SearchIntent.QueryChanged(query))
            if (query.isBlank()) {
                store.send(SearchIntent.ClearSearch)
            } else {
                store.send(SearchIntent.SubmitSearch)
            }
        },
        onClear = { store.send(SearchIntent.ClearSearch) },
        modifier = modifier,
    )
}
