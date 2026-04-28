package cz.miroslavpasek.pigeonnavigator.platform

import cz.miroslavpasek.pigeonnavigator.core.platform.settings.KeyValueSettingsStore
import platform.Foundation.NSUserDefaults

/**
 * [KeyValueSettingsStore] adapter that delegates to iOS [NSUserDefaults].
 *
 * The implementation must not leak Foundation types beyond this binding: the only public
 * surface is the platform-agnostic [KeyValueSettingsStore] interface, registered through DI.
 *
 * Reads disambiguate "absent" from "stored zero" by probing [NSUserDefaults.objectForKey]
 * before falling back to typed accessors.
 */
internal class IosKeyValueSettingsStore(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : KeyValueSettingsStore {

    override suspend fun getString(key: String): String? = defaults.stringForKey(key)

    override suspend fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }

    override suspend fun getInt(key: String): Int? {
        defaults.objectForKey(key) ?: return null
        return defaults.integerForKey(key).toInt()
    }

    override suspend fun putInt(key: String, value: Int) {
        defaults.setInteger(value.toLong(), forKey = key)
    }
}
