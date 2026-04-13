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
 * Describes the latest terrain conflict evaluation for one aircraft snapshot.
 *
 * @property hasConflict True when at least one sampled ray intersects terrain inside look-ahead distance.
 * @property warningLevel Severity derived from clearance and time-to-impact thresholds.
 * @property minClearanceMeters Lowest sampled clearance after applying the configured safety margin.
 * @property distanceToImpactMeters Distance to the nearest terrain intersection, or `null` when no impact is predicted.
 * @property timeToImpactSeconds Time to nearest impact using current speed, or `null` when not computable.
 */
data class TerrainConflictPrediction(
    val hasConflict: Boolean,
    val warningLevel: TerrainWarningLevel,
    val minClearanceMeters: Double,
    val distanceToImpactMeters: Double?,
    val timeToImpactSeconds: Double?
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
    val warningTimeToImpactSeconds: Double = 30.0
)
