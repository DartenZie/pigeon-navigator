package cz.miroslavpasek.pigeonnavigator.ui.map.internal

import cz.miroslavpasek.pigeonnavigator.data.FlightLocation
import cz.miroslavpasek.pigeonnavigator.domain.terrain.TerrainHazardLevel
import cz.miroslavpasek.pigeonnavigator.domain.terrain.TerrainHazardSample
import cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation.SearchDockRoutePoint
import org.maplibre.android.style.expressions.Expression.eq
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.literal
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.fillOpacity
import org.maplibre.android.style.layers.PropertyFactory.fillOutlineColor
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textHaloColor
import org.maplibre.android.style.layers.PropertyFactory.textHaloWidth
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.maps.Style
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon

internal const val TERRAIN_SOURCE_ID = "terrain-hazard-source"
private const val TERRAIN_YELLOW_LAYER_ID = "terrain-hazard-near-layer"
private const val TERRAIN_RED_LAYER_ID = "terrain-hazard-conflict-layer"
private const val TERRAIN_SEVERITY_KEY = "severity"
private const val TERRAIN_SEVERITY_NEAR = "near"
private const val TERRAIN_SEVERITY_CONFLICT = "conflict"

private const val USER_LOCATION_DOT_SOURCE_ID = "user-location-dot-source"
private const val USER_LOCATION_DOT_LAYER_ID = "user-location-dot-layer"
private const val USER_LOCATION_ACCURACY_SOURCE_ID = "user-location-accuracy-source"
private const val USER_LOCATION_ACCURACY_LAYER_ID = "user-location-accuracy-layer"
private const val USER_LOCATION_DOT_COLOR = "#1E88E5"
private const val USER_LOCATION_DOT_STROKE_COLOR = "#FFFFFF"
private const val USER_LOCATION_ACCURACY_FILL_COLOR = "#42A5F5"

private const val USER_GUIDANCE_LINE_SOURCE_ID = "user-guidance-line-source"
private const val USER_GUIDANCE_LINE_TRACK_LAYER_ID = "user-guidance-line-track-layer"
private const val USER_GUIDANCE_LINE_CONE_LAYER_ID = "user-guidance-line-cone-layer"
private const val USER_GUIDANCE_MINUTE_MARK_SOURCE_ID = "user-guidance-minute-mark-source"
private const val USER_GUIDANCE_MINUTE_MARK_LAYER_ID = "user-guidance-minute-mark-layer"
private const val USER_GUIDANCE_KIND_KEY = "kind"
private const val USER_GUIDANCE_KIND_TRACK = "track"
private const val USER_GUIDANCE_KIND_CONE = "cone"

private const val ROUTE_LINE_SOURCE_ID = "route-line-source"
private const val ROUTE_LINE_LAYER_ID = "route-line-layer"
private const val ROUTE_POINT_SOURCE_ID = "route-point-source"
private const val ROUTE_POINT_CIRCLE_LAYER_ID = "route-point-circle-layer"
private const val ROUTE_POINT_LABEL_LAYER_ID = "route-point-label-layer"
private const val ROUTE_POINT_LABEL_KEY = "label"
private const val ROUTE_LINE_COLOR = "#00A6FF"
private const val ROUTE_POINT_COLOR = "#00A6FF"

internal fun ensureUserLocationLayers(style: Style) {
    if (style.getSourceAs<GeoJsonSource>(USER_LOCATION_ACCURACY_SOURCE_ID) == null) {
        style.addSource(
            GeoJsonSource(USER_LOCATION_ACCURACY_SOURCE_ID, FeatureCollection.fromFeatures(arrayOf())),
        )
    }

    if (style.getLayer(USER_LOCATION_ACCURACY_LAYER_ID) == null) {
        style.addLayer(
            FillLayer(USER_LOCATION_ACCURACY_LAYER_ID, USER_LOCATION_ACCURACY_SOURCE_ID)
                .withProperties(
                    fillColor(USER_LOCATION_ACCURACY_FILL_COLOR),
                    fillOpacity(0.2f),
                    fillOutlineColor(USER_LOCATION_ACCURACY_FILL_COLOR),
                ),
        )
    }

    if (style.getSourceAs<GeoJsonSource>(USER_LOCATION_DOT_SOURCE_ID) == null) {
        style.addSource(
            GeoJsonSource(USER_LOCATION_DOT_SOURCE_ID, FeatureCollection.fromFeatures(arrayOf())),
        )
    }

    if (style.getLayer(USER_LOCATION_DOT_LAYER_ID) == null) {
        style.addLayer(
            CircleLayer(USER_LOCATION_DOT_LAYER_ID, USER_LOCATION_DOT_SOURCE_ID)
                .withProperties(
                    circleColor(USER_LOCATION_DOT_COLOR),
                    circleRadius(7f),
                    circleOpacity(1f),
                    circleStrokeWidth(2f),
                    circleStrokeColor(USER_LOCATION_DOT_STROKE_COLOR),
                ),
        )
    }
}

