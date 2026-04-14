package cz.miroslavpasek.pigeonnavigator.data.aviation.db

import cz.miroslavpasek.pigeonnavigator.core.database.DatabaseDriverFactory

/**
 * Creates a singleton SQLDelight database instance for aviation data.
 */
class AviationDatabaseProvider(
    private val driverFactory: DatabaseDriverFactory = DatabaseDriverFactory()
) {
    val database: AviationDatabase by lazy {
        AviationDatabase(
            driver = driverFactory.createDriver(
                schema = AviationDatabase.Schema,
                databaseName = DATABASE_NAME
            )
        )
    }

    private companion object {
        const val DATABASE_NAME = "aviation.db"
    }
}
