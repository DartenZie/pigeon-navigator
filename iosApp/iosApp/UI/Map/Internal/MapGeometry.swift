import Foundation
import CoreLocation
import MapLibre

/// Pure geometry helpers used by the navigate map view.
///
/// Mirrors the shapes computed by the Android `ui/map/internal/MapGeometry.kt`
/// helpers so the two platforms render the same accuracy circles, guidance
/// cones, and minute marks.
enum MapGeometry {
    private static let earthRadiusMeters = 6_371_000.0

    static func normalizeBearingDegrees(_ bearing: Double) -> Double {
        let normalized = bearing.truncatingRemainder(dividingBy: 360)
        return normalized >= 0 ? normalized : normalized + 360
    }

    static func normalizeLongitudeDegrees(_ longitude: Double) -> Double {
        var normalized = longitude
        while normalized > 180 { normalized -= 360 }
        while normalized < -180 { normalized += 360 }
        return normalized
    }

    static func destinationCoordinate(
        from start: CLLocationCoordinate2D,
        bearingDegrees: Double,
        distanceMeters: Double
    ) -> CLLocationCoordinate2D {
        let angularDistance = distanceMeters / earthRadiusMeters
        let bearingRadians = bearingDegrees * .pi / 180
        let lat1 = start.latitude * .pi / 180
        let lon1 = start.longitude * .pi / 180

        let sinLat1 = sin(lat1)
        let cosLat1 = cos(lat1)
        let sinAd = sin(angularDistance)
        let cosAd = cos(angularDistance)

        let lat2 = asin(sinLat1 * cosAd + cosLat1 * sinAd * cos(bearingRadians))
        let lon2 = lon1 + atan2(
            sin(bearingRadians) * sinAd * cosLat1,
            cosAd - sinLat1 * sin(lat2)
        )

        return CLLocationCoordinate2D(
            latitude: lat2 * 180 / .pi,
            longitude: normalizeLongitudeDegrees(lon2 * 180 / .pi)
        )
    }

    /// Builds the accuracy ring polygon used to indicate horizontal GPS error
    /// around the user dot.
    static func buildAccuracyPolygon(
        center: CLLocationCoordinate2D,
        radiusMeters: Double,
        segments: Int = 64
    ) -> MLNPolygonFeature {
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

    /// Returns the signed shortest direction delta (in degrees) for animating
    /// between two compass headings.
    static func shortestDirectionDelta(
        from startDirection: CLLocationDirection,
        to endDirection: CLLocationDirection
    ) -> CLLocationDirection {
        let delta = normalizeBearingDegrees(endDirection) - normalizeBearingDegrees(startDirection)
        if delta > 180 { return delta - 360 }
        if delta < -180 { return delta + 360 }
        return delta
    }
}
