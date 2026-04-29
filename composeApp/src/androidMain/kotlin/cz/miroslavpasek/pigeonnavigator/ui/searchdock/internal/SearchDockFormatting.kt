package cz.miroslavpasek.pigeonnavigator.ui.searchdock.internal

import cz.miroslavpasek.pigeonnavigator.bridge.MapTapLookupState
import cz.miroslavpasek.pigeonnavigator.domain.aviation.Airspace
import cz.miroslavpasek.pigeonnavigator.domain.aviation.GeoPoint
import cz.miroslavpasek.pigeonnavigator.domain.aviation.NearbyAirport
import cz.miroslavpasek.pigeonnavigator.domain.aviation.NearbyNavaid
import cz.miroslavpasek.pigeonnavigator.domain.search.GeoBounds
import cz.miroslavpasek.pigeonnavigator.domain.search.SearchResult
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockPoiItem
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockRoute
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockRoutePoint
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

internal fun routeLabelForIndex(index: Int): String {
    return ('A'.code + index).toChar().toString()
}

internal fun SearchDockPoiItem.toRoutePoint(): SearchDockRoutePoint = SearchDockRoutePoint(
    id = id,
    title = title,
    latitude = latitude,
    longitude = longitude,
)

internal fun SearchResult.toRoutePoint(): SearchDockRoutePoint = when (this) {
    is SearchResult.Airport -> SearchDockRoutePoint(id, title, latitude, longitude)
    is SearchResult.Navaid -> SearchDockRoutePoint(id, title, latitude, longitude)
    is SearchResult.Airspace -> SearchDockRoutePoint(id, title, centerLatitude, centerLongitude)
}

internal fun MapTapRecord.toRoutePoint(): SearchDockRoutePoint = when (this) {
    is MapTapRecord.AirportRecord -> SearchDockRoutePoint(
        id = "airport:${value.airport.id}",
        title = value.airport.id,
        latitude = value.airport.latitude,
        longitude = value.airport.longitude,
    )

    is MapTapRecord.NavaidRecord -> SearchDockRoutePoint(
        id = "navaid:${value.navaid.id}",
        title = value.navaid.id,
        latitude = value.navaid.latitude,
        longitude = value.navaid.longitude,
    )

    is MapTapRecord.AirspaceRecord -> {
        val bounds = value.points.toBounds()
        SearchDockRoutePoint(
            id = "airspace:${value.id}",
            title = value.name.ifBlank { value.id },
            latitude = bounds?.let { (it.minLatitude + it.maxLatitude) / 2.0 } ?: 0.0,
            longitude = bounds?.let { (it.minLongitude + it.maxLongitude) / 2.0 } ?: 0.0,
        )
    }
}

private fun List<GeoPoint>.toBounds(): GeoBounds? {
    if (isEmpty()) return null
    var minLatitude = first().latitude
    var maxLatitude = first().latitude
    var minLongitude = first().longitude
    var maxLongitude = first().longitude
    for (point in drop(1)) {
        minLatitude = minOf(minLatitude, point.latitude)
        maxLatitude = maxOf(maxLatitude, point.latitude)
        minLongitude = minOf(minLongitude, point.longitude)
        maxLongitude = maxOf(maxLongitude, point.longitude)
    }
    return GeoBounds(
        minLatitude = minLatitude,
        minLongitude = minLongitude,
        maxLatitude = maxLatitude,
        maxLongitude = maxLongitude,
    )
}
