package cz.miroslavpasek.pigeonnavigator.ui.searchdock.internal

import cz.miroslavpasek.pigeonnavigator.bridge.MapTapLookupState
import cz.miroslavpasek.pigeonnavigator.domain.aviation.Airspace
import cz.miroslavpasek.pigeonnavigator.domain.aviation.NearbyAirport
import cz.miroslavpasek.pigeonnavigator.domain.aviation.NearbyNavaid
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockRoute
import kotlin.math.roundToInt

/**
 * One concrete map-tap detail record.
 *
 * Wraps the heterogenous nearby-airport / navaid / airspace types so the
 * detail panel can render any of them uniformly.
 */
internal sealed interface MapTapRecord {
    data class AirportRecord(val value: NearbyAirport) : MapTapRecord
    data class NavaidRecord(val value: NearbyNavaid) : MapTapRecord
    data class AirspaceRecord(val value: Airspace) : MapTapRecord
}

internal fun findMapTapRecord(
    key: String,
    lookup: MapTapLookupState,
): MapTapRecord? {
    return when {
        key.startsWith("airport:") -> {
            val id = key.removePrefix("airport:")
            lookup.airports.firstOrNull { it.airport.id == id }?.let(MapTapRecord::AirportRecord)
        }

        key.startsWith("navaid:") -> {
            val id = key.removePrefix("navaid:")
            lookup.navaids.firstOrNull { it.navaid.id == id }?.let(MapTapRecord::NavaidRecord)
        }

        key.startsWith("airspace:") -> {
            val id = key.removePrefix("airspace:")
            lookup.airspaces.firstOrNull { it.id == id }?.let(MapTapRecord::AirspaceRecord)
        }

        else -> null
    }
}

internal fun mapTapRecordTitle(record: MapTapRecord): String = when (record) {
    is MapTapRecord.AirportRecord -> record.value.airport.id
    is MapTapRecord.NavaidRecord -> record.value.navaid.id
    is MapTapRecord.AirspaceRecord -> record.value.name.ifBlank { record.value.id }
}

internal fun mapTapRecordRows(record: MapTapRecord): List<Pair<String, String>> = when (record) {
    is MapTapRecord.AirportRecord -> {
        val a = record.value.airport
        buildList {
            add("Name" to a.name)
            add("Type" to a.kind)
            a.elevationMeters?.let { add("Elevation" to "${it} m") }
            add("Position" to formatCoordinateLabel(a.latitude, a.longitude))
            add("Distance" to formatDistance(record.value.distanceMeters))
        }
    }

    is MapTapRecord.NavaidRecord -> {
        val n = record.value.navaid
        buildList {
            add("Name" to n.name)
            add("Type" to n.kind)
            if (n.detail.isNotBlank()) add("Detail" to n.detail)
            n.frequency?.takeIf { it.isNotBlank() }?.let { add("Frequency" to it) }
            add("Position" to formatCoordinateLabel(n.latitude, n.longitude))
            add("Distance" to formatDistance(record.value.distanceMeters))
        }
    }

    is MapTapRecord.AirspaceRecord -> {
        val s = record.value
        buildList {
            if (s.name.isNotBlank()) add("Name" to s.name)
            add("Type" to s.kind)
            add("Lower" to (s.lowerLimitMeters?.let { "${it} m ${s.lowerLimitReference.orEmpty()}".trim() } ?: "SFC"))
            add("Upper" to (s.upperLimitMeters?.let { "${it} m ${s.upperLimitReference.orEmpty()}".trim() } ?: "UNL"))
            add("Vertices" to s.points.size.toString())
        }
    }
}

internal fun formatAltitudeBand(airspace: Airspace): String {
    val lower = airspace.lowerLimitMeters?.let { "${it}m ${airspace.lowerLimitReference ?: ""}".trim() } ?: "SFC"
    val upper = airspace.upperLimitMeters?.let { "${it}m ${airspace.upperLimitReference ?: ""}".trim() } ?: "UNL"
    return "$lower - $upper"
}

internal fun formatCoordinateLabel(latitude: Double, longitude: Double): String {
    return "%.4f, %.4f".format(latitude, longitude)
}

internal fun formatDistance(distanceMeters: Double): String {
    return if (distanceMeters >= 1000.0) {
        "${(distanceMeters / 1000.0 * 10.0).roundToInt() / 10.0} km"
    } else {
        "${distanceMeters.roundToInt()} m"
    }
}

@Suppress("unused")
internal fun SearchDockRoute.label(): String = when (this) {
    SearchDockRoute.Nearby -> "Nearby"
    SearchDockRoute.Search -> "Search"
    SearchDockRoute.RoutePlanner -> "Plan"
    SearchDockRoute.MapTap -> "Map Tap"
}
