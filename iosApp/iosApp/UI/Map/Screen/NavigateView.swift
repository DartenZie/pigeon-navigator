//
//  NavigationView.swift
//  iosApp
//
//  Created by Miroslav Pašek on 07.02.2026.
//

import SwiftUI
import MapLibre
import CoreLocation
import UIKit
import QuartzCore
import Shared

struct NavigateView: UIViewRepresentable {
    let location: CLLocationCoordinate2D?
    var locationAccuracyMeters: Double? = nil
    var locationSpeedMetersPerSecond: Double = 0
    var locationBearingDegrees: Double? = nil
    var terrainHazardPoints: [TerrainHazardOverlayPoint] = []
    var followUser: Bool = true
    var zoomLevel = 10.5
    var onDirectionChange: (CLLocationDirection) -> Void = { _ in }
    var onMapInteraction: () -> Void = {}
    var onMapTap: (CLLocationCoordinate2D) -> Void = { _ in }
    var onAwayFromUserLocationChange: (Bool) -> Void = { _ in }
    var resetNorthToken: Int = 0
    var recenterOnUserToken: Int = 0
    var mapFocus: MapCameraFocus? = nil
    var mapFocusToken: Int = 0
    var bearingUpdateThresholdDegreesOverride: Double? = nil
    var maxDynamicZoomSpeedKmhOverride: Double? = nil
    var maxSpeedZoomOutDeltaOverride: Double? = nil
    private let recenterDistanceMeters: CLLocationDistance = 12
    private let awayFromUserDistanceMeters: CLLocationDistance = 24
    private let userLocationDotSourceId = "user-location-dot-source"
    private let userLocationDotLayerId = "user-location-dot-layer"
    private let userLocationAccuracySourceId = "user-location-accuracy-source"
    private let userLocationAccuracyLayerId = "user-location-accuracy-layer"
    private let userGuidanceLineSourceId = "user-guidance-line-source"
    private let userGuidanceTrackLayerId = "user-guidance-track-layer"
    private let userGuidanceConeLayerId = "user-guidance-cone-layer"
    private let userGuidanceMinuteMarkSourceId = "user-guidance-minute-mark-source"
    private let userGuidanceMinuteMarkLayerId = "user-guidance-minute-mark-layer"
    private let terrainHazardNearSourceId = "terrain-hazard-near-source"
    private let terrainHazardNearLayerId = "terrain-hazard-near-layer"
    private let terrainHazardConflictSourceId = "terrain-hazard-conflict-source"
    private let terrainHazardConflictLayerId = "terrain-hazard-conflict-layer"
    private let userGuidanceLookAheadMeters = 20_000.0
    private let userGuidanceConeHalfAngleDegrees = 25.0
    private let userGuidanceMaxMinuteMarks = 12
    private let userGuidanceTickMarkLengthMeters = 180.0
    private let userGuidanceMinSpeedMetersPerSecond = 0.8
    private let bearingUpdateThresholdDegreesDefault = 4.0
    private let maxDynamicZoomSpeedKmhDefault = 300.0
    private let maxSpeedZoomOutDeltaDefault = 2.5

    private var bearingUpdateThresholdDegrees: Double {
        bearingUpdateThresholdDegreesOverride ?? bearingUpdateThresholdDegreesDefault
    }

    private var maxDynamicZoomSpeedKmh: Double {
        maxDynamicZoomSpeedKmhOverride ?? maxDynamicZoomSpeedKmhDefault
    }

    private var maxSpeedZoomOutDelta: Double {
        maxSpeedZoomOutDeltaOverride ?? maxSpeedZoomOutDeltaDefault
    }

    private var dynamicDefaultZoomLevel: Double {
        let maxSpeed = maxDynamicZoomSpeedKmh
        guard maxSpeed > 0 else { return zoomLevel }
        let speedKmh = max(locationSpeedMetersPerSecond, 0) * 3.6
        let progress = min(speedKmh / maxSpeed, 1)
        return zoomLevel - progress * maxSpeedZoomOutDelta
    }
    
