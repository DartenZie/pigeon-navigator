package cz.miroslavpasek.pigeonnavigator.domain.failure

/**
 * Domain-level failure model shared across use-cases.
 */
sealed interface Failure {
    /**
     * Validation failed for user-provided input.
     */
    data class Validation(val message: String) : Failure

    /**
     * Requested terrain sample is outside bundled DEM coverage.
     */
    data object OutOfCoverage : Failure

    /**
     * Required data source is currently unavailable.
     */
    data object DataUnavailable : Failure

    /**
     * Required data source is unavailable with a diagnostic code for debugging.
     */
    data class DataUnavailableReason(val code: String) : Failure

    /**
     * Unclassified domain failure.
     */
    data object Unexpected : Failure
}