internal fun updateUserLocationLayers(style: Style, location: FlightLocation?) {
    style.getSourceAs<GeoJsonSource>(USER_LOCATION_DOT_SOURCE_ID)
        ?.setGeoJson(buildUserLocationDotFeatureCollection(location))
    style.getSourceAs<GeoJsonSource>(USER_LOCATION_ACCURACY_SOURCE_ID)
        ?.setGeoJson(buildUserLocationAccuracyFeatureCollection(location))
}

internal fun ensureUserGuidanceLayers(style: Style) {
    if (style.getSourceAs<GeoJsonSource>(USER_GUIDANCE_LINE_SOURCE_ID) == null) {
        style.addSource(GeoJsonSource(USER_GUIDANCE_LINE_SOURCE_ID, FeatureCollection.fromFeatures(arrayOf())))
    }

    if (style.getLayer(USER_GUIDANCE_LINE_CONE_LAYER_ID) == null) {
        style.addLayer(
            LineLayer(USER_GUIDANCE_LINE_CONE_LAYER_ID, USER_GUIDANCE_LINE_SOURCE_ID)
                .withFilter(eq(get(USER_GUIDANCE_KIND_KEY), literal(USER_GUIDANCE_KIND_CONE)))
                .withProperties(
                    lineColor("#000000"),
                    lineWidth(1f),
                    lineOpacity(0.5f),
                ),
        )
    }

    if (style.getLayer(USER_GUIDANCE_LINE_TRACK_LAYER_ID) == null) {
        style.addLayer(
            LineLayer(USER_GUIDANCE_LINE_TRACK_LAYER_ID, USER_GUIDANCE_LINE_SOURCE_ID)
                .withFilter(eq(get(USER_GUIDANCE_KIND_KEY), literal(USER_GUIDANCE_KIND_TRACK)))
                .withProperties(
                    lineColor("#000000"),
                    lineWidth(1.5f),
                    lineOpacity(1f),
                ),
        )
    }

    if (style.getSourceAs<GeoJsonSource>(USER_GUIDANCE_MINUTE_MARK_SOURCE_ID) == null) {
        style.addSource(
            GeoJsonSource(USER_GUIDANCE_MINUTE_MARK_SOURCE_ID, FeatureCollection.fromFeatures(arrayOf())),
        )
    }

    if (style.getLayer(USER_GUIDANCE_MINUTE_MARK_LAYER_ID) == null) {
        style.addLayer(
            LineLayer(USER_GUIDANCE_MINUTE_MARK_LAYER_ID, USER_GUIDANCE_MINUTE_MARK_SOURCE_ID)
                .withProperties(
                    lineColor("#000000"),
                    lineWidth(2f),
                    lineOpacity(1f),
                ),
        )
    }
}

internal fun updateUserGuidanceLayers(style: Style, location: FlightLocation?) {
    style.getSourceAs<GeoJsonSource>(USER_GUIDANCE_LINE_SOURCE_ID)
        ?.setGeoJson(buildUserGuidanceLineFeatureCollection(location))
    style.getSourceAs<GeoJsonSource>(USER_GUIDANCE_MINUTE_MARK_SOURCE_ID)
        ?.setGeoJson(buildUserGuidanceMinuteMarkFeatureCollection(location))
}

internal fun ensureTerrainHazardLayers(style: Style) {
    if (style.getSourceAs<GeoJsonSource>(TERRAIN_SOURCE_ID) == null) {
        style.addSource(GeoJsonSource(TERRAIN_SOURCE_ID, FeatureCollection.fromFeatures(arrayOf())))
    }

    if (style.getLayer(TERRAIN_YELLOW_LAYER_ID) == null) {
        style.addLayer(
            CircleLayer(TERRAIN_YELLOW_LAYER_ID, TERRAIN_SOURCE_ID)
                .withFilter(eq(get(TERRAIN_SEVERITY_KEY), literal(TERRAIN_SEVERITY_NEAR)))
                .withProperties(
                    circleColor("#FFC107"),
                    circleRadius(5f),
                    circleOpacity(0.78f),
                ),
        )
    }

    if (style.getLayer(TERRAIN_RED_LAYER_ID) == null) {
        style.addLayer(
            CircleLayer(TERRAIN_RED_LAYER_ID, TERRAIN_SOURCE_ID)
                .withFilter(eq(get(TERRAIN_SEVERITY_KEY), literal(TERRAIN_SEVERITY_CONFLICT)))
                .withProperties(
                    circleColor("#E53935"),
                    circleRadius(6.5f),
                    circleOpacity(0.86f),
                ),
        )
    }
}

