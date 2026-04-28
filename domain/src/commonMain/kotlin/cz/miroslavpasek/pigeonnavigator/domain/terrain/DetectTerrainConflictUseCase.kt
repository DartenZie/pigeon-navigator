package cz.miroslavpasek.pigeonnavigator.domain.terrain

import cz.miroslavpasek.pigeonnavigator.core.util.result.AppResult
import cz.miroslavpasek.pigeonnavigator.domain.failure.Failure
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Computes terrain conflict risk by ray-sampling terrain elevation ahead of the aircraft.
 *
 * The use case samples multiple bearings around current heading, tracks minimum vertical clearance,
 * and derives warning level from clearance and time-to-impact thresholds.
 */
class DetectTerrainConflictUseCase(
    private val terrainRepository: TerrainRepository
) {

    /**
     * Evaluates terrain conflict for [snapshot] using [parameters].
     *
     * @param snapshot Aircraft position, altitude, speed, and course used for sampling.
     * @param parameters Sampling distances and warning thresholds.
     * @return [AppResult.Success] with a prediction when at least one terrain sample is available.
     * Returns [Failure.OutOfCoverage] when all samples are outside coverage and
     * [Failure.DataUnavailable] when no sample can be retrieved for other reasons.
     */
    suspend operator fun invoke(
        snapshot: AircraftSnapshot,
        parameters: TerrainConflictParameters = TerrainConflictParameters()
    ): AppResult<TerrainConflictPrediction, Failure> {
        val bearings = sampleBearings(
            originBearingDegrees = snapshot.bearingDegrees,
            sectorHalfAngleDegrees = parameters.sectorHalfAngleDegrees,
            rayCount = parameters.rayCount
        )

        var minClearanceMeters = Double.POSITIVE_INFINITY
        var nearestImpactDistanceMeters: Double? = null
        var validSampleCount = 0
        var outOfCoverageCount = 0
        val hazardSamples = mutableListOf<TerrainHazardSample>()

        for (bearing in bearings) {
            var distanceMeters = 0.0
            while (distanceMeters <= parameters.lookAheadDistanceMeters) {
                val stepMeters = if (distanceMeters < parameters.nearDistanceMeters) {
                    parameters.nearStepMeters
                } else {
                    parameters.farStepMeters
                }

                distanceMeters += stepMeters

                val samplePoint = destinationPoint(
                    latitudeDegrees = snapshot.latitude,
                    longitudeDegrees = snapshot.longitude,
                    bearingDegrees = bearing,
                    distanceMeters = distanceMeters
                )

                when (val terrainSample = terrainRepository.sampleTerrainElevationMeters(samplePoint.first, samplePoint.second)) {
                    is AppResult.Success -> {
                        validSampleCount += 1

                        val terrainDeltaMeters = terrainSample.value - snapshot.altitudeMeters
                        val clearanceMeters = snapshot.altitudeMeters - terrainSample.value - parameters.safetyMarginMeters
                        minClearanceMeters = min(minClearanceMeters, clearanceMeters)

                        if (terrainDeltaMeters >= 0.0) {
                            hazardSamples += TerrainHazardSample(
                                latitude = samplePoint.first,
                                longitude = samplePoint.second,
                                level = TerrainHazardLevel.Conflict
                            )
                            nearestImpactDistanceMeters = minPositive(nearestImpactDistanceMeters, distanceMeters)
                            break
                        } else if (terrainDeltaMeters >= -parameters.nearConflictVerticalBandMeters) {
                            hazardSamples += TerrainHazardSample(
                                latitude = samplePoint.first,
                                longitude = samplePoint.second,
                                level = TerrainHazardLevel.NearConflict
                            )
                        }
                    }

                    is AppResult.Failure -> {
                        if (terrainSample.error == Failure.OutOfCoverage) {
                            outOfCoverageCount += 1
                        }
                    }
                }
            }
        }

        if (validSampleCount == 0) {
            return if (outOfCoverageCount > 0) {
                AppResult.Failure(Failure.OutOfCoverage)
            } else {
                AppResult.Failure(Failure.DataUnavailable)
            }
        }

        val timeToImpactSeconds = nearestImpactDistanceMeters
            ?.takeIf { snapshot.speedMetersPerSecond > 1.0 }
            ?.div(snapshot.speedMetersPerSecond)

        val warningLevel = when {
            nearestImpactDistanceMeters == null -> TerrainWarningLevel.None
            minClearanceMeters <= parameters.warningClearanceMeters -> TerrainWarningLevel.Warning
            timeToImpactSeconds != null && timeToImpactSeconds <= parameters.warningTimeToImpactSeconds -> TerrainWarningLevel.Warning
            minClearanceMeters <= parameters.cautionClearanceMeters -> TerrainWarningLevel.Caution
            timeToImpactSeconds != null && timeToImpactSeconds <= parameters.cautionTimeToImpactSeconds -> TerrainWarningLevel.Caution
            else -> TerrainWarningLevel.Info
        }

        return AppResult.Success(
            TerrainConflictPrediction(
                hasConflict = nearestImpactDistanceMeters != null,
                warningLevel = warningLevel,
                minClearanceMeters = minClearanceMeters,
                distanceToImpactMeters = nearestImpactDistanceMeters,
                timeToImpactSeconds = timeToImpactSeconds,
                hazardSamples = hazardSamples
            )
        )
    }

    private fun sampleBearings(
        originBearingDegrees: Double,
        sectorHalfAngleDegrees: Double,
        rayCount: Int
    ): List<Double> {
        if (rayCount <= 1) return listOf(normalizeBearing(originBearingDegrees))

        val step = (sectorHalfAngleDegrees * 2.0) / (rayCount - 1)
        return List(rayCount) { index ->
            val offset = -sectorHalfAngleDegrees + index * step
            normalizeBearing(originBearingDegrees + offset)
        }
    }

    private fun destinationPoint(
        latitudeDegrees: Double,
        longitudeDegrees: Double,
        bearingDegrees: Double,
        distanceMeters: Double
    ): Pair<Double, Double> {
        val angularDistance = distanceMeters / EARTH_RADIUS_METERS
        val bearing = bearingDegrees.toRadians()

        val latitude = latitudeDegrees.toRadians()
        val longitude = longitudeDegrees.toRadians()

        val sinLatitude = sin(latitude)
        val cosLatitude = cos(latitude)
        val sinAngularDistance = sin(angularDistance)
        val cosAngularDistance = cos(angularDistance)

        val destinationLatitude = asin(
            sinLatitude * cosAngularDistance +
                cosLatitude * sinAngularDistance * cos(bearing)
        )

        val destinationLongitude = longitude + atan2(
            sin(bearing) * sinAngularDistance * cosLatitude,
            cosAngularDistance - sinLatitude * sin(destinationLatitude)
        )

        return Pair(destinationLatitude.toDegrees(), normalizeLongitude(destinationLongitude.toDegrees()))
    }

    private fun minPositive(current: Double?, candidate: Double): Double {
        if (current == null) return candidate
        return min(current, candidate)
    }

    private fun normalizeBearing(value: Double): Double {
        val normalized = value % 360.0
        return if (normalized < 0.0) normalized + 360.0 else normalized
    }

    private fun normalizeLongitude(value: Double): Double {
        var longitude = value
        while (longitude > 180.0) longitude -= 360.0
        while (longitude < -180.0) longitude += 360.0
        return longitude
    }

    private fun Double.toRadians(): Double = this * PI / 180.0

    private fun Double.toDegrees(): Double = this * 180.0 / PI

    private companion object {
        const val EARTH_RADIUS_METERS = 6_371_000.0
    }
}
