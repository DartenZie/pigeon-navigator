package cz.miroslavpasek.pigeonnavigator.core.platform.coroutines

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Provides coroutine dispatchers used by shared feature and domain code.
 */
interface DispatcherProvider {
    /** Dispatcher for state publishing and main-thread constrained work. */
    val main: CoroutineDispatcher

    /** Dispatcher for blocking or I/O-bound operations. */
    val io: CoroutineDispatcher

    /** Dispatcher for CPU-bound computations. */
    val default: CoroutineDispatcher
}
