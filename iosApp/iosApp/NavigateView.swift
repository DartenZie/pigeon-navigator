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
        let styleJson = styleProvider.getStyleJson()
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
    
    func updateUIView(_ mapView: MLNMapView, context: Context) {
        mapView.attributionButton.isHidden = true
        mapView.logoView.isHidden = true
        
        guard followUser, let loc = location else { return }
        
        guard mapView.style != nil else { return }
        
        if !context.coordinator.didSetInitialCamera {
            context.coordinator.didSetInitialCamera = true
            mapView.setCenter(loc, zoomLevel: zoomLevel, animated: false)
        } else {
            mapView.setCenter(loc, zoomLevel: mapView.zoomLevel, animated: true)
        }
    }
    
    final class Coordinator: NSObject, MLNMapViewDelegate {
        var didSetInitialCamera = false
        
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
