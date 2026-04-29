import Foundation
import Shared

/// Loads the shared MapLibre style JSON, rewrites paths so iOS can resolve
/// glyphs and sprites from the bundle, and writes the result to a file URL
/// MapLibre Native can stream from.
enum MapStyleNormalizer {
    /// Resolves a file URL containing a normalised style JSON, or `nil` if the
    /// shared bridge fails to provide one.
    static func resolveStyleURL() -> URL? {
        let styleBridge = MapStyleBridge()
        guard let rawStyleJson = styleBridge.resolveStyleJson() else {
            return nil
        }
        let styleJson = normalize(styleJson: rawStyleJson)

        guard let appSupportDirectory = FileManager.default.urls(
            for: .applicationSupportDirectory,
            in: .userDomainMask
        ).first else {
            return nil
        }

        let outputDir = appSupportDirectory.appendingPathComponent("map-style", isDirectory: true)
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

    /// Rewrites the style JSON to point at bundled fonts/sprites and to align
    /// label source-layer names with the shipped vector tiles.
    static func normalize(styleJson: String) -> String {
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
}
