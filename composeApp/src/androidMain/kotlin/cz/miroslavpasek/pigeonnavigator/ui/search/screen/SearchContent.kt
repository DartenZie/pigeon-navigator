package cz.miroslavpasek.pigeonnavigator.ui.search.screen

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.miroslavpasek.pigeonnavigator.ui.common.theme.AppSpacing

/**
 * Stateless layout for the standalone search screen.
 *
 * Receives the rendered text only and forwards user inputs through the supplied
 * callbacks; persistence/Store wiring lives in [SearchScreen].
 */
@Composable
fun SearchContent(
    query: String,
    isLoading: Boolean,
    errorMessage: String?,
    results: List<String>,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = query,
                onValueChange = onQueryChange,
                label = { Text("Search") },
                singleLine = true,
            )
            Button(onClick = onClear) {
                Text("Clear")
            }
        }

        if (isLoading) {
            CircularProgressIndicator()
        }

        errorMessage?.let { Text(it) }

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(results) { item ->
                Text(
                    text = item,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AppSpacing.sm),
                )
            }
        }
    }
}
