package cz.miroslavpasek.pigeonnavigator.domain.search

import cz.miroslavpasek.pigeonnavigator.domain.aviation.GeoPoint

sealed interface SearchResult {
    val id: String
    val title: String
    val subtitle: String
    val kindLabel: String

    data class Airport(
        override val id: String,
        override val title: String,
        override val subtitle: String,
        val latitude: Double,
        val longitude: Double
    ) : SearchResult {
        override val kindLabel: String = "Airport"
    }

    data class Navaid(
        override val id: String,
        override val title: String,
        override val subtitle: String,
        val frequency: String?,
        val latitude: Double,
        val longitude: Double
    ) : SearchResult {
        override val kindLabel: String = "Navaid"
    }

    data class Airspace(
        override val id: String,
        override val title: String,
        override val subtitle: String,
        val centerLatitude: Double,
        val centerLongitude: Double,
        val bounds: GeoBounds
    ) : SearchResult {
        override val kindLabel: String = "Airspace"
    }
}

data class GeoBounds(
    val minLatitude: Double,
    val minLongitude: Double,
    val maxLatitude: Double,
    val maxLongitude: Double
) {
    val center: GeoPoint = GeoPoint(
        latitude = (minLatitude + maxLatitude) / 2.0,
        longitude = (minLongitude + maxLongitude) / 2.0
    )
}