internal fun buildTerrainHazardFeatureCollection(samples: List<TerrainHazardSample>): FeatureCollection {
    val features = samples.map { sample ->
        Feature.fromGeometry(Point.fromLngLat(sample.longitude, sample.latitude)).apply {
            addStringProperty(TERRAIN_SEVERITY_KEY, sample.level.toSeverityTag())
        }
    }
    return FeatureCollection.fromFeatures(features)
}

private fun TerrainHazardLevel.toSeverityTag(): String = when (this) {
    TerrainHazardLevel.NearConflict -> TERRAIN_SEVERITY_NEAR
    TerrainHazardLevel.Conflict -> TERRAIN_SEVERITY_CONFLICT
}

internal fun ensureRouteLayers(style: Style) {
    if (style.getSourceAs<GeoJsonSource>(ROUTE_LINE_SOURCE_ID) == null) {
        style.addSource(GeoJsonSource(ROUTE_LINE_SOURCE_ID, FeatureCollection.fromFeatures(arrayOf())))
    }

    if (style.getLayer(ROUTE_LINE_LAYER_ID) == null) {
        style.addLayer(
            LineLayer(ROUTE_LINE_LAYER_ID, ROUTE_LINE_SOURCE_ID)
                .withProperties(
                    lineColor(ROUTE_LINE_COLOR),
                    lineWidth(3.5f),
                    lineOpacity(0.92f),
                ),
        )
    }

    if (style.getSourceAs<GeoJsonSource>(ROUTE_POINT_SOURCE_ID) == null) {
        style.addSource(GeoJsonSource(ROUTE_POINT_SOURCE_ID, FeatureCollection.fromFeatures(arrayOf())))
    }

    if (style.getLayer(ROUTE_POINT_CIRCLE_LAYER_ID) == null) {
        style.addLayer(
            CircleLayer(ROUTE_POINT_CIRCLE_LAYER_ID, ROUTE_POINT_SOURCE_ID)
                .withProperties(
                    circleColor(ROUTE_POINT_COLOR),
                    circleRadius(10f),
                    circleOpacity(1f),
                    circleStrokeWidth(2f),
                    circleStrokeColor("#FFFFFF"),
                ),
        )
    }

    if (style.getLayer(ROUTE_POINT_LABEL_LAYER_ID) == null) {
        style.addLayer(
            SymbolLayer(ROUTE_POINT_LABEL_LAYER_ID, ROUTE_POINT_SOURCE_ID)
                .withProperties(
                    textField(get(ROUTE_POINT_LABEL_KEY)),
                    textSize(12f),
                    textColor("#FFFFFF"),
                    textHaloColor("#000000"),
                    textHaloWidth(0.4f),
                ),
        )
    }
}

internal fun updateRouteLayers(
    style: Style,
    location: FlightLocation?,
    destinations: List<SearchDockRoutePoint>,
) {
    style.getSourceAs<GeoJsonSource>(ROUTE_LINE_SOURCE_ID)
        ?.setGeoJson(buildRouteLineFeatureCollection(location, destinations))
    style.getSourceAs<GeoJsonSource>(ROUTE_POINT_SOURCE_ID)
        ?.setGeoJson(buildRoutePointFeatureCollection(location, destinations))
}

private fun buildRouteLineFeatureCollection(
    location: FlightLocation?,
    destinations: List<SearchDockRoutePoint>,
): FeatureCollection {
    val points = buildRoutePoints(location, destinations)
    if (points.size < 2) {
        return FeatureCollection.fromFeatures(arrayOf())
    }

    return FeatureCollection.fromFeatures(arrayOf(Feature.fromGeometry(LineString.fromLngLats(points))))
}

private fun buildRoutePointFeatureCollection(
    location: FlightLocation?,
    destinations: List<SearchDockRoutePoint>,
): FeatureCollection {
    val features = buildRoutePoints(location, destinations).mapIndexed { index, point ->
        Feature.fromGeometry(point).apply {
            addStringProperty(ROUTE_POINT_LABEL_KEY, routePointLabelForIndex(index))
        }
    }

    return FeatureCollection.fromFeatures(features)
}

