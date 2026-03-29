package cz.miroslavpasek.pigeonnavigator.feature.search.presentation

/**
 * One-shot outputs for UI side effects.
 */
sealed interface SearchEffect {
    data class ShowValidationError(val message: String) : SearchEffect
    data class ShowUnexpectedError(val message: String) : SearchEffect
}
