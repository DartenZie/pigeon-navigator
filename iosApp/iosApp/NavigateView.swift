//
//  NavigationView.swift
//  iosApp
//
//  Created by Miroslav Pašek on 07.02.2026.
//

import SwiftUI
import MapLibre
import CoreLocation
import Shared

struct NavigateView: UIViewRepresentable {
    let location: CLLocationCoordinate2D?
    var followUser: Bool = true
    var zoomLevel = 10.5
    private let recenterDistanceMeters: CLLocationDistance = 12
    
    func makeCoordinator() -> Coordinator { Coordinator() }
    
    func makeUIView(context: Context) -> MLNMapView {
        let styleURL = resolveStyleURL()
            ?? Bundle.main.url(forResource: "style", withExtension: "json", subdirectory: "MapAssets")!
                 
        let mapView = MLNMapView(frame: .zero, styleURL: styleURL)
        mapView.delegate = context.coordinator
        
        mapView.allowsRotating = true
        mapView.allowsTilting = true
        
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
        
        guard followUser, let loc = location else { return }
        
        guard mapView.style != nil else { return }
        
        if !context.coordinator.didSetInitialCamera {
            context.coordinator.didSetInitialCamera = true
            mapView.setCenter(loc, zoomLevel: zoomLevel, animated: false)
            context.coordinator.lastCenter = loc
        } else {
            let currentCenter = mapView.centerCoordinate
            let current = CLLocation(latitude: currentCenter.latitude, longitude: currentCenter.longitude)
            let target = CLLocation(latitude: loc.latitude, longitude: loc.longitude)
            let shouldRecenter = current.distance(from: target) >= recenterDistanceMeters

            if shouldRecenter {
                mapView.setCenter(loc, zoomLevel: mapView.zoomLevel, animated: true)
                context.coordinator.lastCenter = loc
            }
        }
    }
    
    final class Coordinator: NSObject, MLNMapViewDelegate {
        var didSetInitialCamera = false
        var lastCenter: CLLocationCoordinate2D?
        
        func mapView(_ mapView: MLNMapView, didFinishLoading style: MLNStyle) {
            if !didSetInitialCamera {
                let prague = CLLocationCoordinate2D(latitude: 50.0755, longitude: 14.4378)
                mapView.setCenter(prague, zoomLevel: 10.5, animated: false)
            }
            
            mapView.attributionButton.isHidden = true
            mapView.logoView.isHidden = true
        }
    }
}
