package cz.miroslavpasek.pigeonnavigator.ui.searchdock.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cz.miroslavpasek.pigeonnavigator.domain.search.SearchResult
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockPoiItem
import cz.miroslavpasek.pigeonnavigator.ui.searchdock.internal.MapTapRecord
import cz.miroslavpasek.pigeonnavigator.ui.searchdock.internal.formatCoordinateLabel
import cz.miroslavpasek.pigeonnavigator.ui.searchdock.internal.formatDistance
import cz.miroslavpasek.pigeonnavigator.ui.searchdock.internal.mapTapRecordRows
import cz.miroslavpasek.pigeonnavigator.ui.searchdock.internal.mapTapRecordTitle

/**
 * Reusable detail-panel scaffold and the three concrete detail panels (POI,
 * search result, map tap) shown inside the expanded search dock.
 *
 * Behavioural responsibilities:
 *  - render a back-arrow header,
 *  - lay out a list of label/value rows,
 *  - present "Locate on map" / "Add to route" actions.
 *
 * The presenter chooses which rows to display; this layer is pure UI.
 */
@Composable
internal fun NearbyPoiDetailPanel(
    item: SearchDockPoiItem,
    onBack: () -> Unit,
    onLocateClicked: () -> Unit,
    onAddToRouteClicked: () -> Unit,
    showAddToRouteAction: Boolean = true,
    modifier: Modifier = Modifier,
) {
    DetailPanelScaffold(
        title = item.title,
        onBack = onBack,
        rows = buildList {
            add("Kind" to item.kindLabel)
            add("Name" to item.subtitle)
            item.frequency?.takeIf { it.isNotBlank() }?.let { add("Frequency" to it) }
            add("Distance" to formatDistance(item.distanceMeters))
            add("Position" to formatCoordinateLabel(item.latitude, item.longitude))
        },
        onLocateClicked = onLocateClicked,
        onAddToRouteClicked = onAddToRouteClicked,
        showAddToRouteAction = showAddToRouteAction,
        modifier = modifier,
    )
}

@Composable
internal fun SearchResultDetailPanel(
    result: SearchResult,
    onBack: () -> Unit,
    onLocateClicked: () -> Unit,
    onAddToRouteClicked: () -> Unit,
    showAddToRouteAction: Boolean = true,
    modifier: Modifier = Modifier,
) {
    DetailPanelScaffold(
        title = result.title,
        onBack = onBack,
        rows = searchResultRows(result),
        onLocateClicked = onLocateClicked,
        onAddToRouteClicked = onAddToRouteClicked,
        showAddToRouteAction = showAddToRouteAction,
        modifier = modifier,
    )
}

@Composable
internal fun MapTapDetailPanel(
    record: MapTapRecord,
    onBack: () -> Unit,
    onLocateClicked: () -> Unit,
    onAddToRouteClicked: () -> Unit,
    showAddToRouteAction: Boolean = true,
    modifier: Modifier = Modifier,
) {
    DetailPanelScaffold(
        title = mapTapRecordTitle(record),
        onBack = onBack,
        rows = mapTapRecordRows(record),
        onLocateClicked = onLocateClicked,
        onAddToRouteClicked = onAddToRouteClicked,
        showAddToRouteAction = showAddToRouteAction,
        modifier = modifier,
    )
}

private fun searchResultRows(result: SearchResult): List<Pair<String, String>> = buildList {
    add("Kind" to result.kindLabel)
    add("Name" to result.subtitle)
    when (result) {
        is SearchResult.Airport -> add("Position" to formatCoordinateLabel(result.latitude, result.longitude))
        is SearchResult.Navaid -> {
            result.frequency?.takeIf { it.isNotBlank() }?.let { add("Frequency" to it) }
            add("Position" to formatCoordinateLabel(result.latitude, result.longitude))
        }
        is SearchResult.Airspace -> {
            add("Position" to formatCoordinateLabel(result.centerLatitude, result.centerLongitude))
            add("Bounds NE" to formatCoordinateLabel(result.bounds.maxLatitude, result.bounds.maxLongitude))
            add("Bounds SW" to formatCoordinateLabel(result.bounds.minLatitude, result.bounds.minLongitude))
        }
    }
}

@Composable
private fun DetailPanelScaffold(
    title: String,
    onBack: () -> Unit,
    rows: List<Pair<String, String>>,
    onLocateClicked: () -> Unit,
    onAddToRouteClicked: () -> Unit,
    showAddToRouteAction: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth()) {
        DetailHeader(title = title, onBack = onBack)
        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            rows.forEach { (label, value) ->
                DetailRow(label = label, value = value)
            }
        }
        DetailActions(
            onLocateClicked = onLocateClicked,
            onAddToRouteClicked = onAddToRouteClicked,
            showAddToRouteAction = showAddToRouteAction,
        )
    }
}

@Composable
internal fun DetailHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to results",
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun DetailRow(label: String, value: String) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.widthIn(min = 110.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DetailActions(
    onLocateClicked: () -> Unit,
    onAddToRouteClicked: () -> Unit,
    showAddToRouteAction: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DetailActionButton(
            text = "Locate on Map",
            onClick = onLocateClicked,
            icon = {
                Icon(
                    imageVector = Icons.Filled.PinDrop,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
        )
        if (showAddToRouteAction) {
            DetailActionButton(
                text = "Add to Route",
                onClick = onAddToRouteClicked,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun DetailActionButton(
    text: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        icon()
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text)
    }
}
