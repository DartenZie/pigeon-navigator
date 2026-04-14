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
        let styleProvider = MapStyleProvider(
            config: MapStyleConfig(
                baseStyleAssetPath: "style.json",
                pmtilesSourceId: "pmtiles-source",
                tileArchiveLocation: TileArchiveLocationAsset(assetPath: "cz.pmtiles")
            ),
            assetLoader: PlatformAssetLoader(),
            urlResolver: PmtilesUrlResolver(tileArchiveFileStore: TileArchiveFileStore())
        )
        let rawStyleJson = styleProvider.getStyleJson()
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
        
        guard followUser, let loc = location else { return }
        
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
    }
}
