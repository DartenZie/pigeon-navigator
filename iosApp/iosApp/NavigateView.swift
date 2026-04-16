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
import Shared

struct NavigateView: UIViewRepresentable {
    let location: CLLocationCoordinate2D?
    var locationAccuracyMeters: Double? = nil
    var terrainHazardPoints: [TerrainHazardOverlayPoint] = []
    var followUser: Bool = true
    var zoomLevel = 10.5
    var onDirectionChange: (CLLocationDirection) -> Void = { _ in }
    var onMapTap: (CLLocationCoordinate2D) -> Void = { _ in }
    var onAwayFromUserLocationChange: (Bool) -> Void = { _ in }
    var resetNorthToken: Int = 0
    var recenterOnUserToken: Int = 0
    private let recenterDistanceMeters: CLLocationDistance = 12
    private let awayFromUserDistanceMeters: CLLocationDistance = 24
    private let userLocationDotSourceId = "user-location-dot-source"
    private let userLocationDotLayerId = "user-location-dot-layer"
    private let userLocationAccuracySourceId = "user-location-accuracy-source"
    private let userLocationAccuracyLayerId = "user-location-accuracy-layer"
    
    func makeCoordinator() -> Coordinator {
        Coordinator(
            onDirectionChange: onDirectionChange,
            onMapTap: onMapTap,
            onAwayFromUserLocationChange: onAwayFromUserLocationChange,
            awayFromUserDistanceMeters: awayFromUserDistanceMeters
        )
    }
    
