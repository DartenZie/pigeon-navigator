package cz.miroslavpasek.pigeonnavigator.ui.home.internal

import cz.miroslavpasek.pigeonnavigator.domain.aviation.Airspace
import cz.miroslavpasek.pigeonnavigator.domain.aviation.GeoPoint
import cz.miroslavpasek.pigeonnavigator.domain.search.GeoBounds
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockMapFocus

/**
 * Maps an [Airspace] polygon to a [SearchDockMapFocus.Bounds] so the map can
 * frame it. Falls back to a degenerate `(0,0)` bounds if the airspace has no
 * geometry — preserves the existing behaviour from the original AppRoot.
 */
internal fun Airspace.toMapFocus(): SearchDockMapFocus {
    val bounds = points.toBounds()
        ?: GeoBounds(
            minLatitude = 0.0,
            minLongitude = 0.0,
            maxLatitude = 0.0,
            maxLongitude = 0.0,
        )
    return SearchDockMapFocus.Bounds(
        centerLatitude = bounds.center.latitude,
        centerLongitude = bounds.center.longitude,
        bounds = bounds,
    )
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
