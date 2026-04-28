package cz.miroslavpasek.pigeonnavigator.data.aviation.platform

/**
 * Platform logger used for OFPKG install diagnostics.
 */
expect object OfpkgInstallLogger {
    fun log(message: String)
}
