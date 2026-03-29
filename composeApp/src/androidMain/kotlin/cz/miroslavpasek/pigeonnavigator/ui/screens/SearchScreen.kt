package cz.miroslavpasek.pigeonnavigator.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.miroslavpasek.pigeonnavigator.feature.search.api.SearchStore
import cz.miroslavpasek.pigeonnavigator.feature.search.presentation.SearchIntent
import org.koin.core.context.GlobalContext

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier
) {
    val store = remember { GlobalContext.get().get<SearchStore>() }
    val state by store.state.collectAsState()

    DisposableEffect(store) {
        onDispose { store.close() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = state.query,
                onValueChange = {
                    store.send(SearchIntent.QueryChanged(it))
                    if (it.isBlank()) {
                        store.send(SearchIntent.ClearSearch)
                    } else {
                        store.send(SearchIntent.SubmitSearch)
                    }
                },
                label = { Text("Search") },
                singleLine = true
            )
            Button(onClick = { store.send(SearchIntent.ClearSearch) }) {
                Text("Clear")
            }
        }

        if (state.isLoading) {
            CircularProgressIndicator()
        }

        state.errorMessage?.let {
            Text(it)
        }

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(state.results) { item ->
                Text(
                    text = item,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
        }
    }
}
