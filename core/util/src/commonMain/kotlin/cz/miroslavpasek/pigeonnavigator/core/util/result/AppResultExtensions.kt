package cz.miroslavpasek.pigeonnavigator.core.util.result

/**
 * Transforms a success value with [transform] while preserving a failure unchanged.
 */
inline fun <V, E, R> AppResult<V, E>.map(transform: (V) -> R): AppResult<R, E> =
    when (this) {
        is AppResult.Success -> AppResult.Success(transform(value))
        is AppResult.Failure -> AppResult.Failure(error)
    }

/**
 * Chains another [AppResult]-producing transformation for success values.
 */
inline fun <V, E, R> AppResult<V, E>.flatMap(transform: (V) -> AppResult<R, E>): AppResult<R, E> =
    when (this) {
        is AppResult.Success -> transform(value)
        is AppResult.Failure -> AppResult.Failure(error)
    }

/**
 * Runs [action] when the receiver is [AppResult.Success].
 */
inline fun <V, E> AppResult<V, E>.onSuccess(action: (V) -> Unit): AppResult<V, E> =
    apply {
        if (this is AppResult.Success) {
            action(value)
        }
    }

/**
 * Runs [action] when the receiver is [AppResult.Failure].
 */
inline fun <V, E> AppResult<V, E>.onFailure(action: (E) -> Unit): AppResult<V, E> =
    apply {
        if (this is AppResult.Failure) {
            action(error)
        }
    }

/**
 * Reduces the result into a single value by handling success and failure branches.
 */
inline fun <V, E, R> AppResult<V, E>.fold(onSuccess: (V) -> R, onFailure: (E) -> R): R =
    when (this) {
        is AppResult.Success -> onSuccess(value)
        is AppResult.Failure -> onFailure(error)
    }
