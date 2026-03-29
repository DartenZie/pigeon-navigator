package cz.miroslavpasek.pigeonnavigator.core.platform.coroutines

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Provides coroutine dispatchers for shared logic.
 */
interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}
