package cz.miroslavpasek.pigeonnavigator.di

import android.content.Context
import android.content.SharedPreferences
import cz.miroslavpasek.pigeonnavigator.core.platform.settings.KeyValueSettingsStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Stable preferences file used to back [KeyValueSettingsStore] on Android.
 *
 * Keep this constant unchanged — renaming it would orphan previously persisted values.
 */
private const val PIGEON_NAVIGATOR_PREFERENCES_FILE = "pigeon_navigator_app_settings"

/**
 * Android Koin module that exposes a [KeyValueSettingsStore] backed by the application-scoped
 * [SharedPreferences] file [PIGEON_NAVIGATOR_PREFERENCES_FILE].
 *
 * The implementation must not leak Android types beyond this module: it is the only place that
 * is allowed to import `SharedPreferences` and consumers depend exclusively on the
 * platform-agnostic interface.
 */
val settingsModule = module {
    single<SharedPreferences> {
        androidContext().getSharedPreferences(
            PIGEON_NAVIGATOR_PREFERENCES_FILE,
            Context.MODE_PRIVATE,
        )
    }
    single<KeyValueSettingsStore> { SharedPreferencesKeyValueSettingsStore(get()) }
}

/**
 * [KeyValueSettingsStore] adapter that delegates to Android [SharedPreferences].
 *
 * Reads return `null` when the requested key is missing so callers can fall back to defaults
 * instead of misinterpreting type-default values such as `0`.
 */
private class SharedPreferencesKeyValueSettingsStore(
    private val sharedPreferences: SharedPreferences,
) : KeyValueSettingsStore {

    override suspend fun getString(key: String): String? =
        sharedPreferences.getString(key, null)

    override suspend fun putString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    override suspend fun getInt(key: String): Int? =
        if (sharedPreferences.contains(key)) sharedPreferences.getInt(key, 0) else null

    override suspend fun putInt(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }
}
