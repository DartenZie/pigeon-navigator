package cz.miroslavpasek.pigeonnavigator.domain.terrain

/**
 * Classifies terrain conflict severity derived from sampled clearance and impact metrics.
 */
enum class TerrainWarningLevel {
    /** No conflict indicators are currently present. */
    None,
    /** A conflict trend exists but remains outside caution thresholds. */
    Info,
    /** Terrain proximity requires increased pilot awareness. */
    Caution,
    /** Terrain impact risk is imminent under current trajectory. */
    Warning
}

/**
 * Classifies map overlay severity for sampled terrain hazard points.
 */
enum class TerrainHazardLevel {
    /** Terrain is close but not currently intersecting the flight path margin. */
    NearConflict,
    /** Terrain intersects the sampled flight path margin. */
    Conflict
}

/**
 * One sampled map point that should be highlighted as terrain hazard.
 *
 * @property latitude Latitude in decimal degrees.
 * @property longitude Longitude in decimal degrees.
 * @property level Hazard severity for overlay styling.
 */
data class TerrainHazardSample(
    val latitude: Double,
    val longitude: Double,
    val level: TerrainHazardLevel
)

/**
 * Describes the latest terrain conflict evaluation for one aircraft snapshot.
 *
 * @property hasConflict True when at least one sampled ray intersects terrain inside look-ahead distance.
 * @property warningLevel Severity derived from clearance and time-to-impact thresholds.
 * @property minClearanceMeters Lowest sampled clearance after applying the configured safety margin.
 * @property distanceToImpactMeters Distance to the nearest terrain intersection, or `null` when no impact is predicted.
 * @property timeToImpactSeconds Time to nearest impact using current speed, or `null` when not computable.
 * @property hazardSamples Sampled map points representing near-conflict and conflict areas.
 */
data class TerrainConflictPrediction(
    val hasConflict: Boolean,
    val warningLevel: TerrainWarningLevel,
    val minClearanceMeters: Double,
    val distanceToImpactMeters: Double?,
    val timeToImpactSeconds: Double?,
    val hazardSamples: List<TerrainHazardSample> = emptyList()
)

/**
 * Defines sampling geometry and warning thresholds for terrain conflict detection.
 *
 * @property lookAheadDistanceMeters Maximum ray distance sampled ahead of the aircraft.
 * @property sectorHalfAngleDegrees Half-angle of the sampled forward sector centered on current bearing.
 * @property rayCount Number of rays sampled across the sector.
 * @property nearDistanceMeters Distance where sampling switches from near to far step size.
 * @property nearStepMeters Step length used before [nearDistanceMeters].
 * @property farStepMeters Step length used after [nearDistanceMeters].
 * @property safetyMarginMeters Vertical margin subtracted from aircraft altitude before evaluating clearance.
 * @property cautionClearanceMeters Clearance threshold that triggers [TerrainWarningLevel.Caution].
 * @property warningClearanceMeters Clearance threshold that triggers [TerrainWarningLevel.Warning].
 * @property cautionTimeToImpactSeconds Time-to-impact threshold that triggers [TerrainWarningLevel.Caution].
 * @property warningTimeToImpactSeconds Time-to-impact threshold that triggers [TerrainWarningLevel.Warning].
 * @property nearConflictVerticalBandMeters Vertical band below aircraft altitude that should be
 * rendered as near-conflict on the map while terrain is still below the aircraft.
 */
data class TerrainConflictParameters(
    val lookAheadDistanceMeters: Double = 20_000.0,
    val sectorHalfAngleDegrees: Double = 25.0,
    val rayCount: Int = 5,
    val nearDistanceMeters: Double = 5_000.0,
    val nearStepMeters: Double = 75.0,
    val farStepMeters: Double = 150.0,
    val safetyMarginMeters: Double = 100.0,
    val cautionClearanceMeters: Double = 150.0,
    val warningClearanceMeters: Double = 60.0,
    val cautionTimeToImpactSeconds: Double = 60.0,
    val warningTimeToImpactSeconds: Double = 30.0,
    val nearConflictVerticalBandMeters: Double = 50.0
)