    func makeUIView(context: Context) -> MLNMapView {
        let styleURL = resolveStyleURL()
            ?? Bundle.main.url(forResource: "style", withExtension: "json", subdirectory: "MapAssets")!
                 
        let mapView = MLNMapView(frame: .zero, styleURL: styleURL)
        mapView.delegate = context.coordinator
        
        mapView.allowsRotating = true
        mapView.allowsTilting = true
        mapView.compassView.isHidden = true

        mapView.gestureRecognizers?
            .compactMap { $0 as? UIPanGestureRecognizer }
            .forEach { $0.addTarget(context.coordinator, action: #selector(Coordinator.handlePanGesture(_:))) }

        return mapView
    }

    private func resolveStyleURL() -> URL? {
        let styleBridge = MapStyleBridge()
        guard let rawStyleJson = styleBridge.resolveStyleJson() else {
            return nil
        }
        let styleJson = normalizeStyleForIOS(styleJson: rawStyleJson)
        let outputDir = FileManager.default.temporaryDirectory.appendingPathComponent("map-style", isDirectory: true)
        let outputFile = outputDir.appendingPathComponent("style.generated.json")

        do {
            try FileManager.default.createDirectory(at: outputDir, withIntermediateDirectories: true)
            try styleJson.write(to: outputFile, atomically: true, encoding: .utf8)
            return outputFile
        } catch {
            print("Failed to write generated map style: \(error)")
            return nil
        }
    }

    private func normalizeStyleForIOS(styleJson: String) -> String {
        guard let data = styleJson.data(using: .utf8) else {
            return styleJson
        }

        do {
            guard var root = try JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let layers = root["layers"] as? [[String: Any]] else {
                return styleJson
            }

            if let resourcePath = Bundle.main.resourcePath {
                let glyphPath = "\(resourcePath)/MapAssets/fonts/{fontstack}/{range}.pbf"
                root["glyphs"] = "file://\(glyphPath)"
                let spritePath = "\(resourcePath)/MapAssets/sprites/sprite"
                root["sprite"] = "file://\(spritePath)"
            }

            var normalizedLayers = [[String: Any]]()
            normalizedLayers.reserveCapacity(layers.count)

            for var layer in layers {
                guard layer["type"] as? String == "symbol" else {
                    normalizedLayers.append(layer)
                    continue
                }

                if let sourceLayer = layer["source-layer"] as? String,
                   sourceLayer == "place_label_city" || sourceLayer == "place_label_other" {
                    layer["source-layer"] = "place"
                }

                var layout = (layer["layout"] as? [String: Any]) ?? [:]
                if layout["text-font"] == nil {
                    layout["text-font"] = ["Poppins-Regular"]
                }
                layer["layout"] = layout

                normalizedLayers.append(layer)
            }

            root["layers"] = normalizedLayers

            let normalizedData = try JSONSerialization.data(withJSONObject: root)
            return String(data: normalizedData, encoding: .utf8) ?? styleJson
        } catch {
            print("Failed to normalize map style for iOS: \(error)")
            return styleJson
        }
    }
    
    func updateUIView(_ mapView: MLNMapView, context: Context) {
        mapView.attributionButton.isHidden = true
        mapView.logoView.isHidden = true
        mapView.compassView.isHidden = true

        context.coordinator.onDirectionChange = onDirectionChange
        context.coordinator.onMapTap = onMapTap
        context.coordinator.onAwayFromUserLocationChange = onAwayFromUserLocationChange
        context.coordinator.renderTerrainHazardsTemplate(
            mapView,
            points: terrainHazardPoints
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

        if context.coordinator.lastResetNorthToken != resetNorthToken {
            context.coordinator.lastResetNorthToken = resetNorthToken
            mapView.setDirection(0, animated: true)
        }

        if context.coordinator.lastRecenterOnUserToken != recenterOnUserToken {
            context.coordinator.lastRecenterOnUserToken = recenterOnUserToken
            context.coordinator.isTrackingUserLocation = true
            if let userLocation = context.coordinator.lastKnownUserLocation {
                mapView.setCenter(userLocation, zoomLevel: mapView.zoomLevel, animated: true)
                context.coordinator.setAwayFromUserLocation(false)
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
            mapView.setCenter(loc, zoomLevel: zoomLevel, animated: false)
            context.coordinator.lastCenter = loc
            context.coordinator.lastFollowLocation = newLocation
            context.coordinator.didCenterOnFirstGpsFix = true
            context.coordinator.evaluateAwayFromUserLocation(mapView)
        } else {
            if !context.coordinator.didCenterOnFirstGpsFix {
                context.coordinator.didCenterOnFirstGpsFix = true
                mapView.setCenter(loc, zoomLevel: mapView.zoomLevel, animated: true)
                context.coordinator.lastCenter = loc
                context.coordinator.lastFollowLocation = newLocation
                context.coordinator.evaluateAwayFromUserLocation(mapView)
                return
            }

            guard context.coordinator.shouldFollowForLocation(newLocation) else { return }
            guard context.coordinator.isTrackingUserLocation else { return }

            let currentCenter = mapView.centerCoordinate
            let current = CLLocation(latitude: currentCenter.latitude, longitude: currentCenter.longitude)
            let shouldRecenter = current.distance(from: newLocation) >= recenterDistanceMeters

            if shouldRecenter {
                mapView.setCenter(loc, zoomLevel: mapView.zoomLevel, animated: true)
                context.coordinator.lastCenter = loc
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
        var onMapTap: (CLLocationCoordinate2D) -> Void
        var onAwayFromUserLocationChange: (Bool) -> Void
        let awayFromUserDistanceMeters: CLLocationDistance
        var lastResetNorthToken: Int = 0
        var lastRecenterOnUserToken: Int = 0

        init(
            onDirectionChange: @escaping (CLLocationDirection) -> Void,
            onMapTap: @escaping (CLLocationCoordinate2D) -> Void,
            onAwayFromUserLocationChange: @escaping (Bool) -> Void,
            awayFromUserDistanceMeters: CLLocationDistance
        ) {
            self.onDirectionChange = onDirectionChange
            self.onMapTap = onMapTap
            self.onAwayFromUserLocationChange = onAwayFromUserLocationChange
            self.awayFromUserDistanceMeters = awayFromUserDistanceMeters
        }

        @objc
        func handlePanGesture(_ recognizer: UIPanGestureRecognizer) {
            if recognizer.state == .began {
                isTrackingUserLocation = false
            }
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
            guard let userLocation = lastKnownUserLocation else {
                setAwayFromUserLocation(false)
                return
            }

            let center = mapView.centerCoordinate
            let centerLocation = CLLocation(latitude: center.latitude, longitude: center.longitude)
            let gpsLocation = CLLocation(latitude: userLocation.latitude, longitude: userLocation.longitude)
            let isAway = centerLocation.distance(from: gpsLocation) >= awayFromUserDistanceMeters
            setAwayFromUserLocation(isAway)
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
            onMapTap(coordinate)
        }

        func renderTerrainHazardsTemplate(_ mapView: MLNMapView, points: [TerrainHazardOverlayPoint]) {
            _ = mapView
            _ = points
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
            let earthRadiusMeters = 6_371_000.0
            let angularDistance = radiusMeters / earthRadiusMeters
            let lat1 = center.latitude * .pi / 180
            let lon1 = center.longitude * .pi / 180

            var ring = [CLLocationCoordinate2D]()
            ring.reserveCapacity(segments + 1)

            for step in 0...segments {
                let bearing = 2 * Double.pi * Double(step) / Double(segments)
                let sinLat1 = sin(lat1)
                let cosLat1 = cos(lat1)
                let sinAd = sin(angularDistance)
                let cosAd = cos(angularDistance)

                let lat2 = asin(sinLat1 * cosAd + cosLat1 * sinAd * cos(bearing))
                let lon2 = lon1 + atan2(
                    sin(bearing) * sinAd * cosLat1,
                    cosAd - sinLat1 * sin(lat2)
                )

                ring.append(
                    CLLocationCoordinate2D(
                        latitude: lat2 * 180 / .pi,
                        longitude: lon2 * 180 / .pi
                    )
                )
            }

            return ring.withUnsafeMutableBufferPointer { buffer in
                MLNPolygonFeature(
                    coordinates: buffer.baseAddress!,
                    count: UInt(buffer.count),
                    interiorPolygons: nil
                )
            }
        }
    }
}
