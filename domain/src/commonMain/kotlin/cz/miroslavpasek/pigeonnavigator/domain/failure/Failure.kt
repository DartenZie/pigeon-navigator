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
     * Unclassified domain failure.
     */
    data object Unexpected : Failure
}