    func makeCoordinator() -> Coordinator {
        Coordinator(
            onDirectionChange: onDirectionChange,
            onMapInteraction: onMapInteraction,
            onMapTap: onMapTap,
            onAwayFromUserLocationChange: onAwayFromUserLocationChange,
            awayFromUserDistanceMeters: awayFromUserDistanceMeters
        )
    }
    
    func makeUIView(context: Context) -> MLNMapView {
        let styleURL = MapStyleNormalizer.resolveStyleURL()
            ?? Bundle.main.url(forResource: "style", withExtension: "json", subdirectory: "MapAssets")

        let mapView = MLNMapView(frame: .zero)
        if let styleURL {
            mapView.styleURL = styleURL
        } else {
            print("Failed to resolve map style URL")
        }
        mapView.delegate = context.coordinator
        
        mapView.allowsRotating = true
        mapView.allowsTilting = true
        mapView.compassView.isHidden = true

        mapView.gestureRecognizers?
            .compactMap { $0 as? UIPanGestureRecognizer }
            .forEach { $0.addTarget(context.coordinator, action: #selector(Coordinator.handlePanGesture(_:))) }

        let mapTapGesture = UITapGestureRecognizer(
            target: context.coordinator,
            action: #selector(Coordinator.handleMapTapGesture(_:))
        )
        mapTapGesture.cancelsTouchesInView = false
        mapView.addGestureRecognizer(mapTapGesture)

        return mapView
    }

    func updateUIView(_ mapView: MLNMapView, context: Context) {
        mapView.attributionButton.isHidden = true
        mapView.logoView.isHidden = true
        mapView.compassView.isHidden = true

        context.coordinator.onDirectionChange = onDirectionChange
        context.coordinator.onMapInteraction = onMapInteraction
        context.coordinator.onMapTap = onMapTap
        context.coordinator.onAwayFromUserLocationChange = onAwayFromUserLocationChange
        context.coordinator.renderTerrainHazardsTemplate(
            mapView,
            points: terrainHazardPoints,
            nearSourceId: terrainHazardNearSourceId,
            nearLayerId: terrainHazardNearLayerId,
            conflictSourceId: terrainHazardConflictSourceId,
            conflictLayerId: terrainHazardConflictLayerId
        )
        context.coordinator.renderUserLocationIndicator(
            mapView,
            location: location,
            horizontalAccuracyMeters: locationAccuracyMeters,
            dotSourceId: userLocationDotSourceId,
            dotLayerId: userLocationDotLayerId,
            accuracySourceId: userLocationAccuracySourceId,
            accuracyLayerId: userLocationAccuracyLayerId
        )
        context.coordinator.renderGuidanceOverlay(
            mapView,
            location: location,
            speedMetersPerSecond: locationSpeedMetersPerSecond,
            bearingDegrees: locationBearingDegrees,
            lineSourceId: userGuidanceLineSourceId,
            trackLayerId: userGuidanceTrackLayerId,
            coneLayerId: userGuidanceConeLayerId,
            minuteMarkSourceId: userGuidanceMinuteMarkSourceId,
            minuteMarkLayerId: userGuidanceMinuteMarkLayerId,
            lookAheadMeters: userGuidanceLookAheadMeters,
            coneHalfAngleDegrees: userGuidanceConeHalfAngleDegrees,
            maxMinuteMarks: userGuidanceMaxMinuteMarks,
            tickMarkLengthMeters: userGuidanceTickMarkLengthMeters,
            minSpeedMetersPerSecond: userGuidanceMinSpeedMetersPerSecond
        )

        if context.coordinator.lastResetNorthToken != resetNorthToken {
            context.coordinator.lastResetNorthToken = resetNorthToken
            context.coordinator.animateResetNorth(mapView)
        }

        if context.coordinator.lastRecenterOnUserToken != recenterOnUserToken {
            context.coordinator.lastRecenterOnUserToken = recenterOnUserToken
            context.coordinator.isTrackingUserLocation = true
            if let userLocation = context.coordinator.lastKnownUserLocation {
                mapView.setCenter(userLocation, zoomLevel: dynamicDefaultZoomLevel, animated: true)
                context.coordinator.setAwayFromUserLocation(false)
            }
        }

        if context.coordinator.lastMapFocusToken != mapFocusToken {
            context.coordinator.lastMapFocusToken = mapFocusToken
            context.coordinator.isTrackingUserLocation = false
            if let mapFocus {
                if let bounds = mapFocus.bounds {
                    let coordinateBounds = MLNCoordinateBounds(
                        sw: bounds.southWest,
                        ne: bounds.northEast
                    )
                    mapView.setVisibleCoordinateBounds(
                        coordinateBounds,
                        edgePadding: UIEdgeInsets(top: 80, left: 48, bottom: 120, right: 48),
                        animated: true
                    )
                } else {
                    mapView.setCenter(mapFocus.center, zoomLevel: dynamicDefaultZoomLevel, animated: true)
                }
                context.coordinator.setAwayFromUserLocation(true)
            }
        }
        
        guard followUser, let loc = location else {
            context.coordinator.evaluateAwayFromUserLocation(mapView)
            return
        }
        
        guard mapView.style != nil else { return }

        context.coordinator.lastKnownUserLocation = loc

        let newLocation = CLLocation(latitude: loc.latitude, longitude: loc.longitude)
        
        if !context.coordinator.didSetInitialCamera {
            context.coordinator.didSetInitialCamera = true
            mapView.setCenter(loc, zoomLevel: dynamicDefaultZoomLevel, animated: false)
            context.coordinator.lastCenter = loc
            context.coordinator.lastFollowLocation = newLocation
            context.coordinator.didCenterOnFirstGpsFix = true
            context.coordinator.evaluateAwayFromUserLocation(mapView)
        } else {
            if !context.coordinator.didCenterOnFirstGpsFix {
                context.coordinator.didCenterOnFirstGpsFix = true
                mapView.setCenter(loc, zoomLevel: dynamicDefaultZoomLevel, animated: true)
                context.coordinator.lastCenter = loc
                context.coordinator.lastFollowLocation = newLocation
                context.coordinator.evaluateAwayFromUserLocation(mapView)
                return
            }

            guard context.coordinator.isTrackingUserLocation else { return }

            let currentCenter = mapView.centerCoordinate
            let current = CLLocation(latitude: currentCenter.latitude, longitude: currentCenter.longitude)
            let shouldRecenter = current.distance(from: newLocation) >= recenterDistanceMeters
            let desiredDirection = context.coordinator.resolveTrackingDirection(
                currentDirection: mapView.direction,
                speedMetersPerSecond: locationSpeedMetersPerSecond,
                bearingDegrees: locationBearingDegrees,
                minSpeedMetersPerSecond: userGuidanceMinSpeedMetersPerSecond
            )
            let shouldRotate = context.coordinator.angularDistanceDegrees(
                from: mapView.direction,
                to: desiredDirection
            ) >= bearingUpdateThresholdDegrees

            if shouldRecenter {
                mapView.setCenter(loc, zoomLevel: mapView.zoomLevel, animated: true)
                context.coordinator.lastCenter = loc
            }

            if shouldRotate {
                context.coordinator.animateDirection(mapView, to: desiredDirection)
            }

            context.coordinator.lastFollowLocation = newLocation
            context.coordinator.evaluateAwayFromUserLocation(mapView)
        }
    }
    
    final class Coordinator: NSObject, MLNMapViewDelegate {
        var didSetInitialCamera = false
        var didCenterOnFirstGpsFix = false
        var lastCenter: CLLocationCoordinate2D?
        var lastFollowLocation: CLLocation?
        var lastKnownUserLocation: CLLocationCoordinate2D?
        var isAwayFromUserLocation = false
        var isTrackingUserLocation = true
        var onDirectionChange: (CLLocationDirection) -> Void
        var onMapInteraction: () -> Void
        var onMapTap: (CLLocationCoordinate2D) -> Void
        var onAwayFromUserLocationChange: (Bool) -> Void
        let awayFromUserDistanceMeters: CLLocationDistance
        var lastResetNorthToken: Int = 0
        var lastRecenterOnUserToken: Int = 0
        var lastMapFocusToken: Int = 0
        private var lastForwardedMapTap: (coordinate: CLLocationCoordinate2D, timestamp: TimeInterval)?
        private weak var resetNorthMapView: MLNMapView?
        private var resetNorthDisplayLink: CADisplayLink?
        private var resetNorthStartTime: CFTimeInterval = 0
        private var resetNorthStartDirection: CLLocationDirection = 0
        private var resetNorthDeltaDirection: CLLocationDirection = 0
        private let resetNorthAnimationDuration: CFTimeInterval = 0.35

        init(
            onDirectionChange: @escaping (CLLocationDirection) -> Void,
            onMapInteraction: @escaping () -> Void,
            onMapTap: @escaping (CLLocationCoordinate2D) -> Void,
            onAwayFromUserLocationChange: @escaping (Bool) -> Void,
            awayFromUserDistanceMeters: CLLocationDistance
        ) {
            self.onDirectionChange = onDirectionChange
            self.onMapInteraction = onMapInteraction
            self.onMapTap = onMapTap
            self.onAwayFromUserLocationChange = onAwayFromUserLocationChange
            self.awayFromUserDistanceMeters = awayFromUserDistanceMeters
        }

        deinit {
            resetNorthDisplayLink?.invalidate()
        }

        @objc
        func handlePanGesture(_ recognizer: UIPanGestureRecognizer) {
            if recognizer.state == .began {
                stopResetNorthAnimation()
                isTrackingUserLocation = false
                onMapInteraction()
            }
        }

        @objc
        func handleMapTapGesture(_ recognizer: UITapGestureRecognizer) {
            guard recognizer.state == .ended,
                  let mapView = recognizer.view as? MLNMapView else {
                return
            }

            let tapPoint = recognizer.location(in: mapView)
            let coordinate = mapView.convert(tapPoint, toCoordinateFrom: mapView)
            forwardMapTap(coordinate, source: "gesture")
        }

        private func forwardMapTap(_ coordinate: CLLocationCoordinate2D, source: String) {
            let now = Date().timeIntervalSince1970
            if let previous = lastForwardedMapTap {
                let previousLocation = CLLocation(
                    latitude: previous.coordinate.latitude,
                    longitude: previous.coordinate.longitude
                )
                let currentLocation = CLLocation(
                    latitude: coordinate.latitude,
                    longitude: coordinate.longitude
                )
                let isNearPrevious = previousLocation.distance(from: currentLocation) < 1
                let isNearInTime = now - previous.timestamp < 0.25
                if isNearPrevious && isNearInTime {
                    return
                }
            }

            lastForwardedMapTap = (coordinate, now)
            print("[NavigateView] map tap(\(source)) lat=\(coordinate.latitude) lng=\(coordinate.longitude)")
            onMapTap(coordinate)
        }

        func shouldFollowForLocation(_ location: CLLocation) -> Bool {
            guard let previous = lastFollowLocation else { return true }
            return previous.distance(from: location) >= 0.5
        }

        func setAwayFromUserLocation(_ isAway: Bool) {
            guard isAwayFromUserLocation != isAway else { return }
            isAwayFromUserLocation = isAway
            onAwayFromUserLocationChange(isAway)
        }

        func evaluateAwayFromUserLocation(_ mapView: MLNMapView) {
            evaluateAwayFromUserLocation(
                center: mapView.centerCoordinate,
                userLocation: lastKnownUserLocation
            )
        }

        func evaluateAwayFromUserLocation(
            center: CLLocationCoordinate2D,
            userLocation: CLLocationCoordinate2D?
        ) {
            guard let userLocation else {
                setAwayFromUserLocation(false)
                return
            }

            let centerLocation = CLLocation(latitude: center.latitude, longitude: center.longitude)
            let gpsLocation = CLLocation(latitude: userLocation.latitude, longitude: userLocation.longitude)
            let isAway = centerLocation.distance(from: gpsLocation) >= awayFromUserDistanceMeters
            setAwayFromUserLocation(isAway)
        }

        func animateResetNorth(_ mapView: MLNMapView) {
            animateDirection(mapView, to: 0)
        }

        func animateDirection(_ mapView: MLNMapView, to targetDirection: CLLocationDirection) {
            stopResetNorthAnimation()

            let startDirection = normalizeDirection(mapView.direction)
            let targetDirection = normalizeDirection(targetDirection)
            let deltaDirection = shortestDirectionDelta(from: startDirection, to: targetDirection)
            guard abs(deltaDirection) > 0.1 else {
                mapView.setDirection(targetDirection, animated: false)
                onDirectionChange(targetDirection)
                return
            }

            resetNorthMapView = mapView
            resetNorthStartTime = CACurrentMediaTime()
            resetNorthStartDirection = startDirection
            resetNorthDeltaDirection = deltaDirection

            let displayLink = CADisplayLink(target: self, selector: #selector(stepResetNorthAnimation(_:)))
            resetNorthDisplayLink = displayLink
            displayLink.add(to: .main, forMode: .common)
        }

        @objc
        private func stepResetNorthAnimation(_ displayLink: CADisplayLink) {
            guard let mapView = resetNorthMapView else {
                stopResetNorthAnimation()
                return
            }

            let elapsed = displayLink.timestamp - resetNorthStartTime
            let progress = min(max(elapsed / resetNorthAnimationDuration, 0), 1)
            let easedProgress = progress * progress * (3 - 2 * progress)
            let direction = normalizeDirection(
                resetNorthStartDirection + resetNorthDeltaDirection * easedProgress
            )

            mapView.setDirection(direction, animated: false)
            onDirectionChange(direction)

            if progress >= 1 {
                let finalDirection = normalizeDirection(resetNorthStartDirection + resetNorthDeltaDirection)
                mapView.setDirection(finalDirection, animated: false)
                onDirectionChange(finalDirection)
                stopResetNorthAnimation()
            }
        }

        private func stopResetNorthAnimation() {
            resetNorthDisplayLink?.invalidate()
            resetNorthDisplayLink = nil
            resetNorthMapView = nil
        }

        private func normalizeDirection(_ direction: CLLocationDirection) -> CLLocationDirection {
            MapGeometry.normalizeBearingDegrees(direction)
        }

        private func shortestDirectionDelta(
            from startDirection: CLLocationDirection,
            to endDirection: CLLocationDirection
        ) -> CLLocationDirection {
            MapGeometry.shortestDirectionDelta(from: startDirection, to: endDirection)
        }

        func angularDistanceDegrees(
            from startDirection: CLLocationDirection,
            to endDirection: CLLocationDirection
        ) -> CLLocationDirection {
            abs(MapGeometry.shortestDirectionDelta(from: startDirection, to: endDirection))
        }

        func resolveTrackingDirection(
            currentDirection: CLLocationDirection,
            speedMetersPerSecond: Double,
            bearingDegrees: Double?,
            minSpeedMetersPerSecond: Double
        ) -> CLLocationDirection {
            guard speedMetersPerSecond >= minSpeedMetersPerSecond,
                  let bearingDegrees,
                  bearingDegrees.isFinite,
                  bearingDegrees >= 0,
                  bearingDegrees <= 360 else {
                return normalizeDirection(currentDirection)
            }

            return normalizeDirection(bearingDegrees)
        }
        
        func mapView(_ mapView: MLNMapView, didFinishLoading style: MLNStyle) {
            if !didSetInitialCamera {
                let prague = CLLocationCoordinate2D(latitude: 50.0755, longitude: 14.4378)
                mapView.setCenter(prague, zoomLevel: 10.5, animated: false)
            }
            
            mapView.attributionButton.isHidden = true
            mapView.logoView.isHidden = true
            mapView.compassView.isHidden = true
            onDirectionChange(mapView.direction)
            evaluateAwayFromUserLocation(mapView)
        }

        func mapViewRegionIsChanging(_ mapView: MLNMapView) {
            onDirectionChange(mapView.direction)
            evaluateAwayFromUserLocation(mapView)
        }

        func mapView(_ mapView: MLNMapView, regionDidChangeAnimated animated: Bool) {
            onDirectionChange(mapView.direction)
            evaluateAwayFromUserLocation(mapView)
        }

        func mapView(_ mapView: MLNMapView, didTapAt coordinate: CLLocationCoordinate2D) {
            forwardMapTap(coordinate, source: "delegate")
        }

        func renderTerrainHazardsTemplate(
            _ mapView: MLNMapView,
            points: [TerrainHazardOverlayPoint],
            nearSourceId: String,
            nearLayerId: String,
            conflictSourceId: String,
            conflictLayerId: String
        ) {
            guard let style = mapView.style else { return }

            let nearSource: MLNShapeSource
            if let existing = style.source(withIdentifier: nearSourceId) as? MLNShapeSource {
                nearSource = existing
            } else {
                nearSource = MLNShapeSource(identifier: nearSourceId, shape: nil, options: nil)
                style.addSource(nearSource)
            }

            if style.layer(withIdentifier: nearLayerId) == nil {
                let layer = MLNCircleStyleLayer(identifier: nearLayerId, source: nearSource)
                layer.circleColor = NSExpression(
                    forConstantValue: UIColor(red: 1.0, green: 0.757, blue: 0.027, alpha: 1.0)
                )
                layer.circleRadius = NSExpression(forConstantValue: 5)
                layer.circleOpacity = NSExpression(forConstantValue: 0.78)
                style.addLayer(layer)
            }

            let conflictSource: MLNShapeSource
            if let existing = style.source(withIdentifier: conflictSourceId) as? MLNShapeSource {
                conflictSource = existing
            } else {
                conflictSource = MLNShapeSource(identifier: conflictSourceId, shape: nil, options: nil)
                style.addSource(conflictSource)
            }

            if style.layer(withIdentifier: conflictLayerId) == nil {
                let layer = MLNCircleStyleLayer(identifier: conflictLayerId, source: conflictSource)
                layer.circleColor = NSExpression(
                    forConstantValue: UIColor(red: 0.898, green: 0.224, blue: 0.208, alpha: 1.0)
                )
                layer.circleRadius = NSExpression(forConstantValue: 6.5)
                layer.circleOpacity = NSExpression(forConstantValue: 0.86)
                style.addLayer(layer)
            }

            let nearPoints = points.filter { $0.severity == .near }
            let conflictPoints = points.filter { $0.severity == .conflict }

            nearSource.shape = buildTerrainHazardShape(points: nearPoints)
            conflictSource.shape = buildTerrainHazardShape(points: conflictPoints)
        }

        private func buildTerrainHazardShape(points: [TerrainHazardOverlayPoint]) -> MLNShape? {
            guard !points.isEmpty else { return nil }
            let pointFeatures: [MLNShape] = points.map { point in
                let feature = MLNPointFeature()
                feature.coordinate = point.coordinate
                return feature
            }
            return MLNShapeCollectionFeature(shapes: pointFeatures)
        }

        func renderUserLocationIndicator(
            _ mapView: MLNMapView,
            location: CLLocationCoordinate2D?,
            horizontalAccuracyMeters: Double?,
            dotSourceId: String,
            dotLayerId: String,
            accuracySourceId: String,
            accuracyLayerId: String
        ) {
            guard let style = mapView.style else { return }

            let dotSource: MLNShapeSource
            if let existing = style.source(withIdentifier: dotSourceId) as? MLNShapeSource {
                dotSource = existing
            } else {
                dotSource = MLNShapeSource(identifier: dotSourceId, shape: nil, options: nil)
                style.addSource(dotSource)
            }

            if style.layer(withIdentifier: dotLayerId) == nil {
                let layer = MLNCircleStyleLayer(identifier: dotLayerId, source: dotSource)
                layer.circleColor = NSExpression(forConstantValue: UIColor(red: 0.12, green: 0.53, blue: 0.90, alpha: 1.0))
                layer.circleRadius = NSExpression(forConstantValue: 7)
                layer.circleOpacity = NSExpression(forConstantValue: 1.0)
                layer.circleStrokeColor = NSExpression(forConstantValue: UIColor.white)
                layer.circleStrokeWidth = NSExpression(forConstantValue: 2)
                style.addLayer(layer)
            }

            if let location {
                let point = MLNPointFeature()
                point.coordinate = location
                dotSource.shape = point
            } else {
                dotSource.shape = nil
            }

            let accuracySource: MLNShapeSource
            if let existing = style.source(withIdentifier: accuracySourceId) as? MLNShapeSource {
                accuracySource = existing
            } else {
                accuracySource = MLNShapeSource(identifier: accuracySourceId, shape: nil, options: nil)
                style.addSource(accuracySource)
            }

            if style.layer(withIdentifier: accuracyLayerId) == nil {
                let layer = MLNFillStyleLayer(identifier: accuracyLayerId, source: accuracySource)
                let color = UIColor(red: 0.26, green: 0.65, blue: 0.96, alpha: 1.0)
                layer.fillColor = NSExpression(forConstantValue: color)
                layer.fillOpacity = NSExpression(forConstantValue: 0.2)
                layer.fillOutlineColor = NSExpression(forConstantValue: color)
                style.addLayer(layer)
            }

            if let location,
               let horizontalAccuracyMeters,
               horizontalAccuracyMeters > 0 {
                accuracySource.shape = buildAccuracyPolygon(
                    center: location,
                    radiusMeters: horizontalAccuracyMeters
                )
            } else {
                accuracySource.shape = nil
            }
        }

        private func buildAccuracyPolygon(
            center: CLLocationCoordinate2D,
            radiusMeters: Double,
            segments: Int = 64
        ) -> MLNPolygonFeature {
            MapGeometry.buildAccuracyPolygon(
                center: center,
                radiusMeters: radiusMeters,
                segments: segments
            )
        }

        func renderGuidanceOverlay(
            _ mapView: MLNMapView,
            location: CLLocationCoordinate2D?,
            speedMetersPerSecond: Double,
            bearingDegrees: Double?,
            lineSourceId: String,
            trackLayerId: String,
            coneLayerId: String,
            minuteMarkSourceId: String,
            minuteMarkLayerId: String,
            lookAheadMeters: Double,
            coneHalfAngleDegrees: Double,
            maxMinuteMarks: Int,
            tickMarkLengthMeters: Double,
            minSpeedMetersPerSecond: Double
        ) {
            guard let style = mapView.style else { return }

            let lineSource: MLNShapeSource
            if let existing = style.source(withIdentifier: lineSourceId) as? MLNShapeSource {
                lineSource = existing
            } else {
                lineSource = MLNShapeSource(identifier: lineSourceId, shape: nil, options: nil)
                style.addSource(lineSource)
            }

            if style.layer(withIdentifier: coneLayerId) == nil {
                let layer = MLNLineStyleLayer(identifier: coneLayerId, source: lineSource)
                layer.predicate = NSPredicate(format: "kind == %@", "cone")
                layer.lineColor = NSExpression(forConstantValue: UIColor.black)
                layer.lineWidth = NSExpression(forConstantValue: 1)
                layer.lineOpacity = NSExpression(forConstantValue: 0.5)
                style.addLayer(layer)
            }

            if style.layer(withIdentifier: trackLayerId) == nil {
                let layer = MLNLineStyleLayer(identifier: trackLayerId, source: lineSource)
                layer.predicate = NSPredicate(format: "kind == %@", "track")
                layer.lineColor = NSExpression(forConstantValue: UIColor.black)
                layer.lineWidth = NSExpression(forConstantValue: 1.5)
                layer.lineOpacity = NSExpression(forConstantValue: 1.0)
                style.addLayer(layer)
            }

            let minuteMarkSource: MLNShapeSource
            if let existing = style.source(withIdentifier: minuteMarkSourceId) as? MLNShapeSource {
                minuteMarkSource = existing
            } else {
                minuteMarkSource = MLNShapeSource(identifier: minuteMarkSourceId, shape: nil, options: nil)
                style.addSource(minuteMarkSource)
            }

            if style.layer(withIdentifier: minuteMarkLayerId) == nil {
                let layer = MLNLineStyleLayer(identifier: minuteMarkLayerId, source: minuteMarkSource)
                layer.lineColor = NSExpression(forConstantValue: UIColor.black)
                layer.lineWidth = NSExpression(forConstantValue: 2)
                layer.lineOpacity = NSExpression(forConstantValue: 1.0)
                style.addLayer(layer)
            }

            guard let location,
                  let bearingDegrees,
                  bearingDegrees.isFinite,
                  speedMetersPerSecond.isFinite,
                  speedMetersPerSecond >= minSpeedMetersPerSecond,
                  bearingDegrees >= 0,
                  bearingDegrees <= 360 else {
                lineSource.shape = nil
                minuteMarkSource.shape = nil
                return
            }

            let normalizedBearing = normalizeBearingDegrees(bearingDegrees)
            let trackEnd = destinationCoordinate(
                from: location,
                bearingDegrees: normalizedBearing,
                distanceMeters: lookAheadMeters
            )
            let leftConeEnd = destinationCoordinate(
                from: location,
                bearingDegrees: normalizeBearingDegrees(normalizedBearing - coneHalfAngleDegrees),
                distanceMeters: lookAheadMeters
            )
            let rightConeEnd = destinationCoordinate(
                from: location,
                bearingDegrees: normalizeBearingDegrees(normalizedBearing + coneHalfAngleDegrees),
                distanceMeters: lookAheadMeters
            )

            lineSource.shape = MLNShapeCollectionFeature(
                shapes: [
                    makeLineFeature(from: location, to: trackEnd, kind: "track"),
                    makeLineFeature(from: location, to: leftConeEnd, kind: "cone"),
                    makeLineFeature(from: location, to: rightConeEnd, kind: "cone")
                ]
            )

            let oneMinuteDistance = speedMetersPerSecond * 60
            let maxMarksByDistance = Int(floor(lookAheadMeters / oneMinuteDistance))
            let minuteMarkCount = min(maxMinuteMarks, maxMarksByDistance)
            if minuteMarkCount <= 0 {
                minuteMarkSource.shape = nil
            } else {
                let halfTickLengthMeters = tickMarkLengthMeters / 2
                let marks: [MLNPolylineFeature] = (1...minuteMarkCount).map { minute in
                    let markCoordinate = destinationCoordinate(
                        from: location,
                        bearingDegrees: normalizedBearing,
                        distanceMeters: Double(minute) * oneMinuteDistance
                    )

                    let tickLeft = destinationCoordinate(
                        from: markCoordinate,
                        bearingDegrees: normalizeBearingDegrees(normalizedBearing - 90),
                        distanceMeters: halfTickLengthMeters
                    )
                    let tickRight = destinationCoordinate(
                        from: markCoordinate,
                        bearingDegrees: normalizeBearingDegrees(normalizedBearing + 90),
                        distanceMeters: halfTickLengthMeters
                    )
                    return makeLineFeature(from: tickLeft, to: tickRight, kind: "tick")
                }
                minuteMarkSource.shape = MLNShapeCollectionFeature(shapes: marks)
            }
        }

        private func makeLineFeature(
            from start: CLLocationCoordinate2D,
            to end: CLLocationCoordinate2D,
            kind: String
        ) -> MLNPolylineFeature {
            var coordinates = [start, end]
            let line = MLNPolylineFeature(coordinates: &coordinates, count: UInt(coordinates.count))
            line.attributes = ["kind": kind]
            return line
        }

        private func destinationCoordinate(
            from start: CLLocationCoordinate2D,
            bearingDegrees: Double,
            distanceMeters: Double
        ) -> CLLocationCoordinate2D {
            MapGeometry.destinationCoordinate(
                from: start,
                bearingDegrees: bearingDegrees,
                distanceMeters: distanceMeters
            )
        }

        private func normalizeBearingDegrees(_ bearing: Double) -> Double {
            MapGeometry.normalizeBearingDegrees(bearing)
        }
    }
}
