package cz.miroslavpasek.pigeonnavigator.feature.search.presentation

/**
 * Defines one-shot UI effects emitted by the search store.
 */
sealed interface SearchEffect {
    /** Requests display of a validation error message. */
    data class ShowValidationError(val message: String) : SearchEffect

    /** Requests display of a non-validation error message. */
    data class ShowUnexpectedError(val message: String) : SearchEffect
}