private fun buildRoutePoints(
    location: FlightLocation?,
    destinations: List<SearchDockRoutePoint>,
): List<Point> {
    if (location == null || location.requiresPermission || destinations.isEmpty()) {
        return emptyList()
    }

    return listOf(Point.fromLngLat(location.longitude, location.latitude)) +
        destinations.map { Point.fromLngLat(it.longitude, it.latitude) }
}

private fun routePointLabelForIndex(index: Int): String {
    return ('A'.code + index).toChar().toString()
}


private fun buildUserLocationDotFeatureCollection(location: FlightLocation?): FeatureCollection {
    if (location == null || location.requiresPermission) {
        return FeatureCollection.fromFeatures(arrayOf())
    }

    return FeatureCollection.fromFeatures(
        arrayOf(
            Feature.fromGeometry(Point.fromLngLat(location.longitude, location.latitude)),
        ),
    )
}

private fun buildUserLocationAccuracyFeatureCollection(location: FlightLocation?): FeatureCollection {
    if (location == null || location.requiresPermission) {
        return FeatureCollection.fromFeatures(arrayOf())
    }

    val radiusMeters = location.horizontalAccuracyMeters
        ?.takeIf { it.isFinite() && it > 0.0 }
        ?: return FeatureCollection.fromFeatures(arrayOf())

    val polygon = Polygon.fromLngLats(
        listOf(buildCircleRingPoints(location.latitude, location.longitude, radiusMeters)),
    )

    return FeatureCollection.fromFeatures(arrayOf(Feature.fromGeometry(polygon)))
}

private fun buildUserGuidanceLineFeatureCollection(location: FlightLocation?): FeatureCollection {
    val guidance = resolveGuidanceGeometry(location)
        ?: return FeatureCollection.fromFeatures(arrayOf())

    return FeatureCollection.fromFeatures(
        arrayOf(
            Feature.fromGeometry(
                LineString.fromLngLats(listOf(guidance.origin, guidance.trackEndpoint)),
            ).apply {
                addStringProperty(USER_GUIDANCE_KIND_KEY, USER_GUIDANCE_KIND_TRACK)
            },
            Feature.fromGeometry(
                LineString.fromLngLats(listOf(guidance.origin, guidance.leftConeEndpoint)),
            ).apply {
                addStringProperty(USER_GUIDANCE_KIND_KEY, USER_GUIDANCE_KIND_CONE)
            },
            Feature.fromGeometry(
                LineString.fromLngLats(listOf(guidance.origin, guidance.rightConeEndpoint)),
            ).apply {
                addStringProperty(USER_GUIDANCE_KIND_KEY, USER_GUIDANCE_KIND_CONE)
            },
        ),
    )
}

private fun buildUserGuidanceMinuteMarkFeatureCollection(location: FlightLocation?): FeatureCollection {
    val guidance = resolveGuidanceGeometry(location)
        ?: return FeatureCollection.fromFeatures(arrayOf())

    val markDistanceMeters = guidance.speedMetersPerSecond * 60.0
    val maxMarksByDistance = kotlin.math.floor(USER_GUIDANCE_LOOKAHEAD_METERS / markDistanceMeters).toInt()
    val minuteMarkCount = minOf(USER_GUIDANCE_MAX_MINUTE_MARKS, maxMarksByDistance)
    if (minuteMarkCount <= 0) {
        return FeatureCollection.fromFeatures(arrayOf())
    }

    val features = (1..minuteMarkCount).map { minute ->
        val markPoint = destinationPoint(
            latitude = guidance.originLatitude,
            longitude = guidance.originLongitude,
            bearingDegrees = guidance.bearingDegrees,
            distanceMeters = minute * markDistanceMeters,
        )

        val halfTickLengthMeters = USER_GUIDANCE_TICK_MARK_LENGTH_METERS / 2.0
        val leftTickPoint = destinationPoint(
            latitude = markPoint.latitude(),
            longitude = markPoint.longitude(),
            bearingDegrees = normalizeBearing(guidance.bearingDegrees - 90.0),
            distanceMeters = halfTickLengthMeters,
        )
        val rightTickPoint = destinationPoint(
            latitude = markPoint.latitude(),
            longitude = markPoint.longitude(),
            bearingDegrees = normalizeBearing(guidance.bearingDegrees + 90.0),
            distanceMeters = halfTickLengthMeters,
        )

        Feature.fromGeometry(LineString.fromLngLats(listOf(leftTickPoint, rightTickPoint)))
    }

    return FeatureCollection.fromFeatures(features)
}
