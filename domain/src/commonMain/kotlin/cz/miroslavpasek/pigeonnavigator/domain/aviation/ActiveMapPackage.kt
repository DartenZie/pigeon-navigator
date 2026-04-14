package cz.miroslavpasek.pigeonnavigator.domain.aviation

/**
 * Represents the currently active installed package and PMTiles file paths.
 */
data class ActiveMapPackage(
    val packageId: String,
    val mapPmtilesAbsolutePath: String,
    val terrainPmtilesAbsolutePath: String
)
