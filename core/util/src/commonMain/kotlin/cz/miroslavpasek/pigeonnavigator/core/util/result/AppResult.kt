package cz.miroslavpasek.pigeonnavigator.core.util.result

/**
 * Represents an operation outcome with either a success value [V] or a failure value [E].
 */
sealed interface AppResult<out V, out E> {
    /**
     * Successful outcome containing [value].
     */
    data class Success<out V>(val value: V) : AppResult<V, Nothing>

    /**
     * Failed outcome containing domain/application [error].
     */
    data class Failure<out E>(val error: E) : AppResult<Nothing, E>
}

/**
 * Executes [block] and wraps its result into [AppResult.Success].
 *
 * If [block] throws, the exception is mapped by [mapError] and returned as [AppResult.Failure].
 */
inline fun <V, E> appResultOf(block: () -> V, mapError: (Throwable) -> E): AppResult<V, E> =
    try {
        AppResult.Success(block())
    } catch (throwable: Throwable) {
        AppResult.Failure(mapError(throwable))
    }
