package cz.miroslavpasek.pigeonnavigator.core.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

/**
 * Creates platform SQLDelight drivers for a concrete database schema.
 */
expect class DatabaseDriverFactory() {
    /**
     * Returns a platform [SqlDriver] for [schema] and [databaseName].
     */
    fun createDriver(
        schema: SqlSchema<QueryResult.Value<Unit>>,
        databaseName: String
    ): SqlDriver
}
