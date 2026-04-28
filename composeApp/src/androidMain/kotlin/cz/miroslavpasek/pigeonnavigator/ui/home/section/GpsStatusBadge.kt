package cz.miroslavpasek.pigeonnavigator.ui.home.section

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cz.miroslavpasek.pigeonnavigator.data.LocationStatus

/**
 * Pill-shaped badge that surfaces non-active GPS states (no permission, signal
 * lost). Hides itself when the GPS is active or status is unknown.
 */
@Composable
fun GpsStatusBadge(
    status: LocationStatus?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (status == null || status == LocationStatus.Active) return

    val colors = MaterialTheme.colorScheme
    val isPermissionRequired = status == LocationStatus.PermissionRequired
    val label = if (isPermissionRequired) "No location access" else "GPS signal lost"
    val icon: ImageVector = if (isPermissionRequired) Icons.Filled.LocationOff else Icons.Filled.MyLocation
    val containerColor = if (isPermissionRequired) colors.errorContainer else colors.tertiaryContainer
    val contentColor = if (isPermissionRequired) colors.onErrorContainer else colors.onTertiaryContainer

    Surface(
        modifier = modifier
            .then(if (isPermissionRequired) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(percent = 50),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 8.dp,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(PaddingValues(horizontal = 14.dp, vertical = 10.dp)),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
